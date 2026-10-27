package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

open class BaseRecipeViewModel(internal val repository: ShoppingRepository) : ViewModel() {
    // This LiveData is the single source of truth for the full list of favorited recipes.
    val allFavorites: LiveData<List<Recipe>> = repository.getAllFavorites()

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            // toggleFavorite() already updates the badge progress. Logging it here as well
            // counted every favourite twice.
            repository.toggleFavorite(recipe)
        }
    }
}

