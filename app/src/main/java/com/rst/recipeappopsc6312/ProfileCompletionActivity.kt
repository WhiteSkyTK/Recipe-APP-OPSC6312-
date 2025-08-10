package com.rst.recipeappopsc6312

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar

class ProfileCompletionActivity : AppCompatActivity() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var genderAutoComplete: AutoCompleteTextView
    private lateinit var dobEditText: TextInputEditText
    private lateinit var dobLayout: TextInputLayout

    private lateinit var registrationData: RegistrationData
    private var profileImageUri: Uri? = null

    // ActivityResultLauncher for picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            profileImageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_completion)

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
            // Launch the image picker
            pickImageLauncher.launch("image/*")
        }

        dobLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }
        dobEditText.setOnClickListener {
            showDatePickerDialog()
        }

        continueButton.setOnClickListener {
            if (validateInput()) {
                // Save data to our object
                registrationData.fullName = fullNameEditText.text.toString()
                registrationData.phoneNumber = phoneEditText.text.toString()
                registrationData.gender = genderAutoComplete.text.toString()
                registrationData.dateOfBirth = dobEditText.text.toString()
                // The profileImageUri can be used later for uploading

                navigateToNextScreen()
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            dobEditText.setText(selectedDate)
        }, year, month, day).show()
    }

    private fun validateInput(): Boolean {
        if (fullNameEditText.text.isNullOrBlank()) {
            fullNameEditText.error = "Full name cannot be empty"
            return false
        }
        if (phoneEditText.text.isNullOrBlank()) {
            phoneEditText.error = "Phone number cannot be empty"
            return false
        }
        if (genderAutoComplete.text.isNullOrBlank()) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
            return false
        }
        if (dobEditText.text.isNullOrBlank()) {
            Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show()
            return false
        }
        // Note: We don't require a profile picture, it's optional.
        return true
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, CreateAccountActivity::class.java)
        intent.putExtra("REGISTRATION_DATA", registrationData)
        // You might also need to pass the image URI if you handle uploads on the final screen
        profileImageUri?.let {
            intent.putExtra("PROFILE_IMAGE_URI", it.toString())
        }
        startActivity(intent)
    }
}