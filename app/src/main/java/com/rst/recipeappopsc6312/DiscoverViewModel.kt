package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DiscoverViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentSortOption = "Recommended"
    private var isFetching = false
    private var canLoadMore = true

    fun setSortOption(sortOption: String) {
        if (sortOption == currentSortOption) return // No change

        currentSortOption = sortOption
        // Reset the list and pagination state
        _recipes.value = emptyList()
        canLoadMore = true
        isFetching = false
        // The repository's last document map will handle the cursor reset

        loadMoreRecipes()
    }

    fun loadMoreRecipes() {
        if (isFetching || !canLoadMore) return

        isFetching = true
        // Show the main progress bar only for the first page
        if (_recipes.value.isNullOrEmpty()) {
            _isLoading.value = true
        }

        viewModelScope.launch {
            val newRecipes = repository.getDiscoverPage(currentSortOption, 10)

            if (newRecipes.isNotEmpty()) {
                val currentList = _recipes.value ?: emptyList()
                _recipes.postValue(currentList + newRecipes)
            } else {
                // If we get an empty list, it means we've reached the end
                canLoadMore = false
            }

            _isLoading.postValue(false)
            isFetching = false
        }
    }
}