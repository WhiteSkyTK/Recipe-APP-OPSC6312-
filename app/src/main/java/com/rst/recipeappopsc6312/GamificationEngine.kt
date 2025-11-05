package com.rst.recipeappopsc6312

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * The "brain" of the gamification system. This class defines all available badges
 * and contains the logic for checking if a user has earned new ones.
 */
class GamificationEngine(
    private val firestore: FirebaseFirestore,
    private val repository: ShoppingRepository
) {

    // This is our central list of all possible badges in the app.
    val allBadges = listOf(
        Badge("chef_apprentice", "Chef's Apprentice", "Create your first recipe.", "ic_badge_first_recipe"),
        Badge("sous_chef", "Sous Chef", "Create 5 recipes.", "ic_badge_sous_chef"),
        Badge("executive_chef", "Executive Chef", "Create 20 recipes.", "ic_badge_executive_chef"),
        Badge("first_favorite", "First Favorite", "Save your first favorite recipe.", "ic_badge_first_favorite"),
        Badge("curious_cook", "Curious Cook", "Save 10 favorite recipes.", "ic_badge_curious_cook"),
        Badge("recipe_collector", "Recipe Collector", "Save 50 favorite recipes.", "ic_badge_recipe_collector"),
        Badge("welcome_back", "Welcome Back!", "Log in two days in a row.", "ic_badge_login_streak_2"),
        Badge("dedicated_diner", "Dedicated Diner", "Log in for 7 consecutive days.", "ic_badge_login_streak_7")
    )

    /**
     * The main function that checks a user's progress and awards new badges.
     */
    suspend fun checkAndAwardBadges(userId: String) {
        val progressRef = firestore.collection("users").document(userId).collection("progress").document("main")
        val progress = progressRef.get().await().toObject(UserProgress::class.java) ?: UserProgress(userId = userId)

        val newlyEarnedBadges = mutableListOf<Badge>()

        for (badge in allBadges) {
            // Check if the user already has this badge
            if (badge.id in progress.earnedBadgeIds) continue

            // Check if the user meets the criteria for this badge
            val hasEarned = when (badge.id) {
                "chef_apprentice" -> progress.recipesCreated >= 1
                "sous_chef" -> progress.recipesCreated >= 5
                "executive_chef" -> progress.recipesCreated >= 20
                "first_favorite" -> progress.recipesFavorited >= 1
                "curious_cook" -> progress.recipesFavorited >= 10
                "recipe_collector" -> progress.recipesFavorited >= 50
                "welcome_back" -> progress.loginStreak >= 2
                "dedicated_diner" -> progress.loginStreak >= 7
                else -> false
            }

            if (hasEarned) {
                newlyEarnedBadges.add(badge)
            }
        }

        if (newlyEarnedBadges.isNotEmpty()) {
            val updatedBadgeIds = progress.earnedBadgeIds + newlyEarnedBadges.map { it.id }
            progressRef.update("earnedBadgeIds", updatedBadgeIds).await()
            Log.d("Gamification", "User $userId earned ${newlyEarnedBadges.size} new badges.")

            // Send a notification for the first new badge earned in this check
            val firstNewBadge = newlyEarnedBadges.first()
            repository.createNotification(
                userId, // <-- Pass the userId here
                "Badge Unlocked! 🎉",
                "You've earned the '${firstNewBadge.title}' badge!",
                firstNewBadge.iconName
            )
        }
    }
}
