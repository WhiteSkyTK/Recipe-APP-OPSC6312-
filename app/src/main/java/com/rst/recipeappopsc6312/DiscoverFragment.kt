package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DiscoverFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    // You will create a RecipeAdapter similar to your other adapters
    // private lateinit var recipeAdapter: RecipeAdapter
    private var recipeList = ArrayList<Recipe>() // You'll need a Recipe data class

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_discover, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewDiscover)

        // In a real app, you would fetch this data from your backend (Supabase)
        prepareDummyData()

        // recipeAdapter = RecipeAdapter(recipeList)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        // recyclerView.adapter = recipeAdapter

        return view
    }

    private fun prepareDummyData() {
        // Placeholder data for testing the layout
        // You'll need to create a 'Recipe' data class and add your own images
        // recipeList.add(Recipe("Blueberry muffins", R.drawable.muffins, "40 Min"))
        // recipeList.add(Recipe("Blackberry fool", R.drawable.fool, "20 Min"))
        // ... and so on
    }
}