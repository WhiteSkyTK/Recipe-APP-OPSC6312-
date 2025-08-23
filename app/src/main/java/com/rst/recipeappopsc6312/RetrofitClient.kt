package com.rst.recipeappopsc6312

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.spoonacular.com/"

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

    private const val TASTY_BASE_URL = "https://tasty.p.rapidapi.com/"

    val tastyInstance: TastyApiService by lazy {
        // 1. Create a logger to see the API calls
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        // 2. Create a custom OkHttpClient that adds the required headers
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // This intercepts every request and adds the headers
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-host", "tasty.p.rapidapi.com")
                    // IMPORTANT: Get your key securely from BuildConfig
                    .addHeader("x-rapidapi-key", BuildConfig.TASTY_API_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()

        // 3. Build Retrofit using this custom client
        Retrofit.Builder()
            .baseUrl(TASTY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TastyApiService::class.java)
    }

}