package com.rst.recipeappopsc6312

import kotlin.math.min

object DummyData {

    // A master list of all possible recipes with full details

/*
    fun getRecipeById(id: String): Recipe? {
        return allRecipes.find { it.id == id }
    }

    fun getFeaturedRecipes(): List<Recipe> {
        return allRecipes.filter { it.id in listOf("1", "10", "16") }
    }

    fun getRecommendedRecipes(): List<Recipe> {
        return allRecipes.filter { it.id in listOf("2", "5", "9") }
    }

    fun getDiscoverRecipes(page: Int, pageSize: Int): List<Recipe> {
        val start = page * pageSize
        val end = min(start + pageSize, allRecipes.size)
        return if (start < end) allRecipes.subList(start, end) else emptyList()
    }

    fun getAllCategories(): List<Category> {
        // This is the full list for the CategoryFragment
        return listOf(
            Category("Breakfast", isSelected = true),
            Category("Lunch"),
            Category("Dinner"),
            Category("Fruits"),
            Category("Dairy product"),
            Category("Protein"),
            Category("Cereal"),
            Category("Grain"),
            Category("Egg"),
            Category("Vegetables"),
            Category("Dairy"),
            Category("Sweets"),
            Category("Beverages"),
            Category("Spices"),
            Category("Snacks"),
            Category("Meaty"),
        )
    }

    fun getNotifications(): List<Notification> {
        return listOf(
            Notification("New Update Available", "Today / 10:52 PM", R.drawable.ic_alert),
            Notification("New Recipes Added", "Yesterday / 04:01 PM", R.drawable.ic_new_recipe)
        )
    }

    fun getRecommendedRecipes(): List<Recipe> = allRecipes
    fun getFeaturedRecipes(): List<Recipe> = allRecipes.shuffled().take(5)
    fun getBreakfastRecipes(): List<Recipe> = allRecipes.filter { it.mealType == "Breakfast" }
    fun getLunchRecipes(): List<Recipe> = allRecipes.filter { it.mealType == "Lunch" }
    fun getDinnerRecipes(): List<Recipe> = allRecipes.filter { it.mealType == "Dinner" }
    fun getSnackRecipes(): List<Recipe> = allRecipes.filter { it.mealType == "Snack" || it.mealType == "Dessert" }
    fun getAllCategories(): List<Category> {
        return allRecipes.map { Category(it.category) }.distinctBy { it.name }.sortedBy { it.name }
    }

 */



    fun getNotifications(): List<Notification> {
        return listOf(
            Notification("New Update Available", "Today / 10:52 PM", R.drawable.ic_alert),
            Notification("New Recipes Added", "Yesterday / 04:01 PM", R.drawable.ic_new_recipe)
        )
    }


}
