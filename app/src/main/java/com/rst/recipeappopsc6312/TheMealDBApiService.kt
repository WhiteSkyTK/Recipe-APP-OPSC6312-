package com.rst.recipeappopsc6312

import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDBApiService {
    // This function will fetch a single random recipe
    @GET("api/json/v1/1/random.php")
    suspend fun getRandomRecipe(): TheMealDBResponse

    // This function will filter recipes by category
    @GET("api/json/v1/1/filter.php")
    suspend fun filterByCategory(@Query("c") category: String): TheMealDBResponse

    @GET("api/json/v1/1/search.php")
    suspend fun searchByName(@Query("s") query: String): TheMealDBResponse

    @GET("api/json/v1/1/lookup.php")
    suspend fun lookupById(@Query("i") id: String): TheMealDBResponse
}