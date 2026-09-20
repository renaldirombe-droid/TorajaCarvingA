package com.example.torajacarvingai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * Sumber tunggal kebenaran untuk model/labels/katalog yang sedang aktif dipakai aplikasi.
 *
 * Prioritas sumber:
 *   1. Bundle online tervalidasi di files/toraja_update/active/ (jika ada & konsisten)
 *   2. Assets bawaan APK (fallback utama, selalu tersedia)
 *
 * Nama file default assets DIAMBIL DARI HASIL AUDIT kode aktif (bukan tebakan):
 *   - model: "model_best_final_float32 (1).tflite"  (dibuka via assets.openFd() di setupAI())
 *   - labels: "labels_new.txt"                        (dibuka via assets.open() di setupAI())
 *   - catalog: "motif_catalog.json"                    (baru ditambahkan, belum ada sebelumnya)
 */
class ModelBundleManager(private val context: Context) {

    companion object {
        private const val TAG = "TorajaBundle"

        const val DEFAULT_ASSET_MODEL_FILENAME = "model_best_final_float32 (1).tflite"
        const val DEFAULT_ASSET_LABELS_FILENAME = "labels_new.txt"
        const val DEFAULT_ASSET_CATALOG_FILENAME = "motif_catalog.json"
        const val DEFAULT_INPUT_SIZE = 320

        private const val PREFS_NAME = "toraja_bundle_prefs"
        private const val KEY_USING_ONLINE = "using_online"
        private const val KEY_MANIFEST_JSON = "active_manifest_json"
        private const val KEY_ACTIVE_MODEL_FILE = "active_model_file"
        private const val KEY_ACTIVE_LABELS_FILE = "active_labels_file"
        private const val KEY_ACTIVE_CATALOG_FILE = "active_catalog_file"

        fun sha256OfFile(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val baseDir = File(context.filesDir, "toraja_update")
    val tmpDir = File(baseDir, "tmp")
    val activeDir = File(baseDir, "active")
    val backupDir = File(baseDir, "backup")

    fun ensureDirs() {
        tmpDir.mkdirs()
        activeDir.mkdirs()
        backupDir.mkdirs()
    }

    // ── Status bundle online yang tersimpan ───────────────────────────────
    private fun isOnlineBundleFlagSet(): Boolean = prefs.getBoolean(KEY_USING_ONLINE, false)

    private fun savedManifest(): UpdateManifest? {
        val json = prefs.getString(KEY_MANIFEST_JSON, null) ?: return null
        return try {
            gson.fromJson(json, UpdateManifest::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Online bundle dianggap valid dipakai hanya jika: flag aktif + manifest tersimpan +
     * ketiga file (model/labels/catalog) benar-benar ada di folder active/.
     * Jika salah satu tidak konsisten, otomatis fallback ke assets (tidak pernah crash).
     */
    private fun resolveOnlineBundleIfValid(): Triple<UpdateManifest, File, File>? {
        if (!isOnlineBundleFlagSet()) return null
        val manifest = savedManifest() ?: return null
        val modelFileName = prefs.getString(KEY_ACTIVE_MODEL_FILE, null) ?: return null
        val labelsFileName = prefs.getString(KEY_ACTIVE_LABELS_FILE, null) ?: return null

        val modelFile = File(activeDir, modelFileName)
        val labelsFile = File(activeDir, labelsFileName)
        if (!modelFile.exists() || !labelsFile.exists()) {
            Log.d(TAG, "Online bundle flag set but files missing on disk, falling back to assets")
            return null
        }
        return Triple(manifest, modelFile, labelsFile)
    }

    private fun activeCatalogFile(): File? {
        if (!isOnlineBundleFlagSet()) return null
        val catalogFileName = prefs.getString(KEY_ACTIVE_CATALOG_FILE, null) ?: return null
        val file = File(activeDir, catalogFileName)
        return if (file.exists()) file else null
    }

    // ── Interpreter ────────────────────────────────────────────────────────
    data class InterpreterBundle(
        val interpreter: Interpreter,
        val source: BundleSource,
        val modelFileName: String,
        val inputShape: IntArray,
        val outputShape: IntArray
    )

    fun createInterpreter(options: Interpreter.Options): InterpreterBundle {
        val online = resolveOnlineBundleIfValid()
        val interpreter: Interpreter
        val source: BundleSource
        val modelFileName: String

        if (online != null) {
            val (_, modelFile, _) = online
            interpreter = Interpreter(modelFile, options)
            source = BundleSource.ONLINE
            modelFileName = modelFile.name
        } else {
            // Fallback: assets bawaan APK, cara load sama persis dengan setupAI() semula.
            val fd = context.assets.openFd(DEFAULT_ASSET_MODEL_FILENAME)
            val inputStream = FileInputStream(fd.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
            interpreter = Interpreter(modelBuffer, options)
            source = BundleSource.ASSETS
            modelFileName = DEFAULT_ASSET_MODEL_FILENAME
        }

        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        return InterpreterBundle(interpreter, source, modelFileName, inputShape, outputShape)
    }

    // ── Labels ──────────────────────────────────────────────────────────────
    fun loadActiveLabels(): List<String> {
        val online = resolveOnlineBundleIfValid()
        return if (online != null) {
            val (_, _, labelsFile) = online
            labelsFile.readLines()
        } else {
            context.assets.open(DEFAULT_ASSET_LABELS_FILENAME).bufferedReader().readLines()
        }
    }

    // ── Katalog motif ───────────────────────────────────────────────────────
    private val catalogType = object : TypeToken<Map<String, MotifInfo>>() {}.type

    fun loadActiveCatalog(): Map<String, MotifInfo> {
        val onlineCatalogFile = activeCatalogFile()
        val json = if (onlineCatalogFile != null) {
            onlineCatalogFile.readText()
        } else {
            try {
                context.assets.open(DEFAULT_ASSET_CATALOG_FILENAME).bufferedReader().readText()
            } catch (e: Exception) {
                return emptyMap()
            }
        }
        return try {
            gson.fromJson<Map<String, MotifInfo>>(json, catalogType) ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal parse motif_catalog.json: ${e.message}")
            emptyMap()
        }
    }

    /** Ambil info motif untuk satu label. Key harus sama persis dengan label aktif (poin 1 spesifikasi). */
    fun getMotifInfo(catalog: Map<String, MotifInfo>, rawLabel: String): MotifInfo {
        catalog[rawLabel]?.let { return it }
        // Fallback aman: label belum terdaftar di katalog, jangan pernah crash.
        val isUnknownClass = rawLabel.trim().lowercase() == "dataset_tidak_terdeteksi"
        return MotifInfo(
            display_name = rawLabel.replace("_", " "),
            category = "",
            philosophy = "",
            show_popup = !isUnknownClass
        )
    }

    // ── Info bundle aktif (untuk log & fungsi manual) ───────────────────────
    fun getActiveBundleInfo(): ActiveBundleInfo {
        val online = resolveOnlineBundleIfValid()
        val labels = loadActiveLabels()
        val catalog = loadActiveCatalog()

        return if (online != null) {
            val (manifest, modelFile, labelsFile) = online
            ActiveBundleInfo(
                isUsingOnlineBundle = true,
                activeBundleVersion = manifest.bundle_version,
                activeModelVersion = manifest.model_version,
                activeModelSource = "ONLINE",
                activeModelFileName = modelFile.name,
                activeLabelsFileName = labelsFile.name,
                activeCatalogFileName = prefs.getString(KEY_ACTIVE_CATALOG_FILE, "") ?: "",
                activeLabelsCount = labels.size,
                activeCatalogCount = catalog.size
            )
        } else {
            ActiveBundleInfo(
                isUsingOnlineBundle = false,
                activeBundleVersion = 0,
                activeModelVersion = "bundled",
                activeModelSource = "ASSETS",
                activeModelFileName = DEFAULT_ASSET_MODEL_FILENAME,
                activeLabelsFileName = DEFAULT_ASSET_LABELS_FILENAME,
                activeCatalogFileName = DEFAULT_ASSET_CATALOG_FILENAME,
                activeLabelsCount = labels.size,
                activeCatalogCount = catalog.size
            )
        }
    }

    fun localBundleVersion(): Int = savedManifest()?.bundle_version ?: 0

    /**
     * Dipanggil UpdateManager setelah bundle baru lolos SEMUA validasi.
     * Memindahkan file dari tmp/ ke active/ (active lama dipindah ke backup/ dulu),
     * lalu menyimpan manifest aktif ke SharedPreferences.
     */
    fun activateBundleFromTmp(manifest: UpdateManifest, tmpModelFile: File, tmpLabelsFile: File, tmpCatalogFile: File) {
        ensureDirs()

        // Backup active lama (best-effort, tidak fatal jika gagal)
        activeDir.listFiles()?.forEach { old ->
            try {
                val dest = File(backupDir, old.name)
                old.copyTo(dest, overwrite = true)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal backup file lama ${old.name}: ${e.message}")
            }
        }
        activeDir.listFiles()?.forEach { it.delete() }

        val newModelFile = File(activeDir, manifest.model_file)
        val newLabelsFile = File(activeDir, manifest.labels_file)
        val newCatalogFile = File(activeDir, manifest.catalog_file)
        tmpModelFile.copyTo(newModelFile, overwrite = true)
        tmpLabelsFile.copyTo(newLabelsFile, overwrite = true)
        tmpCatalogFile.copyTo(newCatalogFile, overwrite = true)

        prefs.edit()
            .putBoolean(KEY_USING_ONLINE, true)
            .putString(KEY_MANIFEST_JSON, gson.toJson(manifest))
            .putString(KEY_ACTIVE_MODEL_FILE, manifest.model_file)
            .putString(KEY_ACTIVE_LABELS_FILE, manifest.labels_file)
            .putString(KEY_ACTIVE_CATALOG_FILE, manifest.catalog_file)
            .apply()

        tmpDir.listFiles()?.forEach { it.delete() }
    }

    /** Hapus bundle online (jika ada) dan kembali memakai assets bawaan APK. */
    fun clearOnlineBundleAndUseAssets() {
        activeDir.listFiles()?.forEach { it.delete() }
        tmpDir.listFiles()?.forEach { it.delete() }
        prefs.edit()
            .putBoolean(KEY_USING_ONLINE, false)
            .remove(KEY_MANIFEST_JSON)
            .remove(KEY_ACTIVE_MODEL_FILE)
            .remove(KEY_ACTIVE_LABELS_FILE)
            .remove(KEY_ACTIVE_CATALOG_FILE)
            .apply()
        Log.d(TAG, "Online bundle dihapus, kembali memakai assets bawaan APK")
    }

    fun logStartupInfo(interpreterBundle: InterpreterBundle) {
        val info = getActiveBundleInfo()
        val labelCountMatches = interpreterBundle.outputShape.getOrNull(1) == info.activeLabelsCount
        Log.d(TAG, "using source = ${interpreterBundle.source}")
        Log.d(TAG, "asset model filename = $DEFAULT_ASSET_MODEL_FILENAME")
        Log.d(TAG, "asset labels filename = $DEFAULT_ASSET_LABELS_FILENAME")
        Log.d(TAG, "active model file = ${interpreterBundle.modelFileName}")
        Log.d(TAG, "active bundle version = ${info.activeBundleVersion}")
        Log.d(TAG, "labels count = ${info.activeLabelsCount}")
        Log.d(TAG, "catalog count = ${info.activeCatalogCount}")
        Log.d(TAG, "input tensor shape = ${interpreterBundle.inputShape.contentToString()}")
        Log.d(TAG, "output tensor shape = ${interpreterBundle.outputShape.contentToString()}")
        Log.d(TAG, "model-label valid = $labelCountMatches")
    }
}
