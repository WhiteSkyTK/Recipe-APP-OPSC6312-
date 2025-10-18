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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        val dietSelectionLayout = findViewById<View>(R.id.dietSelectionLayout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(dietSelectionLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }
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
            continueButton.text = getString(R.string.save_changes)
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
                Toast.makeText(this, getString(R.string.diet_selection_toast_at_least_one), Toast.LENGTH_SHORT).show()
            } else {
                // Correctly map the resource IDs to actual strings
                val selectedDietNames = selectedDiets.map { getString(it.name) }
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
                Toast.makeText(this, getString(R.string.cuisine_selection_toast_preferences_updated), Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.cuisine_selection_toast_update_failed, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun prepareDietData() {
        // Now gets data from the central DietData object
        dietList = ArrayList(DietData.getAllDiets())
    }
}