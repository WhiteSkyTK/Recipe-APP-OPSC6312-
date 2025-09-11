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
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DiscoverFragment : Fragment() {

    private lateinit var discoverAdapter: DiscoverRecipeAdapter
    private lateinit var mainProgressBar: ProgressBar
    private lateinit var sortSpinner: Spinner

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

   // private val shoppingViewModel: ShoppingViewModel by viewModels {
     //   val db = AppDatabase.getDatabase(requireContext())
       // val repo = ShoppingRepository(db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(), FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
        //ShoppingViewModelFactory(repo)
    //}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewDiscover)
        val searchView = view.findViewById<SearchView>(R.id.searchViewRecipes) // ++ USE CORRECT ID
        val sortSpinner = view.findViewById<Spinner>(R.id.spinnerSort)
        mainProgressBar = view.findViewById(R.id.mainProgressBar)

        setupRecyclerView(recyclerView)
        setupSortSpinner(sortSpinner)
        observeViewModel()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.search(query)
                }
                searchView.clearFocus() // Hide the keyboard
                return true // Consume the event
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    // If the search bar is cleared, go back to the default sorted list.
                    viewModel.setSortOption(sortSpinner.selectedItem.toString())
                }
                return false
            }
        })

        return view
    }

    //override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      //  super.onViewCreated(view, savedInstanceState)

        //viewModel.loadDiscoverData()
    //}

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

        // Add scroll listener for pagination
        //recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
          //  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            //    super.onScrolled(recyclerView, dx, dy)
              //  if (!recyclerView.canScrollVertically(1)) {
                 //   viewModel.loadMoreRecipes()
                //}
            //}
        //})
    }


    private fun observeViewModel() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            discoverAdapter.updateData(recipes)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // This now controls the main, centered progress bar
            mainProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}
