package com.example.torajacarvingai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesManager(context: Context) {
    private val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Mendapatkan daftar ID motif yang disukai
    fun getFavoriteIds(): Set<String> {
        val json = prefs.getString("list_favorites", null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // Menambahkan motif ke favorit
    fun addFavorite(idModel: String) {
        val currentFavorites = getFavoriteIds().toMutableSet()
        currentFavorites.add(idModel)
        prefs.edit().putString("list_favorites", gson.toJson(currentFavorites)).apply()
    }

    // Menghapus motif dari favorit
    fun removeFavorite(idModel: String) {
        val currentFavorites = getFavoriteIds().toMutableSet()
        currentFavorites.remove(idModel)
        prefs.edit().putString("list_favorites", gson.toJson(currentFavorites)).apply()
    }

    // Mengecek apakah suatu motif difavoritkan
    fun isFavorite(idModel: String): Boolean {
        return getFavoriteIds().contains(idModel)
    }

    // Mengubah status favorit (toggle)
    fun toggleFavorite(idModel: String): Boolean {
        return if (isFavorite(idModel)) {
            removeFavorite(idModel)
            false
        } else {
            addFavorite(idModel)
            true
        }
    }
}
