package com.rst.recipeappopsc6312

// Parcelable allows you to pass this object between activities/fragments
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String, // A unique ID for each recipe
    val title: String,
    val imageUrl: String, // URL for the image
    val timeInMins: Int,
    var isFavorite: Boolean = false
) : Parcelable