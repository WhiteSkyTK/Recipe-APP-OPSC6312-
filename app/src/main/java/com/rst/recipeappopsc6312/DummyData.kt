package com.rst.recipeappopsc6312

import kotlin.math.min

object DummyData {
    fun getNotifications(): List<Notification> {
        return listOf(
            Notification("New Update Available", "Today / 10:52 PM", R.drawable.ic_alert),
            Notification("New Recipes Added", "Yesterday / 04:01 PM", R.drawable.ic_new_recipe)
        )
    }
}