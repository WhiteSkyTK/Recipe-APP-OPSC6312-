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

class CuisineSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cuisineAdapter: CuisineAdapter
    private var cuisineList = ArrayList<Cuisine>()
    private lateinit var registrationData: RegistrationData
    private var isAllSelected = false
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuisine_selection)

        enableEdgeToEdge()
        val cusaineSelectionLayout = findViewById<View>(R.id.cuisineSelectionLayout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(cusaineSelectionLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        val continueButton = findViewById<Button>(R.id.buttonContinue)
        val skipButton = findViewById<Button>(R.id.buttonSkip)
        val selectAllButton = findViewById<Button>(R.id.buttonSelectAll)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewCuisines)

        if (isEditMode) {
            progressBar.visibility = View.GONE
            continueButton.text = getString(R.string.save_changes)
            skipButton.visibility = View.GONE // Hide skip button in edit mode
            // In edit mode, you would fetch the user's current selections from Firestore
            // and pre-select them in the 'prepareCuisineData' function.
        }
        prepareCuisineData()

        // The adapter now handles its own clicks
        cuisineAdapter = CuisineAdapter { clickedCuisine ->
            val updatedList = cuisineList.map {
                if (it.name == clickedCuisine.name) it.copy(isSelected = !it.isSelected) else it
            }
            cuisineList = ArrayList(updatedList)
            cuisineAdapter.submitList(cuisineList)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = cuisineAdapter
        cuisineAdapter.submitList(cuisineList)

        // --- Button Logic ---
        backButton.setOnClickListener {
            finish() // This will close the current activity and go back to CountrySelectionActivity
        }

        continueButton.setOnClickListener {
            val selectedCuisines = cuisineList.filter { it.isSelected }
            if (selectedCuisines.isEmpty()) {
                Toast.makeText(this, getString(R.string.cuisine_selection_toast_at_least_one), Toast.LENGTH_SHORT).show()
            } else {
                val selectedCuisineNames = selectedCuisines.map { getString(it.name) }
                if (isEditMode) {
                    updateUserCuisines(selectedCuisineNames)
                } else {
                    registrationData.selectedCuisines = selectedCuisineNames
                    navigateToNextScreen()
                }
            }
        }


        skipButton.setOnClickListener {
            // Set cuisines to an empty list and navigate
            registrationData.selectedCuisines = emptyList()
            navigateToNextScreen()
        }

        selectAllButton.setOnClickListener {
            isAllSelected = !isAllSelected
            // Update the entire list's selected state
            val updatedList = cuisineList.map { it.copy(isSelected = isAllSelected) }
            cuisineList = ArrayList(updatedList)
            cuisineAdapter.submitList(cuisineList) // Submit the updated list
            (it as Button).text = if (isAllSelected) getString(R.string.deselect_all) else getString(R.string.select_all)
        }
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, DietarySelectionActivity::class.java)
        intent.putExtra("REGISTRATION_DATA", registrationData)
        startActivity(intent)
    }

    private fun updateUserCuisines(cuisines: List<String>) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        // Show loading indicator
        FirebaseManager.firestore.collection("users").document(userId)
            .update("selected_cuisines", cuisines)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.cuisine_selection_toast_preferences_updated), Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.cuisine_selection_toast_update_failed, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun prepareCuisineData() {
        // It now gets the data from our single source of truth.
        cuisineList = ArrayList(CuisineData.getAllCuisines())
    }
}