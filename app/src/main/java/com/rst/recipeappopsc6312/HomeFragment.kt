package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

        val seeAllCategories = view.findViewById<TextView>(R.id.textViewCategorySeeAll)
        seeAllCategories.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(CategoryFragment())
        }

        // --- Setup RecyclerViews with correct LayoutManagers ---

        // Featured: Horizontal List
        val featuredRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFeatured)
        featuredRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        // TODO: Set adapter for featuredRecyclerView

        // Categories: Horizontal List
        val categoriesRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        categoriesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        // TODO: Set adapter for categoriesRecyclerView

        // Recommended: Vertical Grid with 2 columns
        val recommendedRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecommended)
        recommendedRecyclerView.layoutManager = GridLayoutManager(context, 2)
        // TODO: Set adapter for recommendedRecyclerView

        return view
    }
}