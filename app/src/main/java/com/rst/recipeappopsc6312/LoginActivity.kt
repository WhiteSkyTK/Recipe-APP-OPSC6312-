package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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

    private lateinit var loadingOverlayContainer: FrameLayout
    private lateinit var progressBar: ProgressBar // Already the one inside the overlay
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var googleSignInButton: ImageButton
    private lateinit var backButton: ImageView


    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        enableEdgeToEdge()

        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        emailEditText = findViewById(R.id.editTextEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPassword)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        forgotPasswordTextView = findViewById(R.id.textViewForgotPassword)
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn) // Assuming you have this
        backButton = findViewById(R.id.imageViewBack)

        loadingOverlayContainer = findViewById(R.id.loadingOverlayContainer) // Ensure this ID exists in your XML
        progressBar = loadingOverlayContainer.findViewById(R.id.loadingIndicator) // Ensure this ID exists INSIDE the FrameLayout in XML

        // --- Click Listeners ---

        showLoading(false)

        backButton.setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener {
            val userInput = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(userInput, password, emailLayout, passwordLayout)) {
                handleLogin(userInput, password)
            }
        }

        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        googleSignInButton.setOnClickListener {
            handleGoogleSignIn()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingOverlayContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Disable/Enable interactive elements
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        loginButton.isEnabled = !isLoading
        forgotPasswordTextView.isEnabled = !isLoading // Or make it unclickable
        googleSignInButton.isEnabled = !isLoading
        backButton.isEnabled = !isLoading
    }

    private fun handleLogin(email: String, password: String) {
        Log.d(TAG, "handleLogin: Attempting to sign in user: $email")
        showLoading(true)
        FirebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
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
        var isValid = true
        if (userInput.isEmpty()) {
            userLayout.error = "Email or Username cannot be empty"
            isValid = false
        }
        // Basic email validation if it looks like an email, otherwise assume username
        else if (userInput.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
            userLayout.error = "Invalid email format"
            isValid = false
        }

        if (pass.isEmpty()) {
            passLayout.error = "Password cannot be empty"
            isValid = false
        }
        return isValid
    }

    private fun handleGoogleSignIn() {
        // --- Placeholder for your  Google Sign-In Logic ---
        // This code would be the same as in your WelcomeActivity.
        // It initiates the Google One-Tap flow and, on success,
        // either logs the user in or creates a default profile if they're new.
        Toast.makeText(this, "Coming Soon...", Toast.LENGTH_SHORT).show()
        //navigateToMainApp()
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear the activity stack so the user can't go back to the auth flow
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}