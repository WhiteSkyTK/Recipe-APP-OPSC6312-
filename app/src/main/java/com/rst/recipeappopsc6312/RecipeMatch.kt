package com.rst.recipeappopsc6312

// This class holds a recipe and the list of ingredients the user is missing
data class RecipeMatch(
    val recipe: Recipe,
    val missingIngredients: List<String>
)