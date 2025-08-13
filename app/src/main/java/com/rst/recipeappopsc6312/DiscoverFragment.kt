package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DiscoverFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private var recipeList = mutableListOf<Recipe>()
    private var isLoading = false
    private var currentPage = 0
    private val pageSize = 10 // Load 10 items at a time

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewDiscover)
        val searchEditText = view.findViewById<EditText>(R.id.editTextSearch)

        setupRecyclerView()
        loadMoreRecipes()

        // Add the search listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // The filter call will now work correctly
                recipeAdapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun setupRecyclerView() {
        // Define what happens when a recipe is clicked
        val onRecipeClicked = { recipe: Recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }

        // FIXED: Initialize the adapter correctly with the click listener
        recipeAdapter = RecipeAdapter(recipeList, onRecipeClicked)
        val layoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = recipeAdapter

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        loadMoreRecipes()
                    }
                }
            }
        })
    }

    private fun loadMoreRecipes() {
        isLoading = true
        // Simulate a network delay
        Handler(Looper.getMainLooper()).postDelayed({
            val newRecipes = DummyData.getDiscoverRecipes(currentPage, pageSize)
            if (newRecipes.isNotEmpty()) {
                val startPosition = recipeList.size
                recipeList.addAll(newRecipes)
                recipeAdapter.notifyItemRangeInserted(startPosition, newRecipes.size)
                currentPage++
            }
            isLoading = false
        }, 500) // 0.5 second delay
    }
}
