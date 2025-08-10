package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var registrationData: RegistrationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        // Correctly handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_account_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        // Find views
        val usernameLayout = findViewById<TextInputLayout>(R.id.textInputLayoutUsername)
        val usernameEditText = findViewById<TextInputEditText>(R.id.editTextUsername)
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPassword)
        val passwordEditText = findViewById<TextInputEditText>(R.id.editTextPassword)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.editTextConfirmPassword)
        val signUpButton = findViewById<Button>(R.id.buttonSignUp)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)

        // --- Click Listeners ---
        backButton.setOnClickListener { finish() }

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (validateInputs(username, email, password, confirmPassword, usernameLayout, emailLayout, passwordLayout, confirmPasswordLayout)) {

                // Show loading animation here
                Toast.makeText(this, "Creating Account...", Toast.LENGTH_SHORT).show()

                // Update the final details in our data object
                registrationData.username = username
                registrationData.email = email

                // --- Placeholder for your final Supabase sign-up call ---
                // Here you would call Supabase to create the user with email/password.
                // Then, in the success callback, you would insert the complete
                // registrationData object into your 'profiles' table.
                // supabase.auth.signUp(email, password)
                // supabase.from("profiles").insert(registrationData)

                // On success:
                // hideLoadingAnimation()
                Toast.makeText(this, "Sign Up Successful! 🎉", Toast.LENGTH_LONG).show()
                navigateToMainApp()

                // On failure:
                // hideLoadingAnimation()
                // Toast.makeText(this, "Error: " + error.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(
        username: String, email: String, pass1: String, pass2: String,
        userLayout: TextInputLayout, emailLayout: TextInputLayout, pass1Layout: TextInputLayout, pass2Layout: TextInputLayout
    ): Boolean {
        // Clear all previous errors
        userLayout.error = null
        emailLayout.error = null
        pass1Layout.error = null
        pass2Layout.error = null

        if (username.isEmpty()) {
            userLayout.error = "Username cannot be empty"
            return false
        }
        if (email.isEmpty()) {
            emailLayout.error = "Email cannot be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email address"
            return false
        }
        if (pass1.isEmpty()) {
            pass1Layout.error = "Password cannot be empty"
            return false
        }
        if (pass1.length < 6) {
            pass1Layout.error = "Password must be at least 6 characters"
            return false
        }
        if (pass2.isEmpty()) {
            pass2Layout.error = "Please confirm your password"
            return false
        }
        if (pass1 != pass2) {
            pass2Layout.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}