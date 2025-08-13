package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import android.util.Log
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Log.d(TAG, "onCreate: Splash screen started.")

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        Handler(Looper.getMainLooper()).postDelayed({
            if (FirebaseManager.auth.currentUser != null) {
                Log.d(TAG, "User is logged in. Navigating to MainActivity.")
                navigateTo(MainActivity::class.java)
            } else {
                Log.d(TAG, "User is not logged in. Navigating to WelcomeActivity.")
                navigateTo(WelcomeActivity::class.java)
            }
        }, 2000)
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}