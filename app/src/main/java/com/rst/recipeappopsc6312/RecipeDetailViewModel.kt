package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private var favoriteStatusSource: LiveData<Boolean>? = null
    private var currentRecipeId: String? = null

    fun fetchRecipe(recipeId: String) {
        currentRecipeId = recipeId
        _isLoading.value = true

        favoriteStatusSource?.let { _isFavorite.removeSource(it) }
        favoriteStatusSource = repository.isFavorite(recipeId)
        _isFavorite.addSource(favoriteStatusSource!!) { isFavoriteValue ->
            _isFavorite.value = isFavoriteValue
        }

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

    // ++ This function now correctly lives in the ViewModel ++
    fun logRecipeView(durationSeconds: Long) {
        currentRecipeId?.let {
            repository.logRecipeView(it, durationSeconds)
        }
    }

    fun publishCurrentRecipe() {
        val currentRecipe = recipe.value ?: return
        if (!currentRecipe.isPublic) {
            viewModelScope.launch {
                repository.publishRecipe(currentRecipe)
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

