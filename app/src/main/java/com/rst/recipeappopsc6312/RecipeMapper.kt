package com.rst.recipeappopsc6312

import android.text.Html

/**
 * Converts a recipe from the Spoonacular API to our app's internal Recipe model.
 */
fun SpoonacularRecipe.toAppRecipe(): Recipe {
    val highQualityImageUrl = if (this.image != null && this.imageType != null) {
        "https://img.spoonacular.com/recipes/${this.id}-636x393.${this.imageType}"
    } else { "" }

    val fullDescription = this.summary?.let {
        Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString()
    } ?: "No description available."

    return Recipe(
        id = "sp_${this.id}",
        title = this.title,
        imageUrl = highQualityImageUrl,
        timeInMins = this.readyInMinutes,
        servings = this.servings,
        author = this.sourceName ?: "Spoonacular",
        description = fullDescription,
        isPublic = true,
        category = this.cuisines?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "General",
        mealType = this.dishTypes?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Dinner",
        ingredients = this.extendedIngredients.map {
            Ingredient(it.name, it.amount.toString(), it.unit)
        },
        method = this.analyzedInstructions.flatMap { instruction -> instruction.steps.map { MethodStep(it.step) } },
        isVegan = this.vegan,
        isVegetarian = this.vegetarian,
        isGlutenFree = this.glutenFree,
        isDairyFree = this.dairyFree,
        diets = this.diets,
        isKeto = this.ketogenic,
        isLowFodmap = this.lowFodmap,
        isPaleo = this.whole30,
        isPopular = this.veryPopular,
        nutrition = emptyList()
    )
}

/**
 * Converts a recipe from the Tasty API to our app's internal Recipe model.
 */
fun TastyRecipe.toAppRecipe(): Recipe {
    val totalTime = this.total_time_minutes?.takeIf { it > 0 }
        ?: (this.cook_time_minutes ?: 0) + (this.prep_time_minutes ?: 0)

    val category = this.tags?.find { it.type == "cuisine" }?.display_name ?: "General"
    val mealType = this.tags?.find { it.type == "meal" }?.display_name ?: "General"

    val ingredients = this.sections?.flatMap { section ->
        section.components.mapNotNull { component ->
            val measurement = component.measurements.firstOrNull()
            val quantityString = measurement?.quantity
            val parsableQuantity = convertFractionStringToDoubleString(quantityString)
            val quantityValue = parsableQuantity?.toDoubleOrNull()

            if (quantityValue != null && quantityValue > 0) {
                Ingredient(
                    name = component.ingredient.name.replaceFirstChar { it.uppercase() },
                    quantity = quantityString!!,
                    unit = measurement.unit.display_singular
                )
            } else { null }
        }
    } ?: emptyList()

    val fullDescription = this.description?.ifBlank { "No description available." } ?: "No description available."

    val nutritionFacts = mutableListOf<NutritionFact>()
    this.nutrition?.let {
        it.calories?.let { cal -> nutritionFacts.add(NutritionFact("Calories", cal.toString())) }
        it.fat?.let { fat -> nutritionFacts.add(NutritionFact("Fat", "${fat}g")) }
        it.carbohydrates?.let { carbs -> nutritionFacts.add(NutritionFact("Carbs", "${carbs}g")) }
        it.protein?.let { pro -> nutritionFacts.add(NutritionFact("Protein", "${pro}g")) }
        it.sugar?.let { sug -> nutritionFacts.add(NutritionFact("Sugar", "${sug}g")) }
    }

    val tagNames = this.tags?.map { it.name }?.toSet() ?: emptySet()
    val isVegan = tagNames.contains("vegan")
    val isVegetarian = tagNames.contains("vegetarian")
    val isGlutenFree = tagNames.contains("gluten_free")
    val isDairyFree = tagNames.contains("dairy_free")
    val isKeto = tagNames.contains("keto")
    val dietsList = this.tags?.filter { it.type == "dietary" }?.map { it.display_name } ?: emptyList()

    return Recipe(
        id = "tasty_${this.id}",
        title = this.name,
        imageUrl = this.thumbnail_url ?: "",
        description = fullDescription,
        timeInMins = if (totalTime > 0) totalTime else 30,
        servings = this.num_servings ?: 0,
        author = "Tasty",
        isPublic = true,
        category = category,
        mealType = mealType,
        ingredients = ingredients,
        method = this.instructions?.map { MethodStep(it.display_text) } ?: emptyList(),
        isVegan = isVegan,
        isVegetarian = isVegetarian,
        isGlutenFree = isGlutenFree,
        isDairyFree = isDairyFree,
        isKeto = isKeto,
        isPaleo = false,
        isLowFodmap = false,
        diets = dietsList,
        nutrition = nutritionFacts
    )
}

private fun convertFractionStringToDoubleString(input: String?): String? {
    if (input == null) return null
    val fractionMap = mapOf(
        "½" to 0.5, "1/2" to 0.5, "⅓" to 0.333, "1/3" to 0.333,
        "⅔" to 0.667, "2/3" to 0.667, "¼" to 0.25, "1/4" to 0.25,
        "¾" to 0.75, "3/4" to 0.75, "⅛" to 0.125, "1/8" to 0.125,
        "⅜" to 0.375, "3/8" to 0.375, "⅝" to 0.625, "5/8" to 0.625,
        "⅞" to 0.875, "7/8" to 0.875
    )
    var normalized = input.trim()
    val mixedRegex = Regex("""(\d+)\s+(\d+)/(\d+)""")
    val mixedMatch = mixedRegex.find(normalized)
    if (mixedMatch != null) {
        val whole = mixedMatch.groupValues[1].toDouble()
        val num = mixedMatch.groupValues[2].toDouble()
        val den = mixedMatch.groupValues[3].toDouble()
        return (whole + num / den).toString()
    }
    for ((fraction, value) in fractionMap) {
        if (normalized.contains(fraction)) {
            normalized = normalized.replace(fraction, value.toString())
        }
    }
    val fractionRegex = Regex("""(\d+)/(\d+)""")
    val match = fractionRegex.find(normalized)
    if (match != null) {
        val numerator = match.groupValues[1].toDouble()
        val denominator = match.groupValues[2].toDouble()
        normalized = normalized.replace(match.value, (numerator / denominator).toString())
    }
    return normalized
}
