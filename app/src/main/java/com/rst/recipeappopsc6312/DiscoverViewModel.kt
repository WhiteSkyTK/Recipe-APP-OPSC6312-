package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DiscoverViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    // This holds the full list of all recipes from the Firebase cache.
    private var masterRecipeList = listOf<Recipe>()

    // This is the list that is actually displayed on the screen (sorted or filtered).
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentSortOption = "Recommended"

    init {
        // Start loading the full cache in the background as soon as the screen is opened
        warmUpCache()
        // Load the initial view with the "Recommended" list
        setSortOption("Recommended")
    }

    fun setSortOption(sortOption: String) {
        currentSortOption = sortOption
        _isLoading.value = true
        viewModelScope.launch {
            val sortedList = when (sortOption) {
                "A-Z" -> masterRecipeList.sortedBy { it.title }
                "Recommended" -> repository.getRecommendedForYou()
                "Popular" -> repository.getPopularRecipes()
                // These now sort the master list that's already in memory, making them instant.
                "Cook Time" -> masterRecipeList.sortedBy { it.timeInMins }
                "Z-A" -> masterRecipeList.sortedByDescending { it.title }
                else -> masterRecipeList
            }
            _recipes.postValue(sortedList)
            _isLoading.postValue(false)
        }
    }

    fun search(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            // Step 1: Search the local master list first for instant results.
            val localResults = masterRecipeList.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.ingredients.any { i -> i.name.contains(query, ignoreCase = true) }
            }
            _recipes.postValue(localResults) // Show local results immediately

            // Step 2: Simultaneously, search the APIs in the background for new recipes.
            val apiResults = repository.searchRecipesFromApis(query)

            // Step 3: Combine the results, remove duplicates, and update the UI.
            val finalResults = (localResults + apiResults).distinctBy { it.id }
            _recipes.postValue(finalResults)

            // Also update the master list so the new recipes are included in future local searches
            masterRecipeList = (masterRecipeList + apiResults).distinctBy { it.id }

            _isLoading.postValue(false)
        }
    }

    // This function runs in the background to keep the master list ready.
    private fun warmUpCache() {
        viewModelScope.launch {
            masterRecipeList = repository.getPublicRecipes(forceRefresh = false)
        }
    }
}