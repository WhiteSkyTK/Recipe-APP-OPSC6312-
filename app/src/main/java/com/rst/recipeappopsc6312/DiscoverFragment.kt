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
    private lateinit var recipeAdapter: RecipeAdapter
    private var recipeList = ArrayList<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewDiscover)

        // In a real app, you would fetch this data from Supabase
        prepareDummyData()

        recipeAdapter = RecipeAdapter(recipeList)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = recipeAdapter

        return view
    }

    private fun prepareDummyData() {
        // Placeholder data for testing. Replace image URLs with real ones.
        recipeList.add(Recipe("1", "Blueberry muffins", "https://images.unsplash.com/photo-1593443320739-73f44974a104", 40, true))
        recipeList.add(Recipe("2", "Blackberry fool", "https://images.unsplash.com/photo-1626201343755-75e1a72970a4", 20))
        recipeList.add(Recipe("3", "Frozen yoghurt cake", "https://images.unsplash.com/photo-1563805042-762955212533", 15))
        recipeList.add(Recipe("4", "Limoncello fro-yo", "https://images.unsplash.com/photo-1567206563064-6f60f40a2b57", 10, true))
    }
}