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

    fun fetchNotifications() {
        viewModelScope.launch {
            _notifications.postValue(repository.getNotifications())
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

    init {
        // This will run once when the app starts.
        // It will check Firestore and only fetch new recipes if needed.
        viewModelScope.launch {
            repository.seedFirebaseDatabase()
        }
    }
}