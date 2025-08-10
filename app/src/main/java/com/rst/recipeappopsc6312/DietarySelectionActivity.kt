package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DietarySelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dietAdapter: DietAdapter
    private var dietList = ArrayList<Diet>()
    private lateinit var registrationData: RegistrationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietary_selection)

        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        // Find views
        val continueButton = findViewById<Button>(R.id.buttonContinue)
        val skipButton = findViewById<Button>(R.id.buttonSkip)
        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        recyclerView = findViewById(R.id.recyclerViewDiets)

        // Set up the data and RecyclerView
        prepareDietData()
        dietAdapter = DietAdapter(dietList)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // Using 2 columns as per your design
        recyclerView.adapter = dietAdapter

        // --- Button Logic ---

        backButton.setOnClickListener {
            finish() // Go back to the previous screen
        }

        continueButton.setOnClickListener {
            val selectedDiets = dietAdapter.getSelectedDiets()
            if (selectedDiets.isEmpty()) {
                Toast.makeText(this, "Please select at least one preference or skip", Toast.LENGTH_SHORT).show()
            } else {
                // Save the selected data and navigate
                registrationData.selectedDiets = selectedDiets.map { it.name }
                navigateToNextScreen()
            }
        }

        skipButton.setOnClickListener {
            // Set diets to an empty list and navigate
            registrationData.selectedDiets = emptyList()
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, ProfileCompletionActivity::class.java)
        intent.putExtra("REGISTRATION_DATA", registrationData)
        startActivity(intent)
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