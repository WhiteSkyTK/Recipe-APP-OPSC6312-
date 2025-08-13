package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DietarySelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dietAdapter: DietAdapter
    private var dietList = ArrayList<Diet>()
    private lateinit var registrationData: RegistrationData
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietary_selection)

        enableEdgeToEdge()
        //TODO: Add edge-to-edge support
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        // Find views
        val continueButton = findViewById<Button>(R.id.buttonContinue)
        val skipButton = findViewById<Button>(R.id.buttonSkip)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewDiets)

        if (isEditMode) {
            progressBar.visibility = View.GONE
            continueButton.text = "Save Changes"
            skipButton.visibility = View.GONE
        }

        // Set up the data and RecyclerView
        prepareDietData()

        // The adapter now handles its own clicks via a lambda
        dietAdapter = DietAdapter { clickedDiet ->
            val updatedList = dietList.map {
                if (it.name == clickedDiet.name) it.copy(isSelected = !it.isSelected) else it
            }
            dietList = ArrayList(updatedList)
            dietAdapter.submitList(dietList)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 2) // Using 2 columns as per your design
        recyclerView.adapter = dietAdapter
        dietAdapter.submitList(dietList)

        // --- Button Logic ---

        backButton.setOnClickListener {
            finish() // Go back to the previous screen
        }

        continueButton.setOnClickListener {
            val selectedDiets = dietList.filter { it.isSelected }
            if (selectedDiets.isEmpty()) {
                Toast.makeText(this, "Please select at least one preference", Toast.LENGTH_SHORT).show()
            } else {
                val selectedDietNames = selectedDiets.map { it.name }
                if (isEditMode) {
                    updateUserDiets(selectedDietNames)
                } else {
                    registrationData.selectedDiets = selectedDietNames
                    navigateToNextScreen()
                }
            }
        }

        skipButton.setOnClickListener {
            registrationData.selectedDiets = emptyList()
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, ProfileCompletionActivity::class.java)
        intent.putExtra("REGISTRATION_DATA", registrationData)
        startActivity(intent)
    }

    private fun updateUserDiets(diets: List<String>) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        FirebaseManager.firestore.collection("users").document(userId)
            .update("selected_diets", diets)
            .addOnSuccessListener {
                Toast.makeText(this, "Preferences updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prepareDietData() {
        // Here is an expanded list of diets.
        dietList.add(Diet("Vegetarian", R.drawable.ic_vegetarian))
        dietList.add(Diet("Vegan", R.drawable.ic_vegan))
        dietList.add(Diet("Gluten-Free", R.drawable.ic_gluten_free))
        dietList.add(Diet("Keto", R.drawable.ic_keto))
        dietList.add(Diet("Paleo", R.drawable.ic_paleo))
        dietList.add(Diet("Pescetarian", R.drawable.ic_pescetarian))
        dietList.add(Diet("Low-Carb", R.drawable.ic_low_carb))
        dietList.add(Diet("Dairy-Free", R.drawable.ic_dairy_free))
        dietList.add(Diet("Nut Allergy", R.drawable.ic_nut_allergy))
        dietList.add(Diet("Low-FODMAP", R.drawable.ic_low_fodmap))
        dietList.add(Diet("Halal", R.drawable.ic_halal))
        dietList.add(Diet("Kosher", R.drawable.ic_kosher))
    }
}