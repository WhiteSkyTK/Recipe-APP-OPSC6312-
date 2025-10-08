package com.rst.recipeappopsc6312

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single, definable badge that a user can earn in the app.
 * This will be stored in a public 'badges' collection in Firestore.
 */
data class Badge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "" // The name of the drawable resource, e.g., "ic_badge_first_recipe"
)

/**
 * Represents a user's progress towards earning badges.
 * This will be stored in a private subcollection: /users/{userId}/progress/main
 */
data class UserProgress(
    val userId: String = "",
    val recipesCreated: Int = 0,
    val recipesFavorited: Int = 0,
    val loginStreak: Int = 0,
    @ServerTimestamp val lastLogin: Date? = null,
    val earnedBadgeIds: List<String> = emptyList()
)

