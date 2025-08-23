package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

open class BaseRecipeViewModel(internal val repository: ShoppingRepository) : ViewModel() {
    // This LiveData is the single source of truth for the full list of favorited recipes.
    val allFavorites: LiveData<List<Recipe>> = repository.getAllFavorites()

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.toggleFavorite(recipe)
        }
    }
}