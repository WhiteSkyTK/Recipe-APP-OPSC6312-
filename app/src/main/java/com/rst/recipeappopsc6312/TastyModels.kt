package com.rst.recipeappopsc6312

data class TastyApiResponse(
    val results: List<TastyRecipe>
)

data class TastyRecipe(
    val id: Int,
    val name: String,
    val thumbnail_url: String?,
    val description: String?,
    val cook_time_minutes: Int?,
    val prep_time_minutes: Int?,
    val total_time_minutes: Int?,
    val num_servings: Int?,
    val user_ratings: TastyUserRatings?,
    val sections: List<TastySection>?,
    val nutrition: TastyNutrition?,
    val instructions: List<TastyInstruction>?,
    val tags: List<TastyTag>?
)

data class TastyUserRatings(
    val score: Double?
)

data class TastyNutrition(
    val calories: Int?,
    val fat: Int?,
    val carbohydrates: Int?,
    val fiber: Int?,
    val protein: Int?,
    val sugar: Int?
)

data class TastySection(
    val components: List<TastyComponent>
)

data class TastyComponent(
    val raw_text: String,
    val ingredient: TastyIngredient,
    val measurements: List<TastyMeasurement>
)

data class TastyIngredient(
    val name: String
)

data class TastyInstruction(
    val display_text: String
)

data class TastyTag(
    val type: String,
    val name: String,
    val display_name: String
)

data class TastyMeasurement(
    val unit: TastyUnit,
    val quantity: String
)

data class TastyUnit(
    val name: String,
    val display_singular: String
)
