package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var loadingOverlayContainer: FrameLayout
    private lateinit var progressBar: ProgressBar // Already the one inside the overlay
    private lateinit var emailEditText: TextInputEditText
    private lateinit var sendOtpButton: Button
    private lateinit var backButton: ImageView

    private val TAG = "ForgotPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        enableEdgeToEdge()

        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        emailEditText = findViewById(R.id.editTextEmail)
        sendOtpButton = findViewById(R.id.buttonSendLink)
        backButton = findViewById(R.id.imageViewBack)

        loadingOverlayContainer = findViewById(R.id.loadingOverlayContainer) // Ensure this ID exists in your XML
        progressBar = loadingOverlayContainer.findViewById(R.id.loadingIndicator) // Ensure this ID exists INSIDE the FrameLayout in XML

        showLoading(false)

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

    private fun showLoading(isLoading: Boolean) {
        loadingOverlayContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Disable/Enable interactive elements
        emailEditText.isEnabled = !isLoading
        sendOtpButton.isEnabled = !isLoading
        backButton.isEnabled = !isLoading
    }


    private fun sendPasswordResetEmail(email: String) {
        Log.d(TAG, "sendPasswordResetEmail: Attempting to send reset email to: $email")
        showLoading(true)

        FirebaseManager.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "Password reset email sent successfully.")
                    Toast.makeText(this, getString(R.string.forgot_password_link_sent), Toast.LENGTH_LONG).show()
                    // Optionally, you can navigate back to the login screen after a delay
                    finish()
                } else {
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)
                    Toast.makeText(this, getString(R.string.forgot_password_link_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateEmail(email: String, emailLayout: TextInputLayout): Boolean {
        // Clear previous error
        emailLayout.error = null

        if (email.isEmpty()) {
            emailLayout.error = getString(R.string.validation_email_empty)
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = getString(R.string.validation_email_invalid)
            return false
        }
        return true
    }
}