package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap

class FavoritesViewModel(repository: ShoppingRepository) : ViewModel() {

    // 1. Get the LiveData list of favorite IDs from the repository.
    private val favoriteIds: LiveData<List<FavoriteRecipe>> = repository.getAllFavoriteIds()

    // 2. Use switchMap to automatically transform the list of IDs into a list of full Recipe objects.
    //    Whenever the favorite IDs change, this will automatically re-fetch the correct recipe details.
    val favoriteRecipes: LiveData<List<Recipe>> = repository.getAllFavorites()
}