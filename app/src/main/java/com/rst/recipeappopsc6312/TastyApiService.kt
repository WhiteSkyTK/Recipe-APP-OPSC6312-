package com.rst.recipeappopsc6312

import retrofit2.http.GET
import retrofit2.http.Query

interface TastyApiService {
    // This will list recipes. We'll start by getting a few.
    @GET("recipes/list")
    suspend fun listRecipes(
        @Query("from") from: Int,
        @Query("size") size: Int,
        @Query("q") query: String? // For searching, e.g., "dessert"
    ): TastyApiResponse
}