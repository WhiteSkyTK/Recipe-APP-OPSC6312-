package com.rst.recipeappopsc6312

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.util.Calendar
import java.io.FileOutputStream


class ProfileCompletionActivity : AppCompatActivity() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var genderAutoComplete: AutoCompleteTextView
    private lateinit var dobEditText: TextInputEditText
    private lateinit var dobLayout: TextInputLayout

    private lateinit var registrationData: RegistrationData
    private var profileImageUri: Uri? = null
    private var tempImageUri: Uri? = null

    // ActivityResultLauncher for picking an image from the gallery
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            profileImageUri = tempImageUri
            profileImageView.setImageURI(profileImageUri)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Save a local copy and get its path
            val localImagePath = saveImageToInternalStorage(it)
            profileImageUri = Uri.fromFile(File(localImagePath)) // Store the local file URI
            profileImageView.setImageURI(profileImageUri)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_completion)

        enableEdgeToEdge()
        //TODO: Add edge-to-edge support
        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        // Find views
        profileImageView = findViewById(R.id.profile_image)
        fullNameEditText = findViewById(R.id.editTextFullName)
        phoneEditText = findViewById(R.id.editTextPhone)
        genderAutoComplete = findViewById(R.id.autoCompleteGender)
        dobEditText = findViewById(R.id.editTextDob)
        dobLayout = findViewById(R.id.textInputLayoutDob) // This is for the icon click
        val continueButton = findViewById<Button>(R.id.buttonContinue)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)

        setupGenderDropdown()
        setupClickListeners(continueButton, backButton)
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        genderAutoComplete.setAdapter(adapter)
    }

    private fun setupClickListeners(continueButton: Button, backButton: ImageView) {
        backButton.setOnClickListener { finish() }

        profileImageView.setOnClickListener {
            showImagePickerDialog()
        }

        dobLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }
        dobEditText.setOnClickListener {
            showDatePickerDialog()
        }

        continueButton.setOnClickListener {
            if (validateInput()) {
                registrationData.fullName = fullNameEditText.text.toString().trim()
                registrationData.phoneNumber = phoneEditText.text.toString().trim()
                registrationData.gender = genderAutoComplete.text.toString().takeIf { it.isNotBlank() }
                registrationData.dateOfBirth = dobEditText.text.toString().takeIf { it.isNotBlank() }

                navigateToNextScreen()
            }
        }
    }


    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        AlertDialog.Builder(this)
            .setTitle("Set Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> {
                        profileImageUri = null
                        profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // You can show a dialog here explaining why you need the permission
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val file = File(filesDir, "temp_image.jpg")
        tempImageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)

        tempImageUri?.let { uri ->
            cameraLauncher.launch(uri)
        }
    }
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            dobEditText.setText("$day/${month + 1}/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateInput(): Boolean {
        // Only Full Name and Phone Number are now required
        if (fullNameEditText.text.isNullOrBlank()) {
            fullNameEditText.error = "Full name cannot be empty"
            return false
        }
        if (phoneEditText.text.isNullOrBlank()) {
            phoneEditText.error = "Phone number cannot be empty"
            return false
        }
        return true
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, CreateAccountActivity::class.java)
        intent.putExtra("REGISTRATION_DATA", registrationData)
        // Pass the path of the locally saved image file
        profileImageUri?.path?.let {
            intent.putExtra("PROFILE_IMAGE_PATH", it)
        }
        startActivity(intent)
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_pic.jpg") // Always use the same name to overwrite
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ProfileCompletion", "Failed to save image locally", e)
            null
        }
    }
}