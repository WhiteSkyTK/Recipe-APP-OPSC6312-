package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
class LoginActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        enableEdgeToEdge()
        //TODO: Add edge-to-edge support
        // Find all the views from our layout
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPassword)
        val passwordEditText = findViewById<TextInputEditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgotPassword)
        val googleSignInButton = findViewById<ImageButton>(R.id.buttonGoogleSignIn)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        progressBar = findViewById(R.id.progressBar)

        // --- Click Listeners ---

        backButton.setOnClickListener {
            // FIXED: Explicitly navigate back to WelcomeActivity and clear the history
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val userInput = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(userInput, password, emailLayout, passwordLayout)) {
                progressBar.visibility = View.VISIBLE // Show loading animation
                handleLogin(userInput, password)
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

    private fun handleLogin(email: String, password: String) {
        Log.d(TAG, "handleLogin: Attempting to sign in user: $email")
        progressBar.visibility = View.VISIBLE
        FirebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Log.d(TAG, "Sign in successful for user: $email")
                    // Optional but recommended: Pre-fetch user profile to warm up the cache
                    // This ensures MainActivity and ProfileFragment load instantly.
                    // fetchUserProfile()
                    navigateToMainApp()
                } else {
                    Log.w(TAG, "Sign in failed for user: $email", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateInput(userInput: String, pass: String, userLayout: TextInputLayout, passLayout: TextInputLayout): Boolean {
        userLayout.error = null
        passLayout.error = null
        if (userInput.isEmpty()) {
            userLayout.error = "Email or Username cannot be empty"
            return false
        }
        if (pass.isEmpty()) {
            passLayout.error = "Password cannot be empty"
            return false
        }
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