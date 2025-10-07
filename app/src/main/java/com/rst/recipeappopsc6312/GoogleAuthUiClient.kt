package com.rst.recipeappopsc6312

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context
) {

    // Configure Google Sign-In to request the user's ID, email address, and basic profile.
    // The ID token is required for Firebase authentication.
    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)) // This comes from your google-services.json
        .requestEmail()
        .requestProfile()
        .build()

    private val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    // This function returns the Intent that will launch the Google Sign-In UI
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // This function takes the result from the Sign-In UI and processes it
    suspend fun handleSignInResult(intent: Intent): GoogleSignInResult {
        return try {
            // Get the Google account from the result intent
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.await()
            val idToken = account?.idToken

            // If we successfully got an ID token, create a Firebase credential
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                GoogleSignInResult(credential, null)
            } else {
                GoogleSignInResult(null, "Google ID token was null.")
            }
        } catch (e: Exception) {
            // Handle any errors during the process
            GoogleSignInResult(null, e.message)
        }
    }

    // A helper function to sign the user out if needed
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            FirebaseManager.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// A simple data class to hold the result of the sign-in attempt
data class GoogleSignInResult(
    val credential: com.google.firebase.auth.AuthCredential?,
    val errorMessage: String?
)

