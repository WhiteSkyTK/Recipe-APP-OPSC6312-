package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find all the RecyclerViews and buttons
        val featuredRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFeatured)
        val categoriesRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        val recommendedRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecommended)
        val seeAllCategories = view.findViewById<TextView>(R.id.textViewCategorySeeAll)

        // --- Setup Click Listeners ---

        // A shared click listener for any recipe card that navigates to the detail screen
        val onRecipeClicked = { recipe: Recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }

        seeAllCategories.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(CategoryFragment(), -1)
        }

        // --- Setup Adapters and Layout Managers ---

        // 1. Featured Recipes: Uses the special FeaturedRecipeAdapter
        featuredRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        featuredRecyclerView.adapter = FeaturedRecipeAdapter(DummyData.getFeaturedRecipes(), onRecipeClicked)

        // 2. Categories: Uses the CategoryAdapter
        categoriesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoriesRecyclerView.adapter = CategoryAdapter(DummyData.getCategories()) { category ->
            // Handle category chip click, e.g., navigate to a filtered list or show a toast
            Toast.makeText(context, "${category.name} clicked", Toast.LENGTH_SHORT).show()
        }

        // 3. Recommended Recipes: Uses the standard RecipeAdapter
        recommendedRecyclerView.layoutManager = GridLayoutManager(context, 2)
        recommendedRecyclerView.adapter = RecipeAdapter(DummyData.getRecommendedRecipes(), onRecipeClicked)

        return view
    }
}
