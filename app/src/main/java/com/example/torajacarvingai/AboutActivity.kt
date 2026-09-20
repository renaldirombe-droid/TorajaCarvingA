package com.example.torajacarvingai // Sesuaikan dengan package name Anda

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AboutActivity : AppCompatActivity() {

    private lateinit var updateManager: UpdateManager
    private lateinit var txtActiveBundleInfo: TextView
    private lateinit var txtInfoModel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        updateManager = UpdateManager(this)

        val btnBack: ImageButton = findViewById(R.id.btnBack)

        // Logika untuk menutup Halaman Tentang saat tombol back diklik
        btnBack.setOnClickListener {
            finish()
        }

        txtActiveBundleInfo = findViewById(R.id.txtActiveBundleInfo)
        txtInfoModel = findViewById(R.id.txtInfoModel)
        val btnCekUpdate: MaterialButton = findViewById(R.id.btnCekUpdate)
        val btnHapusUpdateOnline: MaterialButton = findViewById(R.id.btnHapusUpdateOnline)
        val btnTampilkanVersi: MaterialButton = findViewById(R.id.btnTampilkanVersi)

        renderActiveBundleInfo()

        // Debug/manual entry point Tahap 3 — belum ada halaman setting khusus,
        // jadi diletakkan minimal di halaman Tentang tanpa redesign besar.
        btnCekUpdate.setOnClickListener {
            Toast.makeText(this, "Mengecek update...", Toast.LENGTH_SHORT).show()
            Log.d("TorajaBundle", "Tombol Cek Update ditekan")
            updateManager.checkForUpdates { result ->
                runOnUiThread {
                    when (result) {
                        is UpdateResult.UpToDate -> {
                            Log.d("TorajaBundle", "Cek Update: sudah versi terbaru (bundle_version=${result.currentBundleVersion})")
                            Toast.makeText(this, "Sudah versi terbaru (bundle_version=${result.currentBundleVersion})", Toast.LENGTH_LONG).show()
                        }
                        is UpdateResult.Updated -> {
                            // Jangan hot-swap interpreter saat scanner aktif -- cukup minta buka ulang app.
                            Log.d("TorajaBundle", "Cek Update: BERHASIL, bundle_version baru=${result.newBundleVersion}")
                            Toast.makeText(
                                this,
                                "Update berhasil. Tutup dan buka ulang aplikasi untuk memakai model terbaru.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is UpdateResult.NeedsAppUpdate -> {
                            Log.d("TorajaBundle", "Cek Update: perlu update APK (min=${result.minAppVersionCode}, current=${result.currentAppVersionCode})")
                            Toast.makeText(
                                this,
                                "Aplikasi perlu diupdate dulu (min versi ${result.minAppVersionCode})",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is UpdateResult.Failed -> {
                            // Detail lengkap sudah di-Log.e oleh UpdateManager; Toast cukup ringkas.
                            Log.e("TorajaBundle", "Cek Update: GAGAL -> ${result.reason}")
                            Toast.makeText(this, "Update gagal, tetap pakai model saat ini.", Toast.LENGTH_LONG).show()
                        }
                    }
                    renderActiveBundleInfo()
                }
            }
        }

        btnHapusUpdateOnline.setOnClickListener {
            updateManager.clearOnlineBundleAndUseAssets()
            Toast.makeText(this, "Update online dihapus, kembali memakai model bawaan APK. Buka ulang aplikasi.", Toast.LENGTH_LONG).show()
            renderActiveBundleInfo()
        }

        btnTampilkanVersi.setOnClickListener {
            renderActiveBundleInfo()
            val info = updateManager.getActiveBundleInfo()
            Toast.makeText(
                this,
                "Sumber: ${info.activeModelSource} | bundle_version=${info.activeBundleVersion} | labels=${info.activeLabelsCount} | katalog=${info.activeCatalogCount}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun renderActiveBundleInfo() {
        val info = updateManager.getActiveBundleInfo()
        txtActiveBundleInfo.text = "Sumber aktif: ${info.activeModelSource}\n" +
            "Versi bundle: ${info.activeBundleVersion} (model: ${info.activeModelVersion})\n" +
            "Model: ${info.activeModelFileName}\n" +
            "Labels: ${info.activeLabelsFileName} (${info.activeLabelsCount} kelas)\n" +
            "Katalog: ${info.activeCatalogFileName} (${info.activeCatalogCount} entri)"

        // "Jumlah Kelas" harus ikut jumlah label AKTIF sesungguhnya -- supaya tidak basi
        // (mis. tetap tertulis "31" padahal sudah update online jadi 37 kelas).
        txtInfoModel.text = "Arsitektur Model: MobileNetV2\n" +
            "Input Size: 320x320\n" +
            "Runtime: Float32 (Reference)\n" +
            "Jumlah Kelas: ${info.activeLabelsCount}\n" +
            "Format Model: TensorFlow Lite (.tflite)\n" +
            "Akurasi Validasi: 98,25%"
    }
}