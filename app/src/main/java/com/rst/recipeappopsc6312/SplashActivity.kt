package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"

    // Get an instance of our new SplashViewModel
    private val viewModel: SplashViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        SplashViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()

        Log.d(TAG, "onCreate: Splash screen started.")

        // --- Start Your Animations ---
        val logoImageView = findViewById<ImageView>(R.id.imageViewLogo)
        val appNameTextView = findViewById<TextView>(R.id.textViewAppName)
        val splashLayout = findViewById<View>(R.id.splash)

        ViewCompat.setOnApplyWindowInsetsListener(splashLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val growInAnimation = AnimationUtils.loadAnimation(this, R.anim.grow_in)
        logoImageView.startAnimation(growInAnimation)

        try {
            val customTypeface = ResourcesCompat.getFont(this, R.font.islandmoments_regular)
            appNameTextView.typeface = customTypeface
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom font.", e)
        }

        // --- Smart Loading Logic ---
        // Observe the navigation target LiveData from the ViewModel
        viewModel.navigationTarget.observe(this) { target ->
            // When the ViewModel tells us it's ready, we navigate
            val nextActivity = when (target) {
                NavigationTarget.MAIN_ACTIVITY -> MainActivity::class.java
                NavigationTarget.WELCOME_ACTIVITY -> WelcomeActivity::class.java
            }
            startActivity(Intent(this, nextActivity))
            // Finish the splash screen so the user can't go back to it
            finish()
        }

        // Tell the ViewModel to start the loading process in the background
        // The animations will run while this is happening.
        viewModel.startLoading()
    }
}
