package com.rst.recipeappopsc6312

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RecipeApplication : Application() {

    // One repository for the whole app, so its in-memory caches are shared between screens.
    // (Each screen used to build its own, so data loaded by the splash screen was thrown away.)
    val repository: ShoppingRepository by lazy {
        val db = AppDatabase.getDatabase(this)
        ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
    }

    override fun onCreate() {
        super.onCreate()

        // This initializes Firebase and sets up offline caching.
        FirebaseManager.initialize(this)

        // Apply the saved theme on launch; it was previously only applied when changed.
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(prefs.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_NO))
    }
}

/** The app-wide repository. */
fun Context.sharedRepository(): ShoppingRepository =
    (applicationContext as RecipeApplication).repository
