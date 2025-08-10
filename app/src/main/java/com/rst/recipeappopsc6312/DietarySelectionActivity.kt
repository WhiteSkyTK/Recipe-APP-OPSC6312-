package com.rst.recipeappopsc6312

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietary_selection)

        recyclerView = findViewById(R.id.recyclerViewDiets)

        prepareDietData()

        dietAdapter = DietAdapter(dietList)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns
        recyclerView.adapter = dietAdapter
    }

    private fun prepareDietData() {
        // You need to add corresponding images to your res/drawable folder
        // For example, 'ic_vegetarian.png', 'ic_vegan.png', etc.
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
    }
}