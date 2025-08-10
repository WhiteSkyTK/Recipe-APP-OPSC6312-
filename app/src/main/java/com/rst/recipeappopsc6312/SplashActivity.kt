package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Note: I've removed enableEdgeToEdge() as it's often simpler to manage
        // themes directly and the original code would crash.
        setContentView(R.layout.activity_splash)

        // --- Placeholder for Dark/Light Mode Logic ---
        // You would check a saved preference here.
        // For now, we'll just default to the system setting.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)


        // Use a Handler to delay the screen transition
        Handler(Looper.getMainLooper()).postDelayed({
            // Check user login status
            if (isUserLoggedIn()) {
                // User is logged in, go to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // User is not logged in, go to WelcomeActivity
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
            }
            // Finish the SplashActivity so the user can't go back to it
            finish()
        }, 3000) // 3000 milliseconds = 3 seconds
    }

    private fun isUserLoggedIn(): Boolean {
        // --- Placeholder for your real authentication check ---
        // In a real app, you would check your backend (Supabase, Firebase, etc.)
        // or SharedPreferences to see if a valid user session exists.
        // For testing, you can change this to 'true' or 'false'.
        return false
    }
}