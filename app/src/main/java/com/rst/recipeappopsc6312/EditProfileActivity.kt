package com.rst.recipeappopsc6312

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    private val TAG = "EditProfileActivity"
    private lateinit var profileImageView: CircleImageView
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar // Add a ProgressBar to your layout

    private var profileImageUri: Uri? = null
    private var tempImageUri: Uri? = null
    private var removeImageFlag = false

    // --- ActivityResultLaunchers for Camera, Gallery, and Permissions ---
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            profileImageUri = tempImageUri
            profileImageView.setImageURI(profileImageUri)
            removeImageFlag = false
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            profileImageView.setImageURI(it)
            removeImageFlag = false
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        enableEdgeToEdge()
        val editprofilelayout = findViewById<View>(R.id.edit_profile_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(editprofilelayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        // Find views
        profileImageView = findViewById(R.id.profile_image)
        fullNameEditText = findViewById(R.id.editTextFullName)
        usernameEditText = findViewById(R.id.editTextUsername)
        phoneEditText = findViewById(R.id.editTextPhone)
        val changePhotoButton = findViewById<TextView>(R.id.textViewChangePhoto)
        val changePasswordButton = findViewById<TextView>(R.id.textViewChangePassword)
        val saveButton = findViewById<Button>(R.id.buttonSaveChanges)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        // progressBar = findViewById(R.id.progressBar) //TODO Make sure this ID exists in your XML

        loadUserProfile()

        // --- Click Listeners ---
        backButton.setOnClickListener { finish() }
        changePhotoButton.setOnClickListener { showImagePickerDialog() }
        profileImageView.setOnClickListener { showImagePickerDialog() }

        changePasswordButton.setOnClickListener {
            val user = FirebaseManager.auth.currentUser
            if (user?.email != null) {
                FirebaseManager.auth.sendPasswordResetEmail(user.email!!)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send reset link: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadUserProfile() {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        FirebaseManager.firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    fullNameEditText.setText(document.getString("full_name"))
                    usernameEditText.setText(document.getString("username"))
                    phoneEditText.setText(document.getString("phone_number"))
                    val imageUrl = document.getString("profileImageUrl")
                    if (imageUrl != null) {
                        Glide.with(this).load(imageUrl).into(profileImageView)
                    }
                }
            }
    }

    private fun saveChanges() {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        if (profileImageUri != null) {
            // Case 1: A new image was selected, upload it first
            val storageRef = FirebaseManager.storage.reference.child("profile_pictures/$userId/profile.jpg")
            storageRef.putFile(profileImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        updateUserProfile(userId, downloadUrl.toString())
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Case 2: No new image was selected, just update text fields
            // The 'removeImageFlag' will determine if we should clear the existing URL
            updateUserProfile(userId, if (removeImageFlag) "" else null)
        }
    }

    private fun updateUserProfile(userId: String, newImageUrl: String?) {
        val updates = mutableMapOf<String, Any?>()
        updates["full_name"] = fullNameEditText.text.toString().trim()
        updates["username"] = usernameEditText.text.toString().trim()
        updates["phone_number"] = phoneEditText.text.toString().trim()

        if (newImageUrl != null) {
            updates["profileImageUrl"] = newImageUrl
        }

        FirebaseManager.firestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                // progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish() // Use finish() to go back
            }
            .addOnFailureListener { e ->
                // progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        AlertDialog.Builder(this) // Use 'this' for context
            .setTitle("Set Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> {
                        profileImageUri = null
                        removeImageFlag = true
                        profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    private fun openCamera() {
        val file = File(filesDir, "temp_image.jpg")
        tempImageUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        tempImageUri?.let { cameraLauncher.launch(it) }
    }

    private fun validateInput(): Boolean {
        if (fullNameEditText.text.isNullOrBlank()) {
            fullNameEditText.error = "Full name cannot be empty"
            return false
        }
        if (usernameEditText.text.isNullOrBlank()) {
            usernameEditText.error = "Username cannot be empty"
            return false
        }
        return true
    }
}