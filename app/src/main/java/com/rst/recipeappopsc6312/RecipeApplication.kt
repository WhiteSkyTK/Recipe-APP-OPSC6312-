package com.rst.recipeappopsc6312

import android.app.Application

class RecipeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // This will initialize Firebase and set up offline caching
        // once and for all when the app starts.
        FirebaseManager.initialize(this)
    }
}