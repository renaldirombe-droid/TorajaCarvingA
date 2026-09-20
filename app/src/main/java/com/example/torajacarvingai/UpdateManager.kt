package com.example.torajacarvingai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Mengecek & mengunduh bundle model/labels/katalog baru dari internet, memvalidasinya
 * secara menyeluruh, dan baru mengaktifkannya jika SEMUA validasi lolos. Tidak pernah
 * mengganti bundle aktif yang sudah berjalan sebelum bundle baru terbukti valid, dan
 * tidak pernah melempar exception ke pemanggil (semua kegagalan dilaporkan lewat callback).
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "TorajaBundle"

        // Supabase Storage (bucket publik "toraja-updates") -- bisa diakses dari internet
        // mana pun, tidak perlu HP & laptop sejaringan WiFi seperti waktu masih pakai
        // backend lokal. HTTPS biasa, tidak butuh cleartext traffic untuk URL ini.
        const val UPDATE_MANIFEST_URL = "https://vemhqgsievkzynkyryev.supabase.co/storage/v1/object/public/toraja-updates/latest/version.json"

        private const val TIMEOUT_MS = 15_000
    }

    private val bundleManager = ModelBundleManager(context)
    private val gson = Gson()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun checkForUpdates(onResult: (UpdateResult) -> Unit) {
        executor.execute {
            val result = runCheck()
            onResult(result)
        }
    }

    private fun runCheck(): UpdateResult {
        return try {
            Log.d(TAG, "checkForUpdates: UPDATE_MANIFEST_URL = $UPDATE_MANIFEST_URL")
            val manifestJson = try {
                downloadText(UPDATE_MANIFEST_URL)
            } catch (e: Exception) {
                Log.e(TAG, "checkForUpdates: gagal download version.json -> ${e.message}", e)
                return UpdateResult.Failed("Gagal download version.json: ${e.message}")
            }
            Log.d(TAG, "checkForUpdates: download version.json = SUKSES (${manifestJson.length} bytes)")

            val manifest = gson.fromJson(manifestJson, UpdateManifest::class.java)
                ?: return UpdateResult.Failed("Manifest kosong / tidak bisa diparse")

            Log.d(TAG, "checkForUpdates: bundle_version online = ${manifest.bundle_version}")
            Log.d(TAG, "checkForUpdates: model_url = ${manifest.model_url}")
            Log.d(TAG, "checkForUpdates: labels_url = ${manifest.labels_url}")
            Log.d(TAG, "checkForUpdates: catalog_url = ${manifest.catalog_url}")

            val currentAppVersionCode = BuildConfig.VERSION_CODE
            if (manifest.min_app_version_code > currentAppVersionCode) {
                Log.d(TAG, "App perlu diupdate: min_app_version_code=${manifest.min_app_version_code} > current=$currentAppVersionCode")
                return UpdateResult.NeedsAppUpdate(manifest.min_app_version_code, currentAppVersionCode)
            }

            val localVersion = bundleManager.localBundleVersion()
            if (manifest.bundle_version <= localVersion) {
                Log.d(TAG, "checkForUpdates: sudah up to date (local=$localVersion, remote=${manifest.bundle_version}) -> fallback ke bundle aktif saat ini")
                return UpdateResult.UpToDate(localVersion)
            }

            bundleManager.ensureDirs()
            bundleManager.tmpDir.listFiles()?.forEach { it.delete() }

            val tmpModelFile = File(bundleManager.tmpDir, manifest.model_file)
            val tmpLabelsFile = File(bundleManager.tmpDir, manifest.labels_file)
            val tmpCatalogFile = File(bundleManager.tmpDir, manifest.catalog_file)

            downloadToFile(manifest.model_url, tmpModelFile)
            Log.d(TAG, "checkForUpdates: download model = SUKSES (${tmpModelFile.length()} bytes)")
            downloadToFile(manifest.labels_url, tmpLabelsFile)
            Log.d(TAG, "checkForUpdates: download labels = SUKSES (${tmpLabelsFile.length()} bytes)")
            downloadToFile(manifest.catalog_url, tmpCatalogFile)
            Log.d(TAG, "checkForUpdates: download catalog = SUKSES (${tmpCatalogFile.length()} bytes)")

            val validationError = validateDownloadedBundle(manifest, tmpModelFile, tmpLabelsFile, tmpCatalogFile)
            if (validationError != null) {
                bundleManager.tmpDir.listFiles()?.forEach { it.delete() }
                Log.e(TAG, "checkForUpdates: Bundle baru INVALID -> $validationError")
                Log.d(TAG, "checkForUpdates: hasil = GAGAL, fallback ke bundle aktif lama / assets")
                return UpdateResult.Failed(validationError)
            }

            bundleManager.activateBundleFromTmp(manifest, tmpModelFile, tmpLabelsFile, tmpCatalogFile)
            Log.d(TAG, "checkForUpdates: Bundle baru berhasil diaktifkan: bundle_version=${manifest.bundle_version}")
            Log.d(TAG, "checkForUpdates: hasil = BERHASIL (perlu buka ulang app utk pakai model terbaru)")
            UpdateResult.Updated(manifest.bundle_version, manifest)
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdates: gagal tak terduga -> ${e.message}", e)
            Log.d(TAG, "checkForUpdates: hasil = GAGAL, fallback ke bundle aktif lama / assets")
            bundleManager.tmpDir.listFiles()?.forEach { it.delete() }
            UpdateResult.Failed(e.message ?: "Unknown error")
        }
    }

    /** Mengembalikan null jika valid, atau pesan error jika bundle harus ditolak. */
    private fun validateDownloadedBundle(
        manifest: UpdateManifest,
        modelFile: File,
        labelsFile: File,
        catalogFile: File
    ): String? {
        // 1) SHA256 setiap file harus cocok dengan manifest
        val modelHash = ModelBundleManager.sha256OfFile(modelFile)
        val modelHashOk = modelHash.equals(manifest.model_sha256, ignoreCase = true)
        Log.d(TAG, "validateDownloadedBundle: SHA256 model cocok = $modelHashOk")
        if (!modelHashOk) {
            return "SHA256 model tidak cocok (expected=${manifest.model_sha256}, actual=$modelHash)"
        }
        val labelsHash = ModelBundleManager.sha256OfFile(labelsFile)
        val labelsHashOk = labelsHash.equals(manifest.labels_sha256, ignoreCase = true)
        Log.d(TAG, "validateDownloadedBundle: SHA256 labels cocok = $labelsHashOk")
        if (!labelsHashOk) {
            return "SHA256 labels tidak cocok (expected=${manifest.labels_sha256}, actual=$labelsHash)"
        }
        val catalogHash = ModelBundleManager.sha256OfFile(catalogFile)
        val catalogHashOk = catalogHash.equals(manifest.catalog_sha256, ignoreCase = true)
        Log.d(TAG, "validateDownloadedBundle: SHA256 catalog cocok = $catalogHashOk")
        if (!catalogHashOk) {
            return "SHA256 catalog tidak cocok (expected=${manifest.catalog_sha256}, actual=$catalogHash)"
        }

        // 2) Labels tidak boleh kosong & jumlahnya harus sesuai manifest.class_count
        val labels = labelsFile.readLines().filter { it.isNotBlank() }
        if (labels.isEmpty()) return "Labels kosong"
        if (labels.size != manifest.class_count) {
            return "Jumlah labels (${labels.size}) tidak sama dengan manifest.class_count (${manifest.class_count})"
        }

        // 3) Catalog harus berupa JSON valid (boleh kosong/berbeda isi dengan labels, itu ditolerir saat load)
        try {
            val type = object : TypeToken<Map<String, MotifInfo>>() {}.type
            gson.fromJson<Map<String, MotifInfo>>(catalogFile.readText(), type)
        } catch (e: Exception) {
            return "Catalog JSON tidak valid: ${e.message}"
        }

        // 4) Model harus bisa di-load, input shape & output class count harus sesuai manifest
        var tempInterpreter: Interpreter? = null
        try {
            tempInterpreter = Interpreter(modelFile, Interpreter.Options())
            val inputShape = tempInterpreter.getInputTensor(0).shape() // [1, H, W, 3]
            val outputShape = tempInterpreter.getOutputTensor(0).shape() // [1, class_count]

            if (inputShape.size < 3 || inputShape[1] != manifest.input_size || inputShape[2] != manifest.input_size) {
                return "Input shape model (${inputShape.contentToString()}) tidak sesuai manifest.input_size (${manifest.input_size})"
            }
            val outputClassCount = outputShape.getOrNull(1) ?: -1
            val outputMatchesLabels = outputClassCount == labels.size && outputClassCount == manifest.class_count
            Log.d(TAG, "validateDownloadedBundle: output model ($outputClassCount kelas) vs labels (${labels.size}) cocok = $outputMatchesLabels")
            if (outputClassCount != labels.size) {
                return "Output tensor model ($outputClassCount kelas) tidak sama dengan jumlah labels (${labels.size})"
            }
            if (outputClassCount != manifest.class_count) {
                return "Output tensor model ($outputClassCount kelas) tidak sama dengan manifest.class_count (${manifest.class_count})"
            }
        } catch (e: Exception) {
            return "Model gagal di-load: ${e.message}"
        } finally {
            tempInterpreter?.close()
        }

        return null
    }

    private fun downloadText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.requestMethod = "GET"
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadToFile(url: String, destination: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.requestMethod = "GET"
        try {
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Hapus bundle online yang aktif (jika ada) dan kembali memakai assets bawaan APK. */
    fun clearOnlineBundleAndUseAssets() {
        bundleManager.clearOnlineBundleAndUseAssets()
    }

    fun getActiveBundleInfo(): ActiveBundleInfo = bundleManager.getActiveBundleInfo()
}
