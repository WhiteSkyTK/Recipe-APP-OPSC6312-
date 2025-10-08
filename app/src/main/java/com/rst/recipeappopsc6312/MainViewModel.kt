package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _networkStatus = MutableLiveData<Boolean>()
    val networkStatus: LiveData<Boolean> = _networkStatus

    private val _hasUnreadNotifications = MutableLiveData<Boolean>()
    val hasUnreadNotifications: LiveData<Boolean> = _hasUnreadNotifications

    fun fetchNotifications() {
        viewModelScope.launch {
            val fetchedNotifications = repository.getNotifications()
            _notifications.postValue(fetchedNotifications)
            // Check if any of the fetched notifications are unread
            _hasUnreadNotifications.postValue(fetchedNotifications.any { !it.isRead })
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            // After updating, fetch the list again to update the UI state
            fetchNotifications()
        }
    }

    fun setNetworkStatus(isConnected: Boolean) {
        _networkStatus.value = isConnected
    }

    fun syncUserData() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                // This tells the repository to download favorites from Firebase
                // and save them into the local Room database.
                repository.syncFavoritesFromFirebase(userId)
            }
        }
    }

    fun logUserLogin() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                repository.logUserLogin(userId)
            }
        }
    }

    fun updateNotificationSubscription(isEnabled: Boolean) {
        repository.updateNotificationSubscription(isEnabled)
    }

    init {
        viewModelScope.launch {
            repository.seedFirebaseDatabase()
        }
        // Fetch notifications once when the ViewModel is created
        fetchNotifications()
    }
}