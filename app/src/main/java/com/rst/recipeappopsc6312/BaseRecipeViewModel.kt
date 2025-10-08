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
            // First, check the REAL current favorite status before toggling.
            val isCurrentlyFavorite = repository.isFavoriteNow(recipe.id)
            val isNowFavoriting = !isCurrentlyFavorite

            // Perform the toggle operation in the repository.
            repository.toggleFavorite(recipe)

            // After the toggle, log the action to the gamification engine.
            Firebase.auth.currentUser?.uid?.let { userId ->
                repository.logFavoriteToggled(userId, isNowFavoriting)
            }
        }
    }
}

