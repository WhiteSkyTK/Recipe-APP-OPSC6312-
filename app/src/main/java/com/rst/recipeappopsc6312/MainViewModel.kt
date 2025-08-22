package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ShoppingRepository) : ViewModel() {
    // This LiveData will always hold the current list of favorite IDs from the database.
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    val favoriteIds: LiveData<List<FavoriteRecipe>> = repository.getAllFavoriteIds()


    fun fetchNotifications() {
        viewModelScope.launch {
            _notifications.postValue(repository.getNotifications())
        }
    }

    init {
        // This will run once when the app starts.
        // It will check Firestore and only fetch new recipes if needed.
        viewModelScope.launch {
            repository.seedFirebaseDatabase()
        }
    }
}