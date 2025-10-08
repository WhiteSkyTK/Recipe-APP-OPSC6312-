package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DiscoverViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isInitiallyLoading = MutableLiveData(true)
    val isInitiallyLoading: LiveData<Boolean> = _isInitiallyLoading

    private val _isFetchingMore = MutableLiveData(false)
    val isFetchingMore: LiveData<Boolean> = _isFetchingMore

    private var currentSortOption = "A-Z"
    private var canLoadMore = true
    private var lastQuery = ""
    private var userDiets: List<String> = emptyList()

    init {
        // Load user preferences first, which will then trigger the initial recipe load.
        loadUserPreferencesAndInitialRecipes()
    }

    private fun loadUserPreferencesAndInitialRecipes() {
        _isInitiallyLoading.value = true
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
            _isInitiallyLoading.postValue(false)
        }
    }

    fun setSortOption(sortOption: String) {
        // Don't reload if the option hasn't changed
        if (sortOption == currentSortOption && _recipes.value?.isNotEmpty() == true) return

        currentSortOption = sortOption
        lastQuery = ""
        _recipes.value = emptyList()
        canLoadMore = true
        _isInitiallyLoading.value = true

        // Reset pagination in the repository before fetching
        repository.resetPagination()

        viewModelScope.launch {
            val firstPage = repository.getDiscoverPage(sortOption, 20, userDiets)
            _recipes.postValue(firstPage)
            _isInitiallyLoading.postValue(false)
        }
    }

    fun loadMoreRecipes() {
        if (_isFetchingMore.value == true || !canLoadMore || lastQuery.isNotEmpty()) return

        _isFetchingMore.value = true
        viewModelScope.launch {
            val nextPage = repository.getDiscoverPage(currentSortOption, 10, userDiets)
            if (nextPage.isNotEmpty()) {
                val currentList = _recipes.value ?: emptyList()
                _recipes.postValue(currentList + nextPage)
            } else {
                canLoadMore = false
            }
            _isFetchingMore.postValue(false)
        }
    }
    fun search(query: String) {
        lastQuery = query
        _isInitiallyLoading.value = true
        viewModelScope.launch {
            val results = repository.searchRecipes(query, userDiets)
            _recipes.postValue(results)
            _isInitiallyLoading.postValue(false)
            canLoadMore = false // Disable pagination for search results
        }
    }
}
