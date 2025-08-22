package com.rst.recipeappopsc6312

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.spoonacular.com/"
    private const val MEALDB_BASE_URL = "https://www.themealdb.com/"

    val instance: SpoonacularApiService by lazy {

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // ++ SET the client with the logger
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(SpoonacularApiService::class.java)
    }

    val mealDbInstance: TheMealDBApiService by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(MEALDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(TheMealDBApiService::class.java)
    }
}