package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ScanResultsActivity : AppCompatActivity() {


    private val viewModel: ScanViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        // ++ UPDATE this to match the new constructor
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(), // Pass the new DAO
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        ScanViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_results)
        enableEdgeToEdge()
        val aboutUsLayout = findViewById<View>(R.id.scan_result_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(aboutUsLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewResults)
        val ingredients = intent.getStringArrayListExtra("INGREDIENTS")

        val matchAdapter = RecipeMatchAdapter(emptyList()) { recipe ->
            // Handle click to open RecipeDetailActivity
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = matchAdapter

        viewModel.recipeMatches.observe(this) { matches ->
            matchAdapter.updateMatches(matches)
        }

        if (!ingredients.isNullOrEmpty()) {
            viewModel.findRecipes(ingredients)
        }
    }
}