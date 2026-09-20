package com.example.torajacarvingai

// Struktur version.json yang diterbitkan server untuk update model/labels/katalog.
data class UpdateManifest(
    val schema_version: Int = 1,
    val bundle_version: Int = 0,
    val model_version: String = "",
    val labels_version: String = "",
    val catalog_version: String = "",
    val input_size: Int = 0,
    val class_count: Int = 0,
    val model_file: String = "",
    val labels_file: String = "",
    val catalog_file: String = "",
    val model_url: String = "",
    val labels_url: String = "",
    val catalog_url: String = "",
    val model_sha256: String = "",
    val labels_sha256: String = "",
    val catalog_sha256: String = "",
    val min_app_version_code: Int = 0,
    val release_notes: String = ""
)

// Satu entri motif di motif_catalog.json. Key map harus sama persis dengan label di labels file.
data class MotifInfo(
    val display_name: String = "",
    val category: String = "",
    val philosophy: String = "",
    val show_popup: Boolean = true,
    val image_url: String = ""
)

// Ringkasan bundle yang sedang aktif dipakai aplikasi saat ini (assets atau online).
data class ActiveBundleInfo(
    val isUsingOnlineBundle: Boolean,
    val activeBundleVersion: Int,
    val activeModelVersion: String,
    val activeModelSource: String, // "ASSETS" atau "ONLINE"
    val activeModelFileName: String,
    val activeLabelsFileName: String,
    val activeCatalogFileName: String,
    val activeLabelsCount: Int,
    val activeCatalogCount: Int
)

enum class BundleSource { ASSETS, ONLINE }

// Hasil proses checkForUpdates() — dipakai untuk log/status, bukan untuk mengubah UI utama.
sealed class UpdateResult {
    data class UpToDate(val currentBundleVersion: Int) : UpdateResult()
    data class Updated(val newBundleVersion: Int, val manifest: UpdateManifest) : UpdateResult()
    data class NeedsAppUpdate(val minAppVersionCode: Int, val currentAppVersionCode: Int) : UpdateResult()
    data class Failed(val reason: String) : UpdateResult()
}
