package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BadgeDisplayInfo(
    val badge: Badge,
    val isEarned: Boolean
)

class BadgesViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _badgeList = MutableLiveData<List<BadgeDisplayInfo>>()
    val badgeList: LiveData<List<BadgeDisplayInfo>> = _badgeList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadBadges() {
        _isLoading.value = true
        val userId = FirebaseManager.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Fetch all possible badges and the user's progress in parallel
                val allBadges = repository.getAllBadges()
                val userProgress = repository.getUserProgress(userId)

                val earnedIds = userProgress?.earnedBadgeIds?.toSet() ?: emptySet()

                // Create the final list for the UI, marking which are earned
                val displayList = allBadges.map { badge ->
                    BadgeDisplayInfo(badge = badge, isEarned = badge.id in earnedIds)
                }.sortedByDescending { it.isEarned } // Show earned badges first

                _badgeList.postValue(displayList)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

