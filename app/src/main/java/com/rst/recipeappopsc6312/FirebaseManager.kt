package com.rst.recipeappopsc6312

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    val auth: FirebaseAuth by lazy {
        Log.d(TAG, "Initializing Firebase Auth")
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val storage: FirebaseStorage by lazy {
        Log.d(TAG, "Initializing Firebase Storage")
        FirebaseStorage.getInstance()
    }

    // This is the new function that will be called by your Application class
    fun initialize(context: Context) {
        val db = FirebaseFirestore.getInstance()

        // This is the corrected way to enable offline caching
        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { /* You can configure size here if needed */ })
        }
        db.firestoreSettings = settings

        Log.d("FirebaseManager", "Firestore offline persistence has been enabled.")
    }
}