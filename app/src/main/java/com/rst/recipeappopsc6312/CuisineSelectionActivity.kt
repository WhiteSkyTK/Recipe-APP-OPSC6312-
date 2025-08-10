package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CuisineSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cuisineAdapter: CuisineAdapter
    private var cuisineList = ArrayList<Cuisine>()
    private lateinit var registrationData: RegistrationData
    private var isAllSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuisine_selection)

        // Receive the data object from the previous screen
        registrationData = intent.getParcelableExtra("REGISTRATION_DATA") ?: RegistrationData()

        val continueButton = findViewById<Button>(R.id.buttonContinue)
        val skipButton = findViewById<Button>(R.id.buttonSkip)
        val selectAllButton = findViewById<Button>(R.id.buttonSelectAll)
        recyclerView = findViewById(R.id.recyclerViewCuisines)

        prepareCuisineData()

        cuisineAdapter = CuisineAdapter(cuisineList)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = cuisineAdapter

        // --- Button Logic ---

        continueButton.setOnClickListener {
            val selectedCuisines = cuisineAdapter.getSelectedCuisines()
            if (selectedCuisines.isEmpty()) {
                Toast.makeText(this, "Please select at least one cuisine or skip", Toast.LENGTH_SHORT).show()
            } else {
                // Save the selected data and navigate
                registrationData.selectedCuisines = selectedCuisines.map { it.name }
                navigateToNextScreen()
            }
        }

        skipButton.setOnClickListener {
            // Set cuisines to an empty list and navigate
            registrationData.selectedCuisines = emptyList()
            navigateToNextScreen()
        }

        selectAllButton.setOnClickListener {
            isAllSelected = !isAllSelected
            cuisineAdapter.selectAll(isAllSelected)
            (it as Button).text = if (isAllSelected) "Deselect All" else "Select All"
        }
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, DietarySelectionActivity::class.java)
        intent.putExtra("REGISTRATION_DATA", registrationData)
        startActivity(intent)
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