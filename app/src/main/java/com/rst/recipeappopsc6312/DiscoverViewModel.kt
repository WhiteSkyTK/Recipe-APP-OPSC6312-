package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DiscoverViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    // This is the list that is actually displayed on the screen (sorted or filtered).
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var isFetchingMore = false
    private var currentSortOption = "A-Z"

    init {
        // Load the very first page when the ViewModel is created
        setSortOption(currentSortOption)
    }

    fun setSortOption(sortOption: String) {
        // Don't reload if the option hasn't changed
        if (sortOption == currentSortOption && _recipes.value?.isNotEmpty() == true) return

        currentSortOption = sortOption
        _isLoading.value = true
        isFetchingMore = true // Block loading more while we do a fresh load

        // Reset pagination in the repository before fetching
        repository.resetPagination()

        viewModelScope.launch {
            // Fetch the FIRST page for the new sort option
            val firstPage = repository.getDiscoverPage(sortOption, 20)
            _recipes.postValue(firstPage) // Replace the current list
            _isLoading.postValue(false)
            isFetchingMore = false
        }
    }

    fun loadMoreRecipes() {
        // Prevent loading if we are already loading or if the list is empty
        if (isFetchingMore || _recipes.value.isNullOrEmpty()) return

        _isLoading.value = true
        isFetchingMore = true

        viewModelScope.launch {
            // Fetch the NEXT page
            val nextPage = repository.getDiscoverPage(currentSortOption, 20)
            if (nextPage.isNotEmpty()) {
                // Append the new recipes to the existing list
                val currentList = _recipes.value ?: emptyList()
                _recipes.postValue(currentList + nextPage)
            }
            isFetchingMore = false
            _isLoading.postValue(false) // You might want a different progress indicator for "loading more"
        }
    }

    fun search(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            // Search no longer uses the master list. It goes straight to the repository.
            val results = repository.searchRecipesFromApis(query)
            _recipes.postValue(results)
            _isLoading.postValue(false)
        }
    }
}