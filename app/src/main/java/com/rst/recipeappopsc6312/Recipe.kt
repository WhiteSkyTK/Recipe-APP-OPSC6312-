package com.rst.recipeappopsc6312

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Main data class for a full recipe
@Parcelize
data class Recipe(
    val id: String,
    val title: String,
    val imageUrl: String,
    val timeInMins: Int,
    var isFavorite: Boolean = false,
    val author: String = "Harmony Kitchen",
    val description: String = "",
    var servings: Int = 4,
    val nutrition: List<NutritionFact> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val method: List<MethodStep> = emptyList()
) : Parcelable

// Helper data classes
@Parcelize
data class NutritionFact(val label: String, val value: String) : Parcelable
@Parcelize
data class Ingredient(val name: String, val quantity: String) : Parcelable
@Parcelize
data class MethodStep(val step: String, var isCompleted: Boolean = false) : Parcelable