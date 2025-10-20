package com.rst.recipeappopsc6312

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _networkStatus = MutableStateFlow(true)
    val networkStatus: StateFlow<Boolean> = _networkStatus.asStateFlow()

    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications.asStateFlow()

    private fun startListeningForNotifications() {
        val userId = Firebase.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.getNotificationsForUser(userId).collect { notificationsList ->
                _notifications.value = notificationsList
                _hasUnreadNotifications.value = notificationsList.any { !it.isRead }
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val userId = Firebase.auth.currentUser?.uid ?: return@launch

            // 1. Get the current list of notifications from our state.
            val unreadNotificationIds = _notifications.value
                .filter { !it.isRead } // Find the unread ones.
                .map { it.id }         // Get their unique document IDs.

            // 2. If there are any, tell the repository to update them.
            if (unreadNotificationIds.isNotEmpty()) {
                repository.markNotificationsAsRead(userId, unreadNotificationIds)
            }
        }
    }

    fun setNetworkStatus(isConnected: Boolean) {
        _networkStatus.value = isConnected
    }

    fun syncUserData() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
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
        startListeningForNotifications()
    }
}

