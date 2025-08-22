package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Change 'protected' to 'internal' to make it visible within your app module
open class BaseRecipeViewModel(internal val repository: ShoppingRepository) : ViewModel() {

    // ++ ADD this LiveData here so all child ViewModels have it ++
    val favoriteIds: LiveData<List<FavoriteRecipe>> = repository.getAllFavoriteIds()

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.toggleFavorite(recipe)
        }
    }
}