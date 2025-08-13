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
        //TODO: Add edge-to-edge support
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
            continueButton.text = "Save Changes"
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
                Toast.makeText(this, "Please select at least one cuisine", Toast.LENGTH_SHORT).show()
            } else {
                val selectedCuisineNames = selectedCuisines.map { it.name }
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
            (it as Button).text = if (isAllSelected) "Deselect All" else "Select All"
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
                Toast.makeText(this, "Preferences updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prepareCuisineData() {
        // Here is a comprehensive list. You will need to add corresponding
        cuisineList.add(Cuisine("Italian", R.drawable.ic_italian))
        cuisineList.add(Cuisine("Mexican", R.drawable.ic_mexican))
        cuisineList.add(Cuisine("Chinese", R.drawable.ic_chinese))
        cuisineList.add(Cuisine("Japanese", R.drawable.ic_japanese))
        cuisineList.add(Cuisine("Indian", R.drawable.ic_indian))
        cuisineList.add(Cuisine("Thai", R.drawable.ic_thai))
        cuisineList.add(Cuisine("French", R.drawable.ic_french))
        cuisineList.add(Cuisine("Spanish", R.drawable.ic_spanish))
        cuisineList.add(Cuisine("Greek", R.drawable.ic_greek))
        cuisineList.add(Cuisine("American", R.drawable.ic_american))
        cuisineList.add(Cuisine("Korean", R.drawable.ic_korean))
        cuisineList.add(Cuisine("Vietnamese", R.drawable.ic_vietnamese))
        cuisineList.add(Cuisine("Mediterranean", R.drawable.ic_mediterranean))
        cuisineList.add(Cuisine("Caribbean", R.drawable.ic_caribbean))
        cuisineList.add(Cuisine("African", R.drawable.ic_african))
    }
}