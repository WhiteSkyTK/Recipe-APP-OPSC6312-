package com.rst.recipeappopsc6312

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    val auth: FirebaseAuth by lazy {
        Log.d(TAG, "Initializing Firebase Auth")
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        Log.d(TAG, "Initializing Firestore")
        val db = FirebaseFirestore.getInstance()
        // This is the magic line that enables offline caching
        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
        db.firestoreSettings = settings
        Log.d(TAG, "Firestore offline persistence enabled.")
        db
    }

    val storage: FirebaseStorage by lazy {
        Log.d(TAG, "Initializing Firebase Storage")
        FirebaseStorage.getInstance()
    }
}