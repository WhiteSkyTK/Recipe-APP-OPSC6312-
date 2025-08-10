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

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Find all the views from the layout
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val sendOtpButton = findViewById<Button>(R.id.buttonSendOtp)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)

        // --- Click Listeners ---

        backButton.setOnClickListener {
            finish() // Go back to the previous screen (Login)
        }

        sendOtpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            // 1. Validate the email input
            if (validateEmail(email, emailLayout)) {
                // 2. --- Placeholder for your Supabase OTP request ---
                // Here, you would call Supabase to send a password recovery OTP.
                // supabase.auth.resetPasswordForEmail(email)
                // In the success callback, you navigate to the OTP entry screen.
                // In the failure callback, you show an error message.

                Toast.makeText(this, "Sending OTP to $email...", Toast.LENGTH_SHORT).show()

                // 3. Navigate to the OTP screen
                val intent = Intent(this, OtpActivity::class.java)
                // You can pass the email to the next screen if needed
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)
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