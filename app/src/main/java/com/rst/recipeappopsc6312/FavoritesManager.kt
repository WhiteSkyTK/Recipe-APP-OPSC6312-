package com.rst.recipeappopsc6312

import android.content.Context

object FavoritesManager {

    private const val PREFS_NAME = "FavoritePrefs"
    private const val FAVORITES_KEY = "FavoriteRecipeIds"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFavorite(context: Context, recipeId: String): Boolean {
        return getFavoriteIds(context).contains(recipeId)
    }

    fun toggleFavorite(context: Context, recipeId: String) {
        val favorites = getFavoriteIds(context).toMutableSet()
        if (favorites.contains(recipeId)) {
            favorites.remove(recipeId)
        } else {
            favorites.add(recipeId)
        }
        saveFavoriteIds(context, favorites)
    }

    private fun getFavoriteIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
    }

    private fun saveFavoriteIds(context: Context, ids: Set<String>) {
        getPrefs(context).edit().putStringSet(FAVORITES_KEY, ids).apply()
    }
}