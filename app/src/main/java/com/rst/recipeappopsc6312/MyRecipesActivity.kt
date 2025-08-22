package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyRecipesActivity : AppCompatActivity() {

    private val TAG = "MyRecipesActivity"
    private lateinit var recyclerView: RecyclerView
    private lateinit var myRecipesAdapter: HomeRecipeAdapter
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var emptyStateTextView: TextView

    private val viewModel: ShoppingViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance())
        ShoppingViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_recipes)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.my_recipes_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        recyclerView = findViewById(R.id.recyclerViewMyRecipes)
        emptyStateTextView = findViewById(R.id.textViewEmptyState)
        // ++ CHANGE 2: Find the FrameLayout by its ID
        loadingOverlay = findViewById(R.id.loadingOverlayContainer)

        backButton.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        observeMyRecipes()
    }

    private fun setupRecyclerView() {
        val onRecipeClicked = { clickedRecipe: Recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", clickedRecipe.id)
            startActivity(intent)
        }

        val onFavoriteClicked = { recipe: Recipe ->
            // Tell the ViewModel to handle the favorite logic
            viewModel.toggleFavorite(recipe)
            // Note: We don't need to manually update the UI here anymore,
            // the LiveData observer in the adapter will do it automatically.
        }

        // Initialize the adapter with BOTH click listeners
        myRecipesAdapter = HomeRecipeAdapter(
            recipeList = emptyList(),
            onRecipeClick = onRecipeClicked,
            onFavoriteClick = { recipe -> viewModel.toggleFavorite(recipe) },
            favoritesLiveData = viewModel.favoriteIds,
            lifecycleOwner = this // The Activity is the LifecycleOwner
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = myRecipesAdapter
    }

    private fun observeMyRecipes() {
        val userId = FirebaseManager.auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to see your recipes.", Toast.LENGTH_SHORT).show()
            return
        }

        loadingOverlay.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateTextView.visibility = View.GONE

        // Get the local database instance
        val recipeDao = AppDatabase.getDatabase(this).recipeDao()

        // 1. Observe the local database for changes
        lifecycleScope.launch {
            recipeDao.getUserRecipes(userId).collectLatest { myRecipes ->
                loadingOverlay.visibility = View.GONE

                if (myRecipes.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyStateTextView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyStateTextView.visibility = View.GONE
                    myRecipesAdapter.updateData(myRecipes)
                }
                Log.d(TAG, "UI updated with ${myRecipes.size} recipes from RoomDB.")
            }
        }


        // 2. Fetch the latest data from Firestore in the background
        FirebaseManager.firestore.collection("recipes").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { documents ->
                val firestoreRecipes = documents.toObjects(Recipe::class.java)
                // 3. Save the new data to Room (which will trigger the observer above)
                lifecycleScope.launch {
                    recipeDao.insertAllRecipes(firestoreRecipes)
                    Log.d(TAG, "Fetched ${firestoreRecipes.size} recipes from Firestore and updated RoomDB.")
                }
            }
            .addOnFailureListener { e ->
                loadingOverlay.visibility = View.GONE
                Log.e(TAG, "Error fetching recipes from Firestore", e)
            }
    }

}