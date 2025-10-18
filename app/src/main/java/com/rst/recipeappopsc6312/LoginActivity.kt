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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class LoginActivity : AppCompatActivity() {

    private lateinit var loadingOverlayContainer: FrameLayout
    private lateinit var progressBar: ProgressBar // Already the one inside the overlay
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var googleSignInButton: ImageButton
    private lateinit var backButton: ImageView

    private lateinit var googleAuthUiClient: GoogleAuthUiClient

    // We need a ViewModel to access the repository for creating the user profile
    private val viewModel: ShoppingViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(),
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
    }

    private val TAG = "LoginActivity"

    // This is the modern way to handle the result of the Google Sign-In intent
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                lifecycleScope.launch {
                    val signInResult = googleAuthUiClient.handleSignInResult(intent)
                    if (signInResult.credential != null) {
                        Firebase.auth.signInWithCredential(signInResult.credential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                                if (isNewUser) {
                                    // If it's a new user, create their default profile
                                    lifecycleScope.launch {
                                        task.result?.user?.let {
                                            viewModel.repository.createDefaultUserProfile(it)
                                        }
                                        navigateToMainApp()
                                    }
                                } else {
                                    // If they are a returning user, just log them in
                                    navigateToMainApp()
                                }
                            } else {
                                Toast.makeText(this@LoginActivity, getString(R.string.login_firebase_auth_failed, task.exception?.message), Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, getString(R.string.login_google_sign_in_failed, signInResult.errorMessage), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        enableEdgeToEdge()

        googleAuthUiClient = GoogleAuthUiClient(applicationContext)

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
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
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
                    navigateToMainApp()
                } else {
                    Log.w(TAG, "Sign in failed for user: $email", task.exception)
                    Toast.makeText(this, getString(R.string.login_auth_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateInput(userInput: String, pass: String, userLayout: TextInputLayout, passLayout: TextInputLayout): Boolean {
        userLayout.error = null
        passLayout.error = null
        var isValid = true
        if (userInput.isEmpty()) {
            userLayout.error = getString(R.string.validation_user_input_empty)
            isValid = false
        }
        // Basic email validation if it looks like an email, otherwise assume username
        else if (userInput.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
            userLayout.error = getString(R.string.validation_user_input_invalid_email)
            isValid = false
        }

        if (pass.isEmpty()) {
            passLayout.error = getString(R.string.validation_password_empty)
            isValid = false
        }
        return isValid
    }

    private fun handleGoogleSignIn() {
        val signInIntent = googleAuthUiClient.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear the activity stack so the user can't go back to the auth flow
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}