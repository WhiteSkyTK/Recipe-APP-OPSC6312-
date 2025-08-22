package com.rst.recipeappopsc6312

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_recipes")
data class FavoriteRecipe(
    @PrimaryKey val id: String,
    // You can add any other fields from the original Recipe object
    // that you want to store directly in the favorites table.
    // For simplicity, we'll start with just the ID and fetch details later.
)