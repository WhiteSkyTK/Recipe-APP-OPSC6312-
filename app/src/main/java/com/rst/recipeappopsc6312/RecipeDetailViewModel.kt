package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.launch

class RecipeDetailViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    private val _recipe = MediatorLiveData<Recipe?>()
    val recipe: LiveData<Recipe?> = _recipe

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isFavorite = MediatorLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> get() = _isFavorite

    // We need to keep track of the old source to remove it later
    private var favoriteStatusSource: LiveData<Boolean>? = null

    private var currentRecipeId: String? = null

    fun fetchRecipe(recipeId: String) {
        currentRecipeId = recipeId
        _isLoading.value = true

        // When isFavorite status changes, re-fetch the whole recipe
        val isFavoriteSource = repository.isFavorite(recipeId)
        _recipe.addSource(isFavoriteSource) { isFav ->
            // When the favorite status changes, re-run the main fetch logic
            // to get the recipe object with the updated isFavorite property.
            viewModelScope.launch {
                val currentRecipe = _recipe.value
                if (currentRecipe != null && currentRecipe.isFavorite != isFav) {
                    fetchAndPostRecipe(recipeId)
                }
            }
        }

        // Initial fetch
        fetchAndPostRecipe(recipeId)
    }

    private fun fetchAndPostRecipe(recipeId: String) {
        viewModelScope.launch {
            val fetchedRecipe = repository.getRecipeById(recipeId)
            if (fetchedRecipe != null) {
                _recipe.postValue(fetchedRecipe)
            } else {
                _error.postValue("Recipe not found.")
            }
            _isLoading.postValue(false)
        }
    }


    fun publishCurrentRecipe() {
        val currentRecipe = recipe.value ?: return
        if (!currentRecipe.isPublic) {
            viewModelScope.launch {
                repository.publishRecipe(currentRecipe)
                // The LiveData will update automatically after the database changes
            }
        }
    }

    fun unpublishCurrentRecipe() {
        val currentRecipe = recipe.value ?: return
        if (currentRecipe.isPublic) {
            viewModelScope.launch {
                repository.unpublishRecipe(currentRecipe)
            }
        }
    }
}
