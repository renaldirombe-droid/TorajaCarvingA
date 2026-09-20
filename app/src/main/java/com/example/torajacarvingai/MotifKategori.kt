package com.example.torajacarvingai

// Kategori utama Passura' Toraja untuk pengelompokan motif hasil klasifikasi AI
enum class KategoriPassura(val namaLayar: String) {
    GARONTO_PASSURA("Garonto’ Passura"),
    PASSURA_TODOLO("Passura’ Todolo"),
    PASSURA_MALOLLEK("Passura’ Malollek"),
    PASSURA_PABAREAN("Passura’ Pa’Barean"),
    TIDAK_TERDETEKSI("Tidak Terdeteksi")
}

// Menyamakan variasi penulisan label (huruf besar/kecil, ' vs ’, tanda kurung, spasi/underscore)
// agar label dari model & label di peta kategori selalu bertemu di key yang sama.
private fun normalizeLabelForKategori(label: String): String {
    return label
        .trim()
        .lowercase()
        .replace("’", "")
        .replace("'", "")
        .replace("(", "")
        .replace(")", "")
        .replace(Regex("\\s+"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
}

private val PETA_KATEGORI_PASSURA: Map<String, KategoriPassura> = buildMap {
    val garontoPassura = listOf(
        "pa_bare_allo", "pa_manuk_londong", "pa_tedong", "pa_sussuk"
    )
    val passuraTodolo = listOf(
        "pa_doti", "pa_doti_pandin", "pa_limbongan", "pa_lolo_tabang",
        "pa_kapu_baka", "pa_bulu_londong", "pa_erong", "pa_tedong_tumuru",
        "pa_don_bolu", "pa_bulintong_somba"
    )
    val passuraMalollek = listOf(
        "pa_barana", "pa_baranae'(Passape_Bai)", "pa_re_po", "pa_tangke_lumu",
        "pa_tangke_lumu_ditoke", "pa_don_paria", "pa_batang_lau"
    )
    val passuraPaBarean = listOf(
        "pa_pollo_gayang", "pa_kadang_pao", "pa_tangki_pattung", "pa_tanduk_rape",
        "pa_bombo_uai", "pa_rangga_ulu", "pa_sekong_kandaure", "pa_gayang", "pa_papan_kandaure"
    )

    garontoPassura.forEach { put(normalizeLabelForKategori(it), KategoriPassura.GARONTO_PASSURA) }
    passuraTodolo.forEach { put(normalizeLabelForKategori(it), KategoriPassura.PASSURA_TODOLO) }
    passuraMalollek.forEach { put(normalizeLabelForKategori(it), KategoriPassura.PASSURA_MALOLLEK) }
    passuraPaBarean.forEach { put(normalizeLabelForKategori(it), KategoriPassura.PASSURA_PABAREAN) }

    put(normalizeLabelForKategori("dataset_tidak_terdeteksi"), KategoriPassura.TIDAK_TERDETEKSI)
    put(normalizeLabelForKategori("tidak_terdeteksi"), KategoriPassura.TIDAK_TERDETEKSI)
    put(normalizeLabelForKategori("unknown"), KategoriPassura.TIDAK_TERDETEKSI)
}

// Fallback aman ke TIDAK_TERDETEKSI jika label tidak dikenali, tidak pernah crash.
fun getKategoriPassura(label: String): KategoriPassura {
    val normalized = normalizeLabelForKategori(label)
    return PETA_KATEGORI_PASSURA[normalized] ?: KategoriPassura.TIDAK_TERDETEKSI
}
