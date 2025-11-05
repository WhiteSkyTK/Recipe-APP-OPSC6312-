package com.rst.recipeappopsc6312

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.util.Calendar
import java.io.FileOutputStream
import java.util.Locale

class ProfileCompletionActivity : AppCompatActivity() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var ccp: CountryCodePicker
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
            Toast.makeText(this, getString(R.string.profile_completion_permission_camera_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_completion)

        enableEdgeToEdge()
        val profileSelectionLayout = findViewById<View>(R.id.profileCompletionLayout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(profileSelectionLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }
        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        // Find views
        profileImageView = findViewById(R.id.profile_image)
        fullNameEditText = findViewById(R.id.editTextFullName)
        phoneEditText = findViewById(R.id.editTextPhone)
        ccp = findViewById(R.id.ccp)
        genderAutoComplete = findViewById(R.id.autoCompleteGender)
        dobEditText = findViewById(R.id.editTextDob)
        dobLayout = findViewById(R.id.textInputLayoutDob) // This is for the icon click
        val continueButton = findViewById<Button>(R.id.buttonContinue)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)

        ccp.registerCarrierNumberEditText(phoneEditText)

        val countryName = registrationData.country
        if (!countryName.isNullOrBlank()) {
            // The library will find the correct code (e.g., "+27" for "South Africa")
            ccp.setCountryForNameCode(getCountryCodeFromName(countryName))
        }

        setupGenderDropdown()
        setupClickListeners(continueButton, backButton)
    }

    private fun setupGenderDropdown() {
        val genders = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        genderAutoComplete.setAdapter(adapter)
    }

    private fun getCountryCodeFromName(countryName: String): String {
        return Locale.getISOCountries().find { Locale("", it).displayCountry == countryName } ?: "ZA"
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
                registrationData.phoneNumber = ccp.fullNumberWithPlus
                registrationData.gender = genderAutoComplete.text.toString().takeIf { it.isNotBlank() }
                registrationData.dateOfBirth = dobEditText.text.toString().takeIf { it.isNotBlank() }

                navigateToNextScreen()
            }
        }
    }


    private fun showImagePickerDialog() {
        val options = resources.getStringArray(R.array.dialog_cover_photo_options).map { it.toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_completion_set_photo_title))
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

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                dobEditText.setText("$day/${month + 1}/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // 1. Set the maximum date to today (user cannot be born in the future)
        // To be safe, let's set a minimum age of 13
        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.add(Calendar.YEAR, -13)
        datePickerDialog.datePicker.maxDate = maxDateCalendar.timeInMillis

        // 2. Set the minimum date (e.g., user cannot be more than 100 years old)
        val minDateCalendar = Calendar.getInstance()
        minDateCalendar.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minDateCalendar.timeInMillis

        datePickerDialog.show()
    }

    private fun validateInput(): Boolean {
        // Only Full Name and Phone Number are now required
        if (fullNameEditText.text.isNullOrBlank()) {
            fullNameEditText.error = getString(R.string.validation_fullname_empty)
            return false
        }
        if (!ccp.isValidFullNumber) {
            phoneEditText.error = getString(R.string.validation_phone_invalid) // Set error on the actual input field
            return false
        }
        return true
    }

    private fun navigateToNextScreen() {
        setDefaultUnitSystem(registrationData.country)
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

    private fun setDefaultUnitSystem(countryName: String?) {
        // List of countries that primarily use the Imperial system
        val imperialCountries = listOf("United States", "Liberia", "Myanmar")

        val systemToSet = if (imperialCountries.contains(countryName)) {
            UnitConverter.IMPERIAL
        } else {
            UnitConverter.METRIC
        }

        // Save this default to SharedPreferences
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putString("UnitSystem", systemToSet).apply()
    }
}