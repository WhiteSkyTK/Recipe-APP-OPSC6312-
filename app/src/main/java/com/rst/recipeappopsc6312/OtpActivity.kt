package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OtpActivity : AppCompatActivity() {

    private lateinit var otpFields: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        val resetButton = findViewById<Button>(R.id.buttonResetPassword)

        otpFields = listOf(
            findViewById(R.id.otp_edit_text1),
            findViewById(R.id.otp_edit_text2),
            findViewById(R.id.otp_edit_text3),
            findViewById(R.id.otp_edit_text4),
            findViewById(R.id.otp_edit_text5),
            findViewById(R.id.otp_edit_text6)
        )

        setupOtpInput()

        backButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            val otp = getOtpFromFields()

            if (otp.length == 6) {
                // --- Placeholder for your Supabase OTP verification ---
                // Here you would call Supabase to verify the OTP.
                // supabase.auth.verifyOtp(...)
                // On success, navigate to the ResetPasswordActivity.
                // On failure, show an error.

                Toast.makeText(this, "Verifying OTP...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ResetPasswordActivity::class.java)
                startActivity(intent)

            } else {
                Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOtpInput() {
        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // If a digit is entered, move to the next field
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun getOtpFromFields(): String {
        return otpFields.joinToString("") { it.text.toString() }
    }
}