package com.rst.recipeappopsc6312

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Full request/response logging only in debug builds; it printed API keys in release builds too.
    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private const val SPOONACULAR_BASE_URL = "https://api.spoonacular.com/"
    private const val TASTY_BASE_URL = "https://tasty.p.rapidapi.com/"

    // ++ This is now correctly named 'instance' and uses ApiService ++
    val instance: ApiService by lazy {
        val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor()).build()

        Retrofit.Builder()
            .baseUrl(SPOONACULAR_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val tastyInstance: TastyApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-host", "tasty.p.rapidapi.com")
                    .addHeader("x-rapidapi-key", BuildConfig.TASTY_API_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(TASTY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TastyApiService::class.java)
    }
}