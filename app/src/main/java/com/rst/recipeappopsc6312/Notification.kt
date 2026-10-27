package com.rst.recipeappopsc6312

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    // This annotation tells Firestore to automatically map the document's ID to this field.
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val iconName: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    // Stored as "isRead". Without these annotations Firestore maps a Kotlin "isRead" property to
    // "read", so every notification looked unread and the red dot never cleared.
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false
)
