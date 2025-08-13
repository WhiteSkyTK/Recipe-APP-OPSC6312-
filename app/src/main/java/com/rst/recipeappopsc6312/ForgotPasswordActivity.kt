package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private val TAG = "ForgotPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Find all the views from the layout
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val sendOtpButton = findViewById<Button>(R.id.buttonSendOtp)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        // You'll need to add a ProgressBar to your activity_forgot_password.xml layout
        // progressBar = findViewById(R.id.progressBar)

        backButton.setOnClickListener {
            finish() // Go back to the previous screen (Login)
        }

        sendOtpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (validateEmail(email, emailLayout)) {
                // All local validation passed, now call Firebase
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        Log.d(TAG, "sendPasswordResetEmail: Attempting to send reset email to: $email")
        // progressBar.visibility = View.VISIBLE // Show loading animation

        FirebaseManager.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                // progressBar.visibility = View.GONE // Hide loading animation
                if (task.isSuccessful) {
                    Log.d(TAG, "Password reset email sent successfully.")
                    Toast.makeText(this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show()
                    // Optionally, you can navigate back to the login screen after a delay
                    finish()
                } else {
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateEmail(email: String, emailLayout: TextInputLayout): Boolean {
        // Clear previous error
        emailLayout.error = null

        if (email.isEmpty()) {
            emailLayout.error = "Email cannot be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email address"
            return false
        }
        return true
    }
}