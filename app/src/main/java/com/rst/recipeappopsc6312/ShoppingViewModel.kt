package com.rst.recipeappopsc6312

import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ShoppingViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    // For the full-screen loading animation on first launch
    private val _isInitiallyLoading = MutableLiveData<Boolean>()
    val isInitiallyLoading: LiveData<Boolean> = _isInitiallyLoading

    // For the smaller pull-to-refresh animation
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // --- LiveData for each section of the Home Screen ---
    private val _featuredRecipes = MutableLiveData<List<Recipe>>()
    val featuredRecipes: LiveData<List<Recipe>> = _featuredRecipes

    private val _timeOfDayTitle = MutableLiveData<String>()
    val timeOfDayTitle: LiveData<String> = _timeOfDayTitle

    private val _timeOfDayRecipes = MutableLiveData<List<Recipe>>()
    val timeOfDayRecipes: LiveData<List<Recipe>> = _timeOfDayRecipes

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _recommendedRecipes = MutableLiveData<List<Recipe>>()
    val recommendedRecipes: LiveData<List<Recipe>> = _recommendedRecipes

    init {
        loadHomeScreenData()
    }

    fun loadHomeScreenData() {
        _isInitiallyLoading.value = true
        viewModelScope.launch {
            try {
                repository.preloadHomeScreenData(forceRefresh = false)
                populateLiveDataFromCache()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load home screen data", e)
            } finally {
                _isInitiallyLoading.postValue(false)
            }
        }
    }

    fun refreshHomeScreenData() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                repository.preloadHomeScreenData(forceRefresh = true)
                populateLiveDataFromCache()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to refresh home screen data", e)
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }

    private suspend fun populateLiveDataFromCache() {
        _featuredRecipes.postValue(repository.getFeaturedRecipes())
        _recommendedRecipes.postValue(repository.getRecommendedForYou())
        _categories.postValue(repository.getAllCategories())

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _timeOfDayRecipes.postValue(getRecipesForTimeOfDay(hour))
    }

    fun onCategorySelected(categoryName: String) {
        viewModelScope.launch {
            _isInitiallyLoading.value = true // You can use a separate loading state for this if you prefer

            // Re-fetch recommended recipes with the selected category filter
            val filteredRecommendations = repository.getRecommendedForYou(forceRefresh = true, category = categoryName)
            _recommendedRecipes.postValue(filteredRecommendations)

            _isInitiallyLoading.postValue(false)
        }
    }

    private suspend fun getRecipesForTimeOfDay(hour: Int): List<Recipe> {
        return when (hour) {
            in 5..10 -> repository.getBreakfastRecipes()
            in 11..13 -> repository.getLunchRecipes()
            in 14..17 -> repository.getSnackRecipes()
            in 18..21 -> repository.getDinnerRecipes()
            else -> repository.getSnackRecipes()
        }
    }
}

