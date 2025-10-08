package com.rst.recipeappopsc6312

import android.util.Log
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
        _isInitiallyLoading.value = true // Trigger the full-screen loader
        viewModelScope.launch {
            // Fetch all data in parallel
            _featuredRecipes.postValue(repository.getFeaturedRecipes())
            _recommendedRecipes.postValue(repository.getPublicRecipes())
            _categories.postValue(repository.getAllCategories())

            // Time of day logic
            _timeOfDayTitle.postValue(GreetingManager.getRandomGreetingForCurrentTime())
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            _timeOfDayRecipes.postValue(when (hour) {
                in 5..10 -> repository.getBreakfastRecipes()
                in 11..13 -> repository.getLunchRecipes()
                in 18..21 -> repository.getDinnerRecipes()
                else -> repository.getSnackRecipes()
            })

            _isInitiallyLoading.postValue(false) // Hide the loader
        }
    }

    fun refreshHomeScreenData() {
        _isRefreshing.value = true // Trigger the pull-to-refresh loader
        viewModelScope.launch {
            // Re-fetch all data with forceRefresh = true
            _featuredRecipes.postValue(repository.getFeaturedRecipes(true))
            _recommendedRecipes.postValue(repository.getPublicRecipes(true))
            _categories.postValue(repository.getAllCategories(true))

            _timeOfDayTitle.postValue(GreetingManager.getRandomGreetingForCurrentTime())
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            _timeOfDayRecipes.postValue(when (hour) {
                in 5..10 -> repository.getBreakfastRecipes(true)
                in 11..13 -> repository.getLunchRecipes(true)
                in 18..21 -> repository.getDinnerRecipes(true)
                else -> repository.getSnackRecipes(true)
            })

            _isRefreshing.postValue(false) // Hide the loader
        }
    }
}

