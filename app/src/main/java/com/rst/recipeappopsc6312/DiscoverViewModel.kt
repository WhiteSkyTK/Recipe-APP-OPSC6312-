package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DiscoverViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _recipeUpdated = MutableLiveData<Pair<String, Boolean>>()
    val recipeUpdated: LiveData<Pair<String, Boolean>> = _recipeUpdated


    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val pageSize = 10 // How many recipes to load at a time

    init {
        loadMoreRecipes() // Load the first page
    }

    fun loadMoreRecipes() {
        if (_isLoading.value == true) return // Prevent multiple loads at once

        _isLoading.value = true
        viewModelScope.launch {
            val newRecipes = repository.getDiscoverRecipes(pageSize)
            // Add the new recipes to the existing list
            val currentList = _recipes.value ?: emptyList()
            _recipes.postValue(currentList + newRecipes)
            _isLoading.postValue(false)
        }
    }

}