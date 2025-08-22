package com.rst.recipeappopsc6312

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()

        Log.d(TAG, "onCreate: Splash screen started.")

        val logoImageView = findViewById<ImageView>(R.id.imageViewLogo)
        val splashLayout = findViewById<View>(R.id.splash) // Add this ID to your root layout in XML
        val appNameTextView = findViewById<TextView>(R.id.textViewAppName)

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(splashLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        // 1. Start the "grow in" animation for the logo
        val growInAnimation = AnimationUtils.loadAnimation(this, R.anim.grow_in)
        logoImageView.startAnimation(growInAnimation)


        try {
            val customTypeface = ResourcesCompat.getFont(this, R.font.islandmoments_regular)
            appNameTextView.typeface = customTypeface
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom font.", e)
        }


        lifecycleScope.launch {
            delay(7000) // Wait for 3 seconds total
            if (FirebaseManager.auth.currentUser != null) {
                navigateTo(MainActivity::class.java)
            } else {
                navigateTo(WelcomeActivity::class.java)
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}