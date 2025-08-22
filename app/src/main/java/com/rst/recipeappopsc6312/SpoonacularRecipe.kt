package com.rst.recipeappopsc6312

// This class matches the overall JSON object
data class SpoonacularRecipe(
    val id: Int,
    val title: String,
    val image: String?,
    val servings: Int,
    val readyInMinutes: Int,
    val sourceName: String?,
    val summary: String?,
    val extendedIngredients: List<SpoonacularIngredient>,
    val analyzedInstructions: List<SpoonacularInstruction>,
    val veryPopular: Boolean,
    val vegetarian: Boolean, // ++ ADD
    val vegan: Boolean,      // ++ ADD
    val glutenFree: Boolean, // ++ ADD
    val dairyFree: Boolean,
    val diets: List<String>,
    val ketogenic: Boolean, // ++ ADD
    val lowFodmap: Boolean, // ++ ADD
    val whole30: Boolean,   // ++ ADD (often similar to Paleo)
    val cuisines: List<String>?,
    val dishTypes: List<String>?
)

// A wrapper class because the API returns a list under a "recipes" key
data class RandomRecipeResponse(
    val recipes: List<SpoonacularRecipe>
)

data class SpoonacularIngredient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class SpoonacularInstruction(
    val name: String,
    val steps: List<SpoonacularStep>
)

data class SpoonacularStep(
    val number: Int,
    val step: String
)