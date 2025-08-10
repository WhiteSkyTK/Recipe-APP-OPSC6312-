package com.rst.recipeappopsc6312

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class RecipeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide the title initially

        // Here you would:
        // 1. Get the recipe ID passed from the previous screen.
        // 2. Fetch the full recipe details from Supabase.
        // 3. Create and set up the adapters for the ingredient and method RecyclerViews.
        // 4. Populate all the TextViews and the ImageView with the fetched data.
        // 5. Set a click listener on the FAB to handle favoriting the recipe.
    }

    // This is needed to make the toolbar's back button work
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}