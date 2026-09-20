package com.example.torajacarvingai

import android.content.Context
import android.util.Log
import android.widget.ImageView
import coil.load

// Sumber data yang benar-benar dipakai untuk satu motif, dicatat untuk kebutuhan log/diagnostik.
enum class MotifDataSource { CATALOG, SUMBERDATA, FALLBACK_LABEL }

// Hasil resolusi satu label menjadi data siap-tampil, sudah menerapkan prioritas:
// motif_catalog.json aktif -> SumberData.kt -> label mentah.
data class ResolvedMotif(
    val label: String,
    val displayName: String,
    val category: String,
    val shortText: String,
    val fullText: String,
    val showPopup: Boolean,
    val imageRes: Int,
    val source: MotifDataSource,
    val aliases: List<String> = emptyList(),
    // URL gambar dari motif_catalog.json (kalau ada) -- HANYA diisi utk motif yang punya
    // entry di catalog dgn image_url terisi. Motif dari SumberData.kt/fallback label tidak
    // punya sumber URL sama sekali, jadi selalu "". Dipakai UI sbg prioritas di atas imageRes.
    val imageUrl: String = ""
)

/**
 * Tampilkan gambar motif: pakai [imageUrl] (dari motif_catalog.json, motif baru dari online
 * update) kalau ada isinya, kalau tidak fallback ke drawable lokal terkompilasi. Tidak pernah
 * crash walau URL tidak valid/gagal dimuat -- Coil otomatis jatuh ke [fallbackDrawableRes].
 */
fun loadMotifImage(imageView: ImageView, imageUrl: String, fallbackDrawableRes: Int) {
    if (imageUrl.isNotBlank()) {
        imageView.load(imageUrl) {
            placeholder(fallbackDrawableRes)
            error(fallbackDrawableRes)
            crossfade(true)
        }
    } else {
        imageView.setImageResource(fallbackDrawableRes)
    }
}

object MotifCatalogResolver {

    private fun isRejectionLabel(label: String): Boolean {
        val normalized = label.trim().lowercase()
        return normalized == "dataset_tidak_terdeteksi" || normalized == "tidak_terdeteksi" || normalized == "unknown"
    }

    /**
     * Resolusi satu label ke data tampilan, dengan prioritas:
     * 1) motif_catalog.json aktif (key harus sama persis dengan label)
     * 2) SumberData.kt (namaLayar/deskripsi/gambar lama, dicari case-insensitive + alias)
     * 3) Label mentah (tidak pernah crash walau data benar-benar tidak ada)
     *
     * Gambar (drawable) HANYA bisa datang dari SumberData.kt, karena motif_catalog.json
     * tidak (dan tidak bisa) membawa resource gambar terkompilasi. Motif baru dari online
     * yang belum punya drawable akan memakai ic_motif_placeholder.
     */
    fun resolve(label: String, catalog: Map<String, MotifInfo>): ResolvedMotif {
        val catalogEntry = catalog[label]
        val sumberEntry = SumberData.cariMotif(label)

        if (catalogEntry != null) {
            val philosophy = catalogEntry.philosophy
            return ResolvedMotif(
                label = label,
                displayName = catalogEntry.display_name.ifBlank { sumberEntry?.namaLayar ?: label.replace("_", " ") },
                category = catalogEntry.category.ifBlank { getKategoriPassura(label).namaLayar },
                shortText = philosophy.ifBlank { sumberEntry?.deskripsiSingkat ?: "Makna belum tersedia." },
                fullText = philosophy.ifBlank { sumberEntry?.deskripsiLengkap ?: "Detail tidak tersedia." },
                showPopup = catalogEntry.show_popup,
                imageRes = sumberEntry?.gambar ?: R.drawable.ic_motif_placeholder,
                source = MotifDataSource.CATALOG,
                aliases = sumberEntry?.aliases ?: emptyList(),
                imageUrl = catalogEntry.image_url
            )
        }

        if (sumberEntry != null) {
            return ResolvedMotif(
                label = label,
                displayName = sumberEntry.namaLayar,
                category = getKategoriPassura(label).namaLayar,
                shortText = sumberEntry.deskripsiSingkat,
                fullText = sumberEntry.deskripsiLengkap,
                showPopup = !isRejectionLabel(label),
                imageRes = sumberEntry.gambar,
                source = MotifDataSource.SUMBERDATA,
                aliases = sumberEntry.aliases
            )
        }

        return ResolvedMotif(
            label = label,
            displayName = label.replace("_", " "),
            category = "",
            shortText = "Makna belum tersedia.",
            fullText = "Detail tidak tersedia.",
            showPopup = !isRejectionLabel(label),
            imageRes = R.drawable.ic_motif_placeholder,
            source = MotifDataSource.FALLBACK_LABEL
        )
    }
}

// Membangun daftar motif untuk Katalog/Favorit dari LABELS AKTIF + CATALOG AKTIF,
// bukan dari SumberData.listMotif secara langsung, agar motif baru dari model online
// ikut muncul walau belum pernah ditulis di SumberData.kt.
object MotifDisplayBuilder {
    private const val TAG = "TorajaBundle"

    fun buildCatalogList(context: Context): List<MotifToraja> {
        val bundleManager = ModelBundleManager(context)
        val labels = bundleManager.loadActiveLabels()
        val catalog = bundleManager.loadActiveCatalog()
        val catalogSource = if (bundleManager.getActiveBundleInfo().isUsingOnlineBundle) "ONLINE" else "ASSETS"

        var hiddenCount = 0
        val result = labels.mapNotNull { label ->
            val resolved = MotifCatalogResolver.resolve(label, catalog)
            if (!resolved.showPopup) {
                hiddenCount++
                return@mapNotNull null
            }
            MotifToraja(
                idModel = resolved.label,
                namaLayar = resolved.displayName,
                deskripsiSingkat = resolved.shortText,
                deskripsiLengkap = resolved.fullText,
                gambar = resolved.imageRes,
                aliases = resolved.aliases,
                category = resolved.category,
                imageUrl = resolved.imageUrl
            )
        }

        // Kelompokkan dan urutkan katalog berdasarkan kelas Passura' (Garonto' Passura' ->
        // Passura' Todolo -> Passura' Malollek -> Passura' Pa'Barean), lalu alfabetis di
        // dalam tiap kelas -- supaya motif satu kategori tampil berdekatan di layar Katalog,
        // bukan mengikuti urutan indeks output model yang tidak berurutan per kelas.
        val sorted = result.sortedWith(
            compareBy(
                { getKategoriPassura(it.idModel).ordinal },
                { it.namaLayar.lowercase() }
            )
        )

        Log.d(TAG, "catalog source = $catalogSource")
        Log.d(TAG, "catalog list count = ${sorted.size}")
        Log.d(TAG, "hidden rejection labels count = $hiddenCount")
        return sorted
    }
}
