package com.rst.recipeappopsc6312

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    val title: String = "",
    val message: String = "",
    val iconName: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val isRead: Boolean = false
)