package com.rst.recipeappopsc6312

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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
    private lateinit var loadingOverlayContainer: FrameLayout // For the overlay
    private lateinit var progressBar: ProgressBar          // For the ProgressBar INSIDE the overlay
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView
    private lateinit var changePhotoButton: TextView
    private lateinit var changePasswordButton: TextView

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
        if (isGranted) openCamera() else Toast.makeText(this, getString(R.string.edit_profile_permission_denied), Toast.LENGTH_SHORT).show()
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
        changePhotoButton = findViewById(R.id.textViewChangePhoto)
        changePasswordButton = findViewById(R.id.textViewChangePassword)
        saveButton = findViewById(R.id.buttonSaveChanges)
        backButton = findViewById(R.id.imageViewBack)
        loadingOverlayContainer = findViewById(R.id.loadingOverlayContainer) // Ensure this ID exists in your XML
        progressBar = loadingOverlayContainer.findViewById(R.id.loadingIndicator) // Ensure this ID exists INSIDE the FrameLayout in XML

        showLoading(false)
        loadUserProfile()

        // --- Click Listeners ---
        backButton.setOnClickListener { finish() }
        changePhotoButton.setOnClickListener { showImagePickerDialog() }
        profileImageView.setOnClickListener { showImagePickerDialog() }

        changePasswordButton.setOnClickListener {
            val user = FirebaseManager.auth.currentUser
            if (user?.email != null) {
                showLoading(true) // Show loading before sending email
                FirebaseManager.auth.sendPasswordResetEmail(user.email!!)
                    .addOnCompleteListener { task -> // Use addOnCompleteListener for robustness
                        showLoading(false) // Hide loading after attempt
                        if (task.isSuccessful) {
                            Toast.makeText(this, getString(R.string.edit_profile_reset_link_sent), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, getString(R.string.edit_profile_reset_link_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingOverlayContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Disable/Enable interactive elements
        profileImageView.isEnabled = !isLoading
        fullNameEditText.isEnabled = !isLoading
        usernameEditText.isEnabled = !isLoading
        phoneEditText.isEnabled = !isLoading
        changePhotoButton.isEnabled = !isLoading
        changePasswordButton.isEnabled = !isLoading
        saveButton.isEnabled = !isLoading
        backButton.isEnabled = !isLoading
    }

    private fun loadUserProfile() {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        showLoading(true)
        FirebaseManager.firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document != null && document.exists()) {
                    fullNameEditText.setText(document.getString("full_name"))
                    usernameEditText.setText(document.getString("username"))
                    phoneEditText.setText(document.getString("phone_number"))
                    val imageUrl = document.getString("profileImageUrl")
                    if (imageUrl != null) {
                        Glide.with(this).load(imageUrl).into(profileImageView)
                    }
                }
            }.addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, getString(R.string.edit_profile_upload_failed, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        showLoading(true)

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
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.edit_profile_upload_failed, e.message), Toast.LENGTH_SHORT).show()
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
                showLoading(false)
                Toast.makeText(this, getString(R.string.edit_profile_update_success), Toast.LENGTH_SHORT).show()
                finish() // Use finish() to go back
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, getString(R.string.edit_profile_update_failed, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showImagePickerDialog() {
        val options = resources.getStringArray(R.array.dialog_cover_photo_options)
        AlertDialog.Builder(this) // Use 'this' for context
            .setTitle(getString(R.string.dialog_set_photo_title))
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
        fullNameEditText.error = null // Clear previous errors
        usernameEditText.error = null

        if (fullNameEditText.text.isNullOrBlank()) {
            fullNameEditText.error = getString(R.string.validation_full_name_empty)
            return false
        }
        if (usernameEditText.text.isNullOrBlank()) {
            usernameEditText.error = getString(R.string.validation_username_empty)
            return false
        }
        // Add phone number validation if needed
        return true
    }
}