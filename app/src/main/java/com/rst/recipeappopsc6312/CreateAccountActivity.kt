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
import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import java.io.File

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var registrationData: RegistrationData
    private lateinit var loadingOverlayContainer: FrameLayout // For the overlay
    private lateinit var progressBar: ProgressBar          // For the actual progress bar
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var signUpButton: Button
    private lateinit var backButton: ImageView

    private var localProfileImagePath: String? = null
    private val TAG = "CreateAccountActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        enableEdgeToEdge()
        val createLayout = findViewById<View>(R.id.create_account_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(createLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()
        localProfileImagePath = intent.getStringExtra("PROFILE_IMAGE_PATH")

        // Find views
        val usernameLayout = findViewById<TextInputLayout>(R.id.textInputLayoutUsername)
        usernameEditText = findViewById(R.id.editTextUsername)
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        emailEditText = findViewById(R.id.editTextEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPassword)
        passwordEditText = findViewById(R.id.editTextPassword)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)
        signUpButton = findViewById(R.id.buttonSignUp)
        backButton = findViewById(R.id.imageViewBack)
        loadingOverlayContainer = findViewById(R.id.loadingOverlayContainer) // Ensure this ID exists in your XML
        progressBar = loadingOverlayContainer.findViewById(R.id.loadingIndicator) // Ensure this ID exists INSIDE the FrameLayout in XML

        showLoading(false)

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

    private fun showLoading(isLoading: Boolean) {
        loadingOverlayContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Disable/Enable interactive elements
        usernameEditText.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        confirmPasswordEditText.isEnabled = !isLoading
        signUpButton.isEnabled = !isLoading
        backButton.isEnabled = !isLoading
    }

    private fun handleSignUp(username: String, email: String, password: String) {
        Log.d(TAG, "handleSignUp: Attempting to create user with email: $email")
        showLoading(true)

        FirebaseManager.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Auth user created successfully.")
                    val userId = FirebaseManager.auth.currentUser?.uid
                    if (userId == null) {
                        // This is a failsafe, should not happen
                        showLoading(false)
                        Toast.makeText(this, getString(R.string.create_account_toast_signup_failed_no_id), Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // Now, check if we need to upload a profile picture
                    if (localProfileImagePath != null) {
                        val fileUri = Uri.fromFile(File(localProfileImagePath!!))
                        val storageRef = FirebaseManager.storage.reference.child("profile_pictures/$userId/profile.jpg")

                        Log.d(TAG, "Uploading profile picture...")
                        storageRef.putFile(fileUri)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    Log.d(TAG, "Profile picture uploaded. Saving profile with URL.")
                                    saveUserProfile(userId, username, email, downloadUrl.toString())
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Profile picture upload failed. Saving profile without URL.", e)
                                saveUserProfile(userId, username, email, null) // Still save profile on failure
                            }
                    } else {
                        Log.d(TAG, "No profile picture. Saving profile without URL.")
                        saveUserProfile(userId, username, email, null)
                    }
                } else {
                    showLoading(false)
                    Log.e(TAG, "Firebase Auth user creation failed.", task.exception)
                    Toast.makeText(this, getString(R.string.create_account_toast_signup_failed_exception, task.exception?.message), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserProfile(userId: String, username: String, email: String, imageUrl: String?) {
        Log.d(TAG, "saveUserProfile: Attempting to save profile to Firestore.")
        val userProfile = hashMapOf(
            "id" to userId,
            "username" to username,
            "email" to email,
            "profileImageUrl" to imageUrl,
            "full_name" to registrationData.fullName,
            "phone_number" to registrationData.phoneNumber,
            "country" to registrationData.country,
            "gender" to registrationData.gender,
            "date_of_birth" to registrationData.dateOfBirth,
            "selected_cuisines" to registrationData.selectedCuisines,
            "selected_diets" to registrationData.selectedDiets
        )

        FirebaseManager.firestore.collection("users").document(userId).set(userProfile)
            .addOnSuccessListener {
                showLoading(false)
                Log.d(TAG, "User profile saved successfully to Firestore.")
                Toast.makeText(this, getString(R.string.create_account_toast_signup_success), Toast.LENGTH_LONG).show()
                navigateToMainApp()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Failed to save user profile to Firestore.", e)
                Toast.makeText(this, getString(R.string.create_account_toast_save_profile_failed, e.message), Toast.LENGTH_SHORT).show()
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

        var isValid = true

        if (username.isEmpty()) {
            userLayout.error = getString(R.string.validation_username_empty)
            return false
        }
        if (email.isEmpty()) {
            emailLayout.error = getString(R.string.validation_email_empty)
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = getString(R.string.validation_email_invalid)
            return false
        }
        if (pass1.isEmpty()) {
            pass1Layout.error = getString(R.string.validation_password_empty)
            isValid = false
        } else {
            // You can adjust these criteria as needed
            val minLength = 8 // Let's make it 8 for stronger passwords
            val hasUppercase = pass1.any { it.isUpperCase() }
            val hasLowercase = pass1.any { it.isLowerCase() }
            val hasDigit = pass1.any { it.isDigit() }
            val hasSpecialChar = pass1.any { !it.isLetterOrDigit() } // Or define your own set of special chars

            if (pass1.length < minLength) {
                pass1Layout.error = getString(R.string.validation_password_min_length, minLength)
                isValid = false
            } else if (!hasUppercase) {
                pass1Layout.error = getString(R.string.validation_password_no_uppercase)
                isValid = false
            } else if (!hasLowercase) {
                pass1Layout.error = getString(R.string.validation_password_no_lowercase) // Good to have, though often implied
                isValid = false
            } else if (!hasDigit) {
                pass1Layout.error = getString(R.string.validation_password_no_digit)
                isValid = false
            } else if (!hasSpecialChar) {
                // Be specific about allowed special characters if you have a strict list
                pass1Layout.error = getString(R.string.validation_password_no_special)
                isValid = false
            }
            // Note: Firebase Auth itself has a minimum password length of 6.
            // Your stricter rules here are for your app's UI validation.
        }
        if (pass2.isEmpty()) {
            pass2Layout.error = getString(R.string.validation_password_confirm_empty)
            return false
        }
        if (pass1 != pass2) {
            pass2Layout.error = getString(R.string.validation_passwords_no_match)
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