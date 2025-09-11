package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

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