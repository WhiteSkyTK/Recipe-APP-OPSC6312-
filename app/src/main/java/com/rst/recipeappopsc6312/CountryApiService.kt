package com.rst.recipeappopsc6312

// In CountryApiService.kt
import retrofit2.Call
import retrofit2.http.GET

interface CountryApiService {
    @GET("v3.1/all?fields=name,cca2,flags")
    fun getAllCountries(): Call<List<Country>>
}