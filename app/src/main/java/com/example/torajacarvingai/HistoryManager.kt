package com.example.torajacarvingai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Struktur data yang disimpan.
// "label" = idModel/label mentah dari AI (mis. "pa_tedong"), dipakai untuk resolve ulang
// display_name/category/philosophy dari catalog aktif saat ditampilkan. Nullable + default
// agar entri riwayat LAMA (sebelum field ini ada) tetap terbaca tanpa crash — RiwayatActivity
// fallback ke "nama" kalau "label" kosong.
data class HistoryModel(val nama: String, val tanggal: String, val label: String? = null)

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Fungsi untuk menyimpan riwayat baru
    fun saveHistory(nama: String, tanggal: String, label: String? = null) {
        val currentHistory = getHistory().toMutableList()
        currentHistory.add(0, HistoryModel(nama, tanggal, label)) // Tambah ke urutan teratas
        val json = gson.toJson(currentHistory)
        prefs.edit().putString("list_history", json).apply()
    }

    // Fungsi untuk mengambil daftar riwayat
    fun getHistory(): List<HistoryModel> {
        val json = prefs.getString("list_history", null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryModel>>() {}.type
        return gson.fromJson(json, type)
    }
}