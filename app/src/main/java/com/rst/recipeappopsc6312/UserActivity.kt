package com.rst.recipeappopsc6312

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserActivity(
    val type: String = "",
    val value: String = "",
    val durationSeconds: Long? = null, // ++ ADD THIS for tracking view time
    @ServerTimestamp val timestamp: Date? = null
)