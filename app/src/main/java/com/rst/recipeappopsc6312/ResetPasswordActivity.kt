package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Find views
        val newPasswordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutNewPassword)
        val newPasswordEditText = findViewById<TextInputEditText>(R.id.editTextNewPassword)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.editTextConfirmPassword)
        val resetButton = findViewById<Button>(R.id.buttonResetPassword)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)

        // --- Click Listeners ---

        backButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validate the input
            if (validatePasswords(newPassword, confirmPassword, newPasswordLayout, confirmPasswordLayout)) {
                // --- Placeholder for your Supabase password update logic ---
                // Here you would call Supabase to update the user's password.
                // supabase.auth.updateUser({ password: newPassword })
                // On success, show a success message and navigate to the login screen.

                Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_LONG).show()

                // Navigate back to the Login screen
                val intent = Intent(this, LoginActivity::class.java)
                // Clear the entire task stack so the user starts fresh at the login page
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun validatePasswords(
        pass1: String,
        pass2: String,
        pass1Layout: TextInputLayout,
        pass2Layout: TextInputLayout
    ): Boolean {
        // Clear previous errors
        pass1Layout.error = null
        pass2Layout.error = null

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
}