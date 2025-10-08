package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {
    private lateinit var googleAuthUiClient: GoogleAuthUiClient
    private val TAG = "SSO_Welcome"

    // We add a ViewModel to access the repository for creating the user profile
    private val viewModel: ShoppingViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(),
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
    }

    // This is the modern way to handle the result of the Google Sign-In intent
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In launcher returned with result code: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                Log.d(TAG, "Intent is not null, proceeding with sign-in.")
                lifecycleScope.launch {
                    val signInResult = googleAuthUiClient.handleSignInResult(intent)
                    if (signInResult.credential != null) {
                        Log.d(TAG, "Successfully got credential from Google. Signing in with Firebase...")
                        Firebase.auth.signInWithCredential(signInResult.credential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "Firebase sign-in successful.")
                                val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                                if (isNewUser) {
                                    Log.d(TAG, "New user detected. Creating default profile...")
                                    // If it's a new user, create their default profile
                                    lifecycleScope.launch {
                                        task.result?.user?.let {
                                            viewModel.repository.createDefaultUserProfile(it)
                                            Log.d(TAG, "Default profile created for user: ${it.uid}")
                                        }
                                        navigateToMainApp()
                                    }
                                } else {
                                    Log.d(TAG, "Returning user detected. Navigating to main app.")
                                    // If they are a returning user, just log them in
                                    navigateToMainApp()
                                }
                            } else {
                                Log.e(TAG, "Firebase Auth with Google credential failed.", task.exception)
                                Toast.makeText(this@WelcomeActivity, "Firebase Auth failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Log.e(TAG, "Google Sign-In failed: ${signInResult.errorMessage}")
                        Toast.makeText(this@WelcomeActivity, "Google Sign-In failed: ${signInResult.errorMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.w(TAG, "Sign-in intent is null.")
            }
        } else {
            Log.w(TAG, "Google Sign-In was cancelled or failed with result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        googleAuthUiClient = GoogleAuthUiClient(applicationContext)

        enableEdgeToEdge()
        val welcomeLayout = findViewById<android.view.View>(R.id.welcome)

        // --- CORRECTED PADDING LOGIC ---
        // 1. Store the initial padding from your XML
        val initialPaddingLeft = welcomeLayout.paddingLeft
        val initialPaddingTop = welcomeLayout.paddingTop
        val initialPaddingRight = welcomeLayout.paddingRight
        val initialPaddingBottom = welcomeLayout.paddingBottom

        // 2. Apply window insets ON TOP of the initial padding
        ViewCompat.setOnApplyWindowInsetsListener(welcomeLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        // Find the buttons from the layout
        val googleSignInButton = findViewById<MaterialButton>(R.id.buttonGoogleSignIn)
        val getStartedButton = findViewById<MaterialButton>(R.id.buttonGetStarted)
        val loginButton = findViewById<MaterialButton>(R.id.buttonLogin)
        val emojiTextView = findViewById<TextView>(R.id.textViewEmoji)

        val waveAnimation = AnimationUtils.loadAnimation(this, R.anim.wave_animation)
        emojiTextView.startAnimation(waveAnimation)

        getStartedButton.setOnClickListener {
            startActivity(Intent(this, CountrySelectionActivity::class.java))
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        googleSignInButton.setOnClickListener {
            handleGoogleSignIn()
        }
    }

    private fun handleGoogleSignIn() {
        Log.d(TAG, "handleGoogleSignIn: Launching Google Sign-In intent...")
        val signInIntent = googleAuthUiClient.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }

    private fun navigateToMainApp() {
        Log.d(TAG, "navigateToMainApp: Navigating to MainActivity.")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}