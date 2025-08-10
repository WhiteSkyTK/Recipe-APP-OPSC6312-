package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find all the views from our layout
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPassword)
        val passwordEditText = findViewById<TextInputEditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgotPassword)
        val googleSignInButton = findViewById<ImageButton>(R.id.buttonGoogleSignIn)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)

        // --- Click Listeners ---

        backButton.setOnClickListener {
            // Finishes the current activity, taking the user back to the previous one
            finish()
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Perform validation before attempting to log in
            if (validateInput(email, password, emailLayout, passwordLayout)) {
                // --- Placeholder for your Supabase login logic ---
                // Here, you would call Supabase to sign in with email and password.
                // supabase.auth.signInWithPassword(email, password)
                // In the success callback, you navigate to the main activity.
                // In the failure callback, you show an error message.

                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
                navigateToMainApp()
            }
        }

        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        googleSignInButton.setOnClickListener {
            // This logic will be identical to the WelcomeActivity's Google Sign-In
            handleGoogleSignIn()
        }
    }

    private fun validateInput(email: String, password: String, emailLayout: TextInputLayout, passwordLayout: TextInputLayout): Boolean {
        // Clear previous errors
        emailLayout.error = null
        passwordLayout.error = null

        if (email.isEmpty()) {
            emailLayout.error = "Email cannot be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email address"
            return false
        }
        if (password.isEmpty()) {
            passwordLayout.error = "Password cannot be empty"
            return false
        }
        // You can add more password validation here (e.g., minimum length)
        return true
    }

    private fun handleGoogleSignIn() {
        // --- Placeholder for your Supabase Google Sign-In Logic ---
        // This code would be the same as in your WelcomeActivity.
        // It initiates the Google One-Tap flow and, on success,
        // either logs the user in or creates a default profile if they're new.
        Toast.makeText(this, "Initiating Google Sign-In...", Toast.LENGTH_SHORT).show()
        navigateToMainApp()
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear the activity stack so the user can't go back to the auth flow
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}