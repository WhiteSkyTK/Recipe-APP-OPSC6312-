package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DiscoverViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var isFetchingMore = false
    private var currentSortOption = "A-Z"
    private var userDiets: List<String> = emptyList()

    init {
        // Load user preferences first, which will then trigger the initial recipe load.
        loadUserPreferencesAndInitialRecipes()
    }

    private fun loadUserPreferencesAndInitialRecipes() {
        _isLoading.value = true
        viewModelScope.launch {
            // Step 1: Get the user's dietary filters
            val userId = FirebaseManager.auth.currentUser?.uid
            if (userId != null) {
                userDiets = repository.getUserDiets(userId)
            }

            // Step 2: Now that we have the filters, fetch the initial list
            repository.resetPagination()
            val firstPage = repository.getDiscoverPage(currentSortOption, 20, userDiets)
            _recipes.postValue(firstPage)
            _isLoading.postValue(false)
        }
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
            // Fetch the FIRST page for the new sort option, applying diet filters
            val firstPage = repository.getDiscoverPage(sortOption, 20, userDiets)
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
            // Fetch the NEXT page, applying diet filters
            val nextPage = repository.getDiscoverPage(currentSortOption, 20, userDiets)
            if (nextPage.isNotEmpty()) {
                // Append the new recipes to the existing list
                val currentList = _recipes.value ?: emptyList()
                _recipes.postValue(currentList + nextPage)
            }
            isFetchingMore = false
            _isLoading.postValue(false)
        }
    }

    fun search(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            // Use the more robust search function that checks the cache first and applies diet filters.
            val results = repository.searchRecipes(query, userDiets)
            _recipes.postValue(results)
            _isLoading.postValue(false)
        }
    }
}