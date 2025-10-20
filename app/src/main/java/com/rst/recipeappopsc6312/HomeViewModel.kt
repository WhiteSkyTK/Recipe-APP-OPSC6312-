package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    private val _featuredRecipes = MutableLiveData<List<Recipe>>()
    val featuredRecipes: LiveData<List<Recipe>> = _featuredRecipes

    private val _recommendedRecipes = MutableLiveData<List<Recipe>>()
    val recommendedRecipes: LiveData<List<Recipe>> = _recommendedRecipes

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // --- ** START OF "SURPRISE ME" LOGIC ** ---

    private val _surpriseRecipe = MutableLiveData<Recipe?>()
    val surpriseRecipe: LiveData<Recipe?> = _surpriseRecipe

    /**
     * Fetches the list of recommended recipes, takes the top one, and posts it.
     */
    fun findSurpriseRecipe() {
        viewModelScope.launch {
            val recommended = repository.getRecommendedForYou()
            _surpriseRecipe.postValue(recommended.firstOrNull())
        }
    }

    /**
     * Resets the surprise recipe value to null to prevent re-triggering on orientation change.
     */
    fun clearSurpriseRecipe() {
        _surpriseRecipe.value = null
    }

    // --- ** END OF "SURPRISE ME" LOGIC ** ---


    init {
        loadData()
    }

    fun refreshData() {
        loadData(forceRefresh = true)
    }

    private fun loadData(forceRefresh: Boolean = false) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.preloadHomeScreenData(forceRefresh)
            _featuredRecipes.postValue(repository.getFeaturedRecipes())
            _recommendedRecipes.postValue(repository.getRecommendedForYou())
            _categories.postValue(repository.getAllCategories())
            _isLoading.postValue(false)
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.toggleFavorite(recipe)
        }
    }
}

