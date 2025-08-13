package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        enableEdgeToEdge()
        val welcomeLayout = findViewById<android.view.View>(R.id.welcome)

        // --- CORRECTED PADDING LOGIC ---
        // 1. Store the initial padding from your XML
        val initialPaddingLeft = welcomeLayout.paddingLeft
        val initialPaddingTop = welcomeLayout.paddingTop
        val initialPaddingRight = welcomeLayout.paddingRight
        val initialPaddingBottom = welcomeLayout.paddingBottom

        // 2. Apply window insets ON TOP of the initial padding
        ViewCompat.setOnApplyWindowInsetsListener(welcomeLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        // Find the buttons from the layout
        val googleSignInButton = findViewById<MaterialButton>(R.id.buttonGoogleSignIn)
        val getStartedButton = findViewById<MaterialButton>(R.id.buttonGetStarted)
        val loginButton = findViewById<MaterialButton>(R.id.buttonLogin)

        // --- Set Click Listeners ---

        // 1. "Get Started" button navigates to the registration flow
        getStartedButton.setOnClickListener {
            val intent = Intent(this, CountrySelectionActivity::class.java)
            startActivity(intent)
        }

        // 2. "I Already Have an Account" button navigates to the Login screen
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // 3. "Continue with Google" button handles one-tap sign-in
        googleSignInButton.setOnClickListener {
            // This is where you will trigger the Supabase Google One-Tap Sign-in flow
            handleGoogleSignIn()
        }
    }

    private fun handleGoogleSignIn() {
        // --- Placeholder for your Supabase Google Sign-In Logic ---
        Toast.makeText(this, "Initiating Google Sign-In...", Toast.LENGTH_SHORT).show()

        // 1. Call the Supabase/Google SDK to start the sign-in process.
        // 2. The SDK will show a bottom sheet with the user's Google accounts.

        // 3. In the success callback from the SDK, you will:
        //    a. Get the user's Google account details (name, email).
        //    b. Create a new user record in your Supabase 'profiles' table.
        //    c. Populate the profile with the default data you planned:
        //       - name = from Google
        //       - username = auto-generated (e.g., from email or a random string)
        //       - country = "South Africa"
        //       - cuisines = null or empty
        //       - dietary_preferences = null or empty
        //    d. Once the profile is created successfully, navigate to the main app.

        // For now, we'll simulate a successful sign-in and navigation
        val intent = Intent(this, MainActivity::class.java)
        // Clear the activity stack so the user can't go back to the welcome screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}