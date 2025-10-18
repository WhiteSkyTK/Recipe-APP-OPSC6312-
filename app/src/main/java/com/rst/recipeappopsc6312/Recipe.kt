package com.rst.recipeappopsc6312

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.firestore.IgnoreExtraProperties // Good practice for Firestore
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "recipes") // Defines the table name
@TypeConverters(
    IngredientListConverter::class,
    MethodStepListConverter::class,
    NutritionFactListConverter::class
)
@IgnoreExtraProperties // Add this for flexibility with Firestore
data class Recipe(
    @PrimaryKey var id: String = "", // Default value added
    var userId: String = "",         // Default value added
    var title: String = "",          // Default value added
    var isPublic: Boolean = false,
    var imageUrl: String = "",       // Default value added
    var timeInMins: Int = 0,         // Default value added
    val isFavorite: Boolean = false,
    var author: String = "", // Already has a default
    var description: String = "",         // Already has a default
    var servings: Int = 0,                // Already has a default
    var category: String = "",
    val mealType: String = "",
    var nutrition: List<NutritionFact> = emptyList(), // Already has a default
    var ingredients: List<Ingredient> = emptyList(),  // Already has a default
    val dietTags: List<String> = emptyList(),
    val isVegan: Boolean = false,      // ++ ADD
    val isVegetarian: Boolean = false, // ++ ADD
    val isGlutenFree: Boolean = false, // ++ ADD
    val isDairyFree: Boolean = false,  // ++ ADD
    val diets: List<String> = emptyList(), // ++ ADD
    val isPopular: Boolean = false, // ++ ADD
    val isKeto: Boolean = false,      // ++ ADD
    val isLowFodmap: Boolean = false, // ++ ADD
    val isPaleo: Boolean = false,     // ++ ADD (we'll map whole30 to this)
    val method: List<MethodStep> = emptyList()
) : Parcelable

// Helper data classes
@Parcelize
@IgnoreExtraProperties // Add for helper classes too if they are directly mapped from sub-collections or complex fields
data class NutritionFact(
    var label: String = "", // Default value added
    var value: String = ""  // Default value added
) : Parcelable

@Parcelize
@IgnoreExtraProperties
data class Ingredient(
    var name: String = "",    // Default value added
    var quantity: String = "",
    val unit: String = "" // Default value added
) : Parcelable

@Parcelize
@IgnoreExtraProperties
data class MethodStep(
    var step: String = "",          // Default value added
    var isCompleted: Boolean = false // Already has a default
) : Parcelable
