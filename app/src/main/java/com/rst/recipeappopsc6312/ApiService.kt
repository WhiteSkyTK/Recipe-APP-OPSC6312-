package com.rst.recipeappopsc6312

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int,
        @Query("tags") tags: String?
    ): SpoonacularApiResponse

    // ++ ADD THIS NEW FUNCTION for searching recipes ++
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int
    ): SpoonacularSearchResponse // Note: This uses a new response class
}
