package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap

class FavoritesViewModel(repository: ShoppingRepository) : ViewModel() {
    // This is now very simple and directly gets the list of full Recipe objects
    val favoriteRecipes: LiveData<List<Recipe>> = repository.getAllFavorites()
}