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
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DiscoverFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var discoverAdapter: DiscoverRecipeAdapter
    private var recipeList = mutableListOf<Recipe>()
    private var isLoading = false
    private var currentPage = 0
    private val pageSize = 10 // Load 10 items at a time
    private lateinit var paginationProgressBar: ProgressBar


    private val viewModel: DiscoverViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance())
        DiscoverViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewDiscover)
        val searchView = view.findViewById<SearchView>(R.id.searchViewRecipes) // ++ USE CORRECT ID
        paginationProgressBar = view.findViewById(R.id.paginationProgressBar) // ++ FIND PROGRESS BAR

        setupRecyclerView(recyclerView)
        observeViewModel()


        // Add the search listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    // ++ LOG THE SEARCH QUERY ++
                    viewModel.repository.logSearchQuery(query)
                }
                discoverAdapter.filter.filter(query)
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // We don't log on every character change, only on submit.
                discoverAdapter.filter.filter(newText)
                return false
            }
        })
        return view
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        // Define what happens when a recipe is clicked
        val onRecipeClicked = { recipe: Recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }

        // Initialize the adapter correctly with all its listeners
        discoverAdapter = DiscoverRecipeAdapter(emptyList(), onRecipeClicked,
            onFavoriteClick = { recipe -> viewModel.toggleFavorite(recipe) },
            favoritesLiveData = viewModel.allFavorites,
            lifecycleOwner = viewLifecycleOwner
        )

        val layoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = discoverAdapter

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.loadMoreRecipes()
                }
            }
        })
    }


    private fun observeViewModel() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            // The updateData function in your new adapter will handle updating both lists
            discoverAdapter.updateData(recipes)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide the pagination loading indicator
            paginationProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

    }
}
