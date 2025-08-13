package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var registrationData: RegistrationData
    private lateinit var progressBar: ProgressBar
    private val TAG = "CreateAccountActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

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
        progressBar = findViewById(R.id.Loadingbar)

        // --- Click Listeners ---
        backButton.setOnClickListener { finish() }

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (validateInputs(username, email, password, confirmPassword, usernameLayout, emailLayout, passwordLayout, confirmPasswordLayout)) {
                // All local validation passed, now call Firebase
                handleSignUp(username, email, password)
            }
        }
    }

    private fun handleSignUp(username: String, email: String, password: String) {
        Log.d(TAG, "handleSignUp: Attempting to create user with email: $email")
        progressBar.visibility = View.VISIBLE // Show loading animation

        FirebaseManager.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Auth user created successfully.")
                    val firebaseUser = FirebaseManager.auth.currentUser
                    if (firebaseUser != null) {
                        // User created in Auth, now save their full profile to Firestore
                        saveUserProfile(firebaseUser.uid, username, email)
                    } else {
                        // This case is unlikely but good to handle
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Sign up failed: Could not get user.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressBar.visibility = View.GONE // Hide loading on failure
                    Log.e(TAG, "Firebase Auth user creation failed.", task.exception)
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserProfile(userId: String, username: String, email: String) {
        Log.d(TAG, "saveUserProfile: Attempting to save profile to Firestore for user ID: $userId")

        // Create a map of all the user's data collected during registration
        val userProfile = hashMapOf(
            "id" to userId,
            "username" to username,
            "email" to email,
            "full_name" to registrationData.fullName,
            "phone_number" to registrationData.phoneNumber,
            "country" to registrationData.country,
            "gender" to registrationData.gender,
            "date_of_birth" to registrationData.dateOfBirth,
            "selected_cuisines" to registrationData.selectedCuisines,
            "selected_diets" to registrationData.selectedDiets
        )

        // Save the profile to a 'users' collection in Firestore, using the auth ID as the document ID
        FirebaseManager.firestore.collection("users").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE // Hide loading on success
                Log.d(TAG, "User profile saved successfully to Firestore.")
                Toast.makeText(this, "Sign Up Successful! 🎉", Toast.LENGTH_LONG).show()
                navigateToMainApp()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE // Hide loading on failure
                Log.e(TAG, "Failed to save user profile to Firestore.", e)
                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
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