package com.rst.recipeappopsc6312

import retrofit2.http.GET
import retrofit2.http.Query

interface SpoonacularApiService {
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int,
        // We can pass a comma-separated list of diets, e.g., "vegetarian,gluten-free"
        @Query("tags") tags: String?
    ): RandomRecipeResponse
}