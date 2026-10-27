package com.rst.recipeappopsc6312

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int,
        // Recipes must have *all* of these tags, so pass one tag per request.
        @Query("include-tags") tags: String?
    ): SpoonacularApiResponse

    // Returns IDs and titles only; load the full recipes with getRecipeInformationBulk.
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int
    ): SpoonacularSearchResponse

    @GET("recipes/informationBulk")
    suspend fun getRecipeInformationBulk(
        @Query("apiKey") apiKey: String,
        @Query("ids") ids: String
    ): List<SpoonacularRecipe>
}
