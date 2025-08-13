package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var recipe: Recipe
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var nutritionLayout: LinearLayout
    private var currentServings = 0
    private var originalServings = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        enableEdgeToEdge()
        val recipeLayout = findViewById<View>(R.id.recipe_detail_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(recipeLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get the recipe ID passed from the previous screen
        val recipeId = intent.getStringExtra("RECIPE_ID")
        if (recipeId == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch the full recipe details
        val fetchedRecipe = DummyData.getRecipeById(recipeId)
        if (fetchedRecipe == null) {
            Toast.makeText(this, "Recipe details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recipe = fetchedRecipe
        currentServings = recipe.servings
        originalServings = recipe.servings

        populateUi()
    }

    private fun populateUi() {
        val recipeImageView = findViewById<ImageView>(R.id.imageViewRecipe)
        val titleTextView = findViewById<TextView>(R.id.textViewRecipeTitle)
        val timeTextView = findViewById<TextView>(R.id.textViewTime)
        val authorTextView = findViewById<TextView>(R.id.textViewAuthor)
        val ingredientsRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewIngredients)
        val methodRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewMethod)
        val favoriteFab = findViewById<FloatingActionButton>(R.id.fabFavorite)
        val servingsValueTextView = findViewById<TextView>(R.id.textViewServingsValue)
        val decreaseButton = findViewById<TextView>(R.id.buttonDecreaseServings)
        val increaseButton = findViewById<TextView>(R.id.buttonIncreaseServings)

        nutritionLayout = findViewById(R.id.nutritionLayout)

        // Populate header
        Glide.with(this).load(recipe.imageUrl).into(recipeImageView)
        titleTextView.text = recipe.title
        timeTextView.text = "${recipe.timeInMins} mins"
        authorTextView.text = "by ${recipe.author}"

        // Populate nutrition facts
        nutritionLayout.removeAllViews()
        for (fact in recipe.nutrition) {
            val factView = LayoutInflater.from(this).inflate(R.layout.item_nutrition_fact, nutritionLayout, false)
            factView.findViewById<TextView>(R.id.textViewNutritionValue).text = fact.value
            factView.findViewById<TextView>(R.id.textViewNutritionLabel).text = fact.label
            nutritionLayout.addView(factView)
        }

        // Setup RecyclerViews
        ingredientAdapter = IngredientAdapter(recipe.ingredients)
        ingredientsRecyclerView.layoutManager = LinearLayoutManager(this)
        ingredientsRecyclerView.adapter = ingredientAdapter

        methodRecyclerView.layoutManager = LinearLayoutManager(this)
        methodRecyclerView.adapter = MethodAdapter(recipe.method)

        // Set initial servings text
        servingsValueTextView.text = currentServings.toString()

        // --- Servings Adjuster Logic ---
        decreaseButton.setOnClickListener {
            if (currentServings > 1) {
                currentServings--
                updateServingsAndValues()
            }
        }

        increaseButton.setOnClickListener {
            currentServings++
            updateServingsAndValues()
        }

        // Setup favorite button
        val heartIcon = if (FavoritesManager.isFavorite(this, recipe.id)) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        favoriteFab.setImageResource(heartIcon)
        favoriteFab.setOnClickListener {
            FavoritesManager.toggleFavorite(this, recipe.id)
            val newIcon = if (FavoritesManager.isFavorite(this, recipe.id)) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            (it as FloatingActionButton).setImageResource(newIcon)
        }
    }

    private fun updateServingsAndValues() {
        val servingsValueTextView = findViewById<TextView>(R.id.textViewServingsValue)
        servingsValueTextView.text = currentServings.toString()

        val ratio = currentServings.toDouble() / originalServings.toDouble()

        // Update Ingredients
        val newIngredients = recipe.ingredients.map { originalIngredient ->
            val originalQty = originalIngredient.quantity.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            val unit = originalIngredient.quantity.filter { it.isLetter() }.trim()

            val newQtyString = if (originalQty != null) {
                val newQty = originalQty * ratio
                if (newQty % 1 == 0.0) newQty.toInt().toString() else String.format("%.1f", newQty)
            } else {
                originalIngredient.quantity
            }

            Ingredient(originalIngredient.name, "$newQtyString $unit".trim())
        }
        ingredientAdapter.updateIngredients(newIngredients)

        // --- NEW: Update Nutrition Facts ---
        updateNutritionFacts(ratio)
    }


    private fun updateNutritionFacts(ratio: Double) {
        nutritionLayout.removeAllViews()
        for (fact in recipe.nutrition) {
            val factView = LayoutInflater.from(this).inflate(R.layout.item_nutrition_fact, nutritionLayout, false)

            val valueTextView = factView.findViewById<TextView>(R.id.textViewNutritionValue)
            val labelTextView = factView.findViewById<TextView>(R.id.textViewNutritionLabel)

            val originalValue = fact.value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            val unit = fact.value.filter { it.isLetter() }.trim()

            val newValueString = if (originalValue != null) {
                val newValue = originalValue * ratio
                String.format("%.1f", newValue) // Always show 1 decimal for nutrition
            } else {
                fact.value // Keep non-numeric values as is
            }

            valueTextView.text = newValueString
            labelTextView.text = fact.label
            nutritionLayout.addView(factView)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}