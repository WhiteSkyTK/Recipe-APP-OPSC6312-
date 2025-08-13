package com.rst.recipeappopsc6312

object DummyData {

    fun getFeaturedRecipes(): List<Recipe> {
        return listOf(
            Recipe("1", "Blackberry fro-yo", "https://images.unsplash.com/photo-1626201343755-75e1a72970a4", 5, true),
            Recipe("5", "Cheesecake flatbread", "https://images.unsplash.com/photo-1588195538326-c5b1e9f80a1b", 15)
        )
    }

    fun getRecommendedRecipes(): List<Recipe> {
        return listOf(
            Recipe("1", "Blueberry muffins", "https://images.unsplash.com/photo-1593443320739-73f44974a104", 40, true),
            Recipe("2", "Blackberry fool", "https://images.unsplash.com/photo-1626201343755-75e1a72970a4", 20),
            Recipe("3", "Frozen yoghurt cake", "https://images.unsplash.com/photo-1563805042-76295533", 15),
            Recipe("4", "Limoncello fro-yo", "https://images.unsplash.com/photo-1567206563064-6f60f40a2b57", 10, true)
        )
    }

    fun getCategories(): List<Category> {
        return listOf(
            Category("Breakfast", isSelected = true),
            Category("Lunch"),
            Category("Dinner"),
            Category("Dessert"),
            Category("Snacks")
        )
    }

    // This list will be used for both the popup and the full fragment
    fun getNotifications(): List<Notification> {
        return listOf(
            Notification("New Update Available", "Today / 10:52 PM", R.drawable.ic_alert),
            Notification("New Recipes Added", "Yesterday / 04:01 PM", R.drawable.ic_new_recipe)
        )
    }
}