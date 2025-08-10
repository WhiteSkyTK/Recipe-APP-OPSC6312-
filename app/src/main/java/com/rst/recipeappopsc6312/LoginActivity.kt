package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find all the views from our layout
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<TextInputEditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgotPassword)
        // You would also find the back arrow and Google sign-in button here

        // Set a click listener for the main login button
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- Placeholder for your login logic ---
            // Here, you would call your backend (e.g., Supabase or Firebase) to authenticate the user.
            // For now, we'll just show a success message.
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

            // Navigate to the main app screen after successful login
            // val intent = Intent(this, MainActivity::class.java)
            // startActivity(intent)
            // finish() // Close the login activity
        }

        // Set a click listener for the "Forgot Password" text
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}