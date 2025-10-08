package com.rst.recipeappopsc6312

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var nutritionLayout: LinearLayout
    private lateinit var nutritionScrollView: HorizontalScrollView // ++ ADD
    private lateinit var noNutritionTextView: TextView
    private lateinit var loadingIndicator: ProgressBar
    private var currentRecipe: Recipe? = null
    private var currentServings = 0
    private var originalServings = 0
    private var isIngredientSelectionMode = false
    private var viewStartTime: Long = 0 // ++ ADD property to store start time
    private var recipeId: String? = null // ++ ADD property to store recipe ID
    private var missingIngredients: List<String>? = null


    private val shoppingListViewModel: ShoppingListViewModel by viewModels {
        val database = AppDatabase.getDatabase(application)
        // Provide the missing FirebaseStorage instance as the fourth argument
        val repository = ShoppingRepository(
            database.shoppingDao(),
            database.recipeDao(),
            database.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance() // <-- This was the missing part
        )
        ShoppingViewModelFactory(repository)
    }

    private val recipeDetailViewModel: RecipeDetailViewModel by viewModels {
        val database = AppDatabase.getDatabase(application)
        val repository = ShoppingRepository(
            database.shoppingDao(),
            database.recipeDao(),
            database.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        RecipeDetailViewModelFactory(repository)
    }


    // UI Elements that are relatively static or only need to be found once
    private lateinit var recipeImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var authorTextView: TextView
    private lateinit var ingredientsRecyclerView: RecyclerView
    private lateinit var methodRecyclerView: RecyclerView
    private lateinit var descriptionTextView: TextView
    private lateinit var readMoreTextView: TextView
    private lateinit var favoriteFab: FloatingActionButton
    private lateinit var servingsValueTextView: TextView
    private lateinit var decreaseButton: TextView
    private lateinit var increaseButton: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Initialize UI elements
        loadingIndicator = findViewById(R.id.loadingIndicator)
        recipeImageView = findViewById(R.id.imageViewRecipe)
        titleTextView = findViewById(R.id.textViewRecipeTitle)
        timeTextView = findViewById(R.id.textViewTime)
        authorTextView = findViewById(R.id.textViewAuthor)
        descriptionTextView = findViewById(R.id.textViewRecipeDescription)
        readMoreTextView = findViewById(R.id.textViewReadMore) // Add this to your layout if you don't have it
        ingredientsRecyclerView = findViewById(R.id.recyclerViewIngredients)
        methodRecyclerView = findViewById(R.id.recyclerViewMethod)
        favoriteFab = findViewById(R.id.fabFavorite)
        servingsValueTextView = findViewById(R.id.textViewServingsValue)
        decreaseButton = findViewById(R.id.buttonDecreaseServings)
        increaseButton = findViewById(R.id.buttonIncreaseServings)
        nutritionLayout = findViewById(R.id.nutritionLayout)
        nutritionScrollView = findViewById(R.id.nutritionScrollView) // ++ FIND
        noNutritionTextView = findViewById(R.id.textViewNoNutrition)

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

        val recipeId = intent.getStringExtra("RECIPE_ID")
        if (recipeId == null) {
            Toast.makeText(this, "Recipe ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        missingIngredients = intent.getStringArrayListExtra("MISSING_INGREDIENTS")

        observeViewModel()
        recipeDetailViewModel.fetchRecipe(recipeId) // Pass the source
    }

    override fun onStart() {
        super.onStart()
        // ++ RECORD the time when the screen becomes visible
        viewStartTime = System.currentTimeMillis()
    }

    override fun onStop() {
        super.onStop()
        // When the user leaves, calculate the time spent and log it
        if (viewStartTime > 0 && recipeId != null) {
            val viewEndTime = System.currentTimeMillis()
            val durationSeconds = (viewEndTime - viewStartTime) / 1000
            if (durationSeconds > 1) { // Only log if they spent more than a second
                recipeDetailViewModel.repository.logRecipeView(recipeId!!, durationSeconds)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_to_shopping_list -> {
                showAddToListDialog()
                true
            }
            R.id.action_confirm_selection -> {
                confirmIngredientSelection()
                true
            }
            R.id.action_publish_recipe -> {
                AlertDialog.Builder(this)
                    .setTitle("Publish Recipe")
                    .setMessage("Are you sure you want to make this recipe public for everyone to see?")
                    .setPositiveButton("Publish") { _, _ ->
                        recipeDetailViewModel.publishCurrentRecipe()
                        Toast.makeText(this, "Recipe published!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            R.id.action_unpublish_recipe -> {
                recipeDetailViewModel.unpublishCurrentRecipe()
                Toast.makeText(this, "Recipe is now private.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_edit_recipe -> {
                // Create an Intent to go back to MainActivity
                val intent = Intent(this, MainActivity::class.java).apply {
                    // Add a special message (an "extra") to tell MainActivity
                    // that we want to open the AddRecipeFragment.
                    putExtra("NAVIGATE_TO", "ADD_RECIPE_FRAGMENT")
                    putExtra("EDIT_RECIPE_ID", currentRecipe?.id)
                    // Clear the activity stack so we don't have a strange back-button history
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish() // Close the detail screen
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmIngredientSelection() {
        val selected = ingredientAdapter.selectedIngredients
        if (selected.isNotEmpty()) {
            val recipeTitle = currentRecipe!!.title
            val ingredientNames = selected.map { it.name }
            shoppingListViewModel.createListFromRecipe(recipeTitle, ingredientNames)
            Toast.makeText(this, "${selected.size} ingredients added to new list", Toast.LENGTH_LONG).show()
        }

        // Reset the state
        isIngredientSelectionMode = false
        invalidateOptionsMenu() // Redraw the toolbar again
        ingredientAdapter.setSelectionMode(false)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val addIcon = menu.findItem(R.id.action_add_to_shopping_list)
        val confirmIcon = menu.findItem(R.id.action_confirm_selection)
        val publishIcon = menu.findItem(R.id.action_publish_recipe)
        val unpublishIcon = menu.findItem(R.id.action_unpublish_recipe)
        val editIcon = menu.findItem(R.id.action_edit_recipe)

        // Your existing logic for add/confirm icons
        addIcon.isVisible = !isIngredientSelectionMode
        confirmIcon.isVisible = isIngredientSelectionMode

        // ++ ADD THIS LOGIC for the publish icon
        val currentUser = FirebaseManager.auth.currentUser
        val recipe = recipeDetailViewModel.recipe.value

        // Show the publish icon ONLY if the recipe is loaded, is private,
        // and the current user is the author.
        publishIcon.isVisible = recipe != null && !recipe.isPublic && currentUser?.uid == recipe.userId

        // Show the unpublish icon ONLY if the recipe is loaded, is PUBLIC,
        // and the current user is the author.
        unpublishIcon.isVisible = recipe != null && recipe.isPublic && currentUser?.uid == recipe.userId

        // Show the edit icon ONLY if the current user is the author.
        editIcon.isVisible = recipe != null && currentUser?.uid == recipe.userId

        return super.onPrepareOptionsMenu(menu)
    }

    private fun showAddToListDialog() {
        // First, make sure we have a recipe loaded
        currentRecipe?.let { recipe ->
            val options = arrayOf("Add all ingredients", "Select ingredients to add")
            AlertDialog.Builder(this)
                .setTitle("Add to Shopping List")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // "Add all ingredients" was clicked
                            val recipeTitle = recipe.title
                            val ingredients = recipe.ingredients.map { it.name }
                            shoppingListViewModel.createListFromRecipe(recipeTitle, ingredients)
                            Toast.makeText(
                                this,
                                "Added ingredients from $recipeTitle to a new list",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        1 -> { // "Select ingredients" was clicked
                            isIngredientSelectionMode = true
                            invalidateOptionsMenu() // This tells the toolbar to redraw itself
                            ingredientAdapter.setSelectionMode(true)
                            Toast.makeText(this, "Select ingredients to add", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        } ?: Toast.makeText(this, "Recipe not loaded yet.", Toast.LENGTH_SHORT).show()
    }
    private fun observeViewModel() {
        recipeDetailViewModel.recipe.observe(this) { fetchedRecipe ->
            if (fetchedRecipe != null) {
                currentRecipe = fetchedRecipe // First, update the current recipe

                // Then, redraw the menu now that you have the correct data
                invalidateOptionsMenu()

                // The rest of your logic is perfect
                currentServings = fetchedRecipe.servings
                originalServings = fetchedRecipe.servings
                populateUi(fetchedRecipe)
                loadingIndicator.visibility = View.GONE
            } else {
                if (recipeDetailViewModel.error.value == null) { // Check if an error message was already posted
                    // Toast.makeText(this, "Recipe details not found.", Toast.LENGTH_SHORT).show()
                    // Consider not finishing immediately, error observer might handle it or show a specific UI state
                }
            }
        }

        recipeDetailViewModel.isFavorite.observe(this) { isFavorite ->
            // If isFavorite is null (meaning not in the DB), treat it as false.
            val heartIcon = if (isFavorite == true) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            favoriteFab.setImageResource(heartIcon)
        }

        recipeDetailViewModel.isLoading.observe(this) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Optionally hide/show main content view
            // findViewById<View>(R.id.content_group).visibility = if(isLoading) View.GONE else View.VISIBLE
        }

        recipeDetailViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Only finish if no recipe was ever loaded and an error occurred
                if (currentRecipe == null) {
                    // finish() // Decide if you want to automatically finish on error
                }
            }
        }
    }

    private fun populateUi(recipeToDisplay: Recipe) {
        currentRecipe = recipeToDisplay
        Glide.with(this).load(recipeToDisplay.imageUrl).into(recipeImageView)
        titleTextView.text = recipeToDisplay.title
        timeTextView.text = "${recipeToDisplay.timeInMins} mins"
        authorTextView.text = "by ${recipeToDisplay.author}"
        descriptionTextView.text = recipeToDisplay.description // Populate description

        descriptionTextView.post {
            // ++ ADD THIS LOG to see the line count
            Log.d("ReadMoreDebug", "Description line count is: ${descriptionTextView.lineCount}")

            if (descriptionTextView.lineCount >= 4) {
                readMoreTextView.visibility = View.VISIBLE
                readMoreTextView.setOnClickListener {
                    toggleReadMoreState()
                }
            } else {
                readMoreTextView.visibility = View.GONE
            }
        }

        // Populate nutrition facts
        nutritionLayout.removeAllViews()
        recipeToDisplay.nutrition.forEach { fact ->
            val factView = LayoutInflater.from(this).inflate(R.layout.item_nutrition_fact, nutritionLayout, false)
            factView.findViewById<TextView>(R.id.textViewNutritionValue).text = fact.value
            factView.findViewById<TextView>(R.id.textViewNutritionLabel).text = fact.label
            nutritionLayout.addView(factView)
        }

        if (recipeToDisplay.nutrition.isEmpty()) {
            // If the list is empty, hide the scroll view and show the message
            nutritionScrollView.visibility = View.GONE
            noNutritionTextView.visibility = View.VISIBLE
        } else {
            // If there is data, show the scroll view and hide the message
            nutritionScrollView.visibility = View.VISIBLE
            noNutritionTextView.visibility = View.GONE

            nutritionLayout.removeAllViews()
            recipeToDisplay.nutrition.forEach { fact ->
                val factView = LayoutInflater.from(this).inflate(R.layout.item_nutrition_fact, nutritionLayout, false)
                factView.findViewById<TextView>(R.id.textViewNutritionValue).text = fact.value
                factView.findViewById<TextView>(R.id.textViewNutritionLabel).text = fact.label
                nutritionLayout.addView(factView)
            }
        }

        // Setup RecyclerViews
        // Ensure adapters can handle new list submissions gracefully
        ingredientAdapter = IngredientAdapter(recipeToDisplay.ingredients.toMutableList(), missingIngredients)
        ingredientsRecyclerView.layoutManager = LinearLayoutManager(this)
        ingredientsRecyclerView.adapter = ingredientAdapter

        methodRecyclerView.layoutManager = LinearLayoutManager(this)
        methodRecyclerView.adapter = MethodAdapter(recipeToDisplay.method.toMutableList())



        servingsValueTextView.text = currentServings.toString()

        decreaseButton.setOnClickListener {
            if (currentServings > 1) {
                currentServings--
                updateServingsAndValues(recipeToDisplay)
            }
        }

        increaseButton.setOnClickListener {
            currentServings++
            updateServingsAndValues(recipeToDisplay)
        }

        favoriteFab.setOnClickListener {
            recipeDetailViewModel.toggleFavorite(recipeToDisplay)
        }

        ingredientAdapter = IngredientAdapter(recipeToDisplay.ingredients.toMutableList(), missingIngredients)
        ingredientsRecyclerView.layoutManager = LinearLayoutManager(this)
        ingredientsRecyclerView.adapter = ingredientAdapter
    }

    private fun updateServingsAndValues(recipeForServings: Recipe) {
        servingsValueTextView.text = currentServings.toString()
        val ratio = currentServings.toDouble() / originalServings.toDouble()

        val newIngredients = recipeForServings.ingredients.map { originalIngredient ->
            // ++ THIS IS NOW SAFE AND RELIABLE ++
            // Because the mapper cleaned the data, this will always work.
            val originalQty = originalIngredient.quantity.toDoubleOrNull() ?: 0.0

            val newQty = originalQty * ratio

            // This formatting logic is now correct
            val newQtyStringValue = if (newQty == 0.0) "0"
            else if (newQty < 1 && newQty > 0) String.format("%.2f", newQty).removeSuffix("0").removeSuffix("0").removeSuffix(".")
            else if (newQty % 1 == 0.0) newQty.toInt().toString()
            else String.format("%.1f", newQty).removeSuffix("0").removeSuffix(".")

            Ingredient(
                name = originalIngredient.name,
                quantity = newQtyStringValue,
                unit = originalIngredient.unit
            )
        }
        ingredientAdapter.updateIngredients(newIngredients)
        updateNutritionFacts(ratio, recipeForServings.nutrition)
    }

    private fun updateNutritionFacts(ratio: Double, originalNutrition: List<NutritionFact>) {
        nutritionLayout.removeAllViews()
        originalNutrition.forEach { fact ->
            val factView = LayoutInflater.from(this).inflate(R.layout.item_nutrition_fact, nutritionLayout, false)
            val valueTextView = factView.findViewById<TextView>(R.id.textViewNutritionValue)
            val labelTextView = factView.findViewById<TextView>(R.id.textViewNutritionLabel)

            val originalValue = fact.value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            val unit = fact.value.filter { it.isLetter() || it == '%' }.trim()

            val newValueString = if (originalValue != null) {
                val newValue = originalValue * ratio
                if (newValue % 1 == 0.0) newValue.toInt().toString() else String.format("%.1f", newValue)
            } else {
                fact.value
            }
            valueTextView.text = "$newValueString$unit".trim()
            labelTextView.text = fact.label
            nutritionLayout.addView(factView)
        }
    }

    private fun toggleReadMoreState() {
        if (readMoreTextView.text.toString().equals("Read More", ignoreCase = true)) {
            descriptionTextView.maxLines = Integer.MAX_VALUE
            descriptionTextView.ellipsize = null // Remove the "..."
            readMoreTextView.text = "Read Less"
        } else {
            descriptionTextView.maxLines = 4
            descriptionTextView.ellipsize = android.text.TextUtils.TruncateAt.END // Restore the "..."
            readMoreTextView.text = "Read More"
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}