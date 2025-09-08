package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(private val repository: ShoppingRepository) : ViewModel() {

    // This LiveData will tell the activity where to navigate
    private val _navigationTarget = MutableLiveData<NavigationTarget>()
    val navigationTarget: LiveData<NavigationTarget> = _navigationTarget

    fun startLoading() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val user = FirebaseManager.auth.currentUser

            if (user != null) {
                // If the user IS logged in, pre-load the recipes...
                repository.getPublicRecipes(forceRefresh = false)
                // ...and then navigate to the main app.
                _navigationTarget.postValue(NavigationTarget.MAIN_ACTIVITY)
            } else {
                // If the user is NOT logged in, navigate directly to the welcome screen.
                // We do NOT try to load any recipes.
                _navigationTarget.postValue(NavigationTarget.WELCOME_ACTIVITY)
            }

            // Ensure the splash screen is visible for a minimum time for the animation
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 3000) {
                delay(3000 - elapsedTime)
            }
        }
    }
}

// A simple enum to represent the possible navigation destinations
enum class NavigationTarget {
    MAIN_ACTIVITY,
    WELCOME_ACTIVITY
}