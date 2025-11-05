package com.rst.recipeappopsc6312

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    // This annotation tells Firestore to automatically map the document's ID to this field.
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val iconName: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    var isRead: Boolean = false
)
