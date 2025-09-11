package com.rst.recipeappopsc6312

import android.app.Application
import com.cloudinary.android.MediaManager

class RecipeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // This initializes Firebase and sets up offline caching.
        FirebaseManager.initialize(this)

        // ++ ADD THIS to initialize Cloudinary with your secure URL ++
        // It reads the URL from your local.properties via BuildConfig.
        MediaManager.init(this, mapOf("cloud_url" to BuildConfig.CLOUDINARY_URL))
    }
}