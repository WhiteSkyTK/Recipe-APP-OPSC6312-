package com.rst.recipeappopsc6312

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String, // Must match the Supabase auth user ID
    val username: String,
    val email: String,
    val full_name: String?,
    val phone_number: String?,
    val country: String?,
    val gender: String?,
    val date_of_birth: String?,
    val selected_cuisines: List<String>?,
    val selected_diets: List<String>?
    // The profile image URL would be handled separately via Supabase Storage
)