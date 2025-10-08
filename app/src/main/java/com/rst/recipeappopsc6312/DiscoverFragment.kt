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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DiscoverFragment : Fragment() {

    // Use the DiscoverViewModel for this screen's specific logic
    private val viewModel: DiscoverViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(
            db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(),
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
    }

    // Get the shared MainViewModel from the MainActivity to know the network status
    private val mainViewModel: MainViewModel by activityViewModels()

    // --- Views ---
    private lateinit var discoverAdapter: DiscoverRecipeAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var contentLayout: View
    private lateinit var offlineContainer: View
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)

        // Find all views
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewDiscover)
        val searchView = view.findViewById<SearchView>(R.id.searchViewRecipes)
        sortSpinner = view.findViewById(R.id.spinnerSort)
        contentLayout = view.findViewById(R.id.discoverContentLayout)
        offlineContainer = view.findViewById(R.id.offlineContainer)
        loadingOverlay = view.findViewById(R.id.loadingOverlayContainer)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutDiscover)

        setupRecyclerView(recyclerView)
        setupSortSpinner(sortSpinner)
        observeViewModel()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.search(query)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.setSortOption(sortSpinner.selectedItem.toString())
                }
                return false
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            // When user pulls to refresh, reset and reload the current sorted list
            viewModel.setSortOption(sortSpinner.selectedItem.toString())
        }

        return view
    }

    private fun setupSortSpinner(spinner: Spinner) {
        val sortOptions = listOf("A-Z", "Recommended", "Popular", "Cook Time", "Z-A")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setSortOption(sortOptions[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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

        // Add scroll listener for pagination.
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
        mainViewModel.networkStatus.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                offlineContainer.visibility = View.GONE
            } else {
                offlineContainer.visibility = View.VISIBLE
                contentLayout.visibility = View.GONE
                loadingOverlay.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.isInitiallyLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (!isLoading && mainViewModel.networkStatus.value == true) {
                contentLayout.animate().alpha(1f).duration = 300
                contentLayout.visibility = View.VISIBLE
            } else if (!isLoading) {
                contentLayout.visibility = View.GONE
            } else {
                contentLayout.visibility = View.INVISIBLE
                contentLayout.alpha = 0f
            }
        }

        viewModel.isFetchingMore.observe(viewLifecycleOwner) { isFetching ->
            // Update SwipeRefreshLayout based on this, NOT the initial loading state
            swipeRefreshLayout.isRefreshing = isFetching
        }

        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            discoverAdapter.updateData(recipes)
            swipeRefreshLayout.isRefreshing = false // Always stop refreshing when new data arrives
        }
    }
}
