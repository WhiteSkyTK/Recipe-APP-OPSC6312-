package com.rst.recipeappopsc6312

import android.text.Html

// This extension function converts a SpoonacularRecipe to our app's Recipe model
fun SpoonacularRecipe.toAppRecipe(): Recipe {
    // Create a short, clean subtitle from the first 3-4 ingredients.
    val subtitle = "With: " + this.extendedIngredients
        .take(4)
        .joinToString(", ") { it.name }

    // Clean up the long summary by removing HTML tags and trimming it.
    val cleanSummary = this.summary?.let {
        val strippedText = Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString()
        // Take the first full sentence, up to a maximum of 200 characters.
        strippedText.substringBefore(". ").take(200) + if (strippedText.length > 200) "..." else "."
    } ?: "No description available."

    return Recipe(
        // ++ ADD a prefix to the ID
        id = "sp_${this.id}",
        title = this.title,
        imageUrl = this.image ?: "",
        timeInMins = this.readyInMinutes,
        servings = this.servings,
        // ++ MAP sourceName to author
        author = this.sourceName ?: "Spoonacular",
        description = cleanSummary,
        isPublic = true,
        category = this.cuisines?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "General",
        mealType = this.dishTypes?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Dinner",
        ingredients = this.extendedIngredients.map {
            Ingredient(it.name, it.amount.toString(), it.unit)
        },
        isVegan = this.vegan,           // ++ ADD
        isVegetarian = this.vegetarian, // ++ ADD
        isGlutenFree = this.glutenFree, // ++ ADD
        isDairyFree = this.dairyFree,   // ++ ADD
        diets = this.diets, // ++ ADD
        isKeto = this.ketogenic,          // ++ ADD
        isLowFodmap = this.lowFodmap,   // ++ ADD
        isPaleo = this.whole30,           // ++ ADD (Using whole30 as a proxy for Paleo)
        isPopular = this.veryPopular, // ++ ADD
        method = this.analyzedInstructions.flatMap { instruction ->
            instruction.steps.map { MethodStep(it.step) }
        },
        nutrition = emptyList()
    )
}

fun TheMealDBRecipe.toAppRecipe(): Recipe {
    val ingredients = mutableListOf<Ingredient>()
    // This loop cleverly combines the 20 ingredient and measure fields
    for (i in 1..20) {
        val ingredientName = getProperty<String?>(this, "strIngredient$i")
        val measure = getProperty<String?>(this, "strMeasure$i")
        if (!ingredientName.isNullOrBlank() && !measure.isNullOrBlank()) {
            ingredients.add(Ingredient(name = ingredientName, quantity = measure, unit = ""))
        }
    }

    return Recipe(
        id = "tmdb_${this.idMeal}", // Prefix the ID to avoid conflicts
        title = this.strMeal,
        imageUrl = this.strMealThumb ?: "",
        description = this.strInstructions ?: "No instructions available.",
        category = this.strCategory ?: "General",
        mealType = this.strCategory ?: "General", // Use category as mealType
        author = this.strArea ?: "TheMealDB",
        isPublic = true,
        ingredients = ingredients,
        method = this.strInstructions?.split("\r\n")
            ?.filter { it.isNotBlank() }
            ?.map { MethodStep(it) } ?: emptyList()
        // Other fields can be left as default
    )
}

// Helper function to dynamically get properties from the data class
@Suppress("UNCHECKED_CAST")
private fun <T> getProperty(instance: Any, propertyName: String): T {
    return instance::class.java.getDeclaredField(propertyName).let {
        it.isAccessible = true
        it.get(instance) as T
    }
}