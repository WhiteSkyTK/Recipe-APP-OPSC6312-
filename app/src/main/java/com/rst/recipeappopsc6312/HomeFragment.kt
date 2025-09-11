package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private val viewModel: ShoppingViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        // Provide the missing FirebaseStorage instance as the fourth argument
        val repository = ShoppingRepository(
            database.shoppingDao(),
            database.recipeDao(),
            database.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        ShoppingViewModelFactory(repository)
    }
    private lateinit var featuredAdapter: FeaturedRecipeAdapter
    private lateinit var timeOfDayAdapter: HomeRecipeAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recommendedAdapter: HomeRecipeAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        setupRecyclerViews(view)
        observeViewModel(view)

        swipeRefreshLayout.setOnRefreshListener {
            // Tell the ViewModel to fetch fresh data
            viewModel.refreshHomeScreenData()
        }

        // Tell the ViewModel to start loading data
        viewModel.loadHomeScreenData()
        return view
    }

    override fun onResume() {
        super.onResume()
        // ++ Refresh the greeting every time the user returns to the screen ++
        val timeOfDayTitle = view?.findViewById<TextView>(R.id.textViewTimeOfDayTitle)
        timeOfDayTitle?.text = GreetingManager.getRandomGreetingForCurrentTime()
    }

    private fun setupRecyclerViews(view: View) {
        val onRecipeClicked = { recipe: Recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }

        val onFavoriteClicked = { recipe: Recipe -> viewModel.toggleFavorite(recipe) }

        // ++ UPDATE the adapter initializations
        timeOfDayAdapter = HomeRecipeAdapter(emptyList(), onRecipeClicked, onFavoriteClicked, viewModel.allFavorites, viewLifecycleOwner)
        recommendedAdapter = HomeRecipeAdapter(emptyList(), onRecipeClicked, onFavoriteClicked, viewModel.allFavorites, viewLifecycleOwner)
        // Initialize adapters with empty lists
        featuredAdapter = FeaturedRecipeAdapter(emptyList(), onRecipeClicked)
        categoryAdapter = CategoryAdapter(emptyList()) { }

        // Find RecyclerViews and set their layouts and adapters
        view.findViewById<RecyclerView>(R.id.recyclerViewFeatured).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
        }
        view.findViewById<RecyclerView>(R.id.recyclerViewTimeOfDay).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = timeOfDayAdapter
        }
        view.findViewById<RecyclerView>(R.id.recyclerViewCategories).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
        view.findViewById<RecyclerView>(R.id.recyclerViewRecommended).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = recommendedAdapter
        }
    }
    private fun observeViewModel(view: View) {
        val timeOfDayTitle = view.findViewById<TextView>(R.id.textViewTimeOfDayTitle)
        setupTimeOfDaySection(timeOfDayTitle)
        // Observer for Featured Recipes
        viewModel.featuredRecipes.observe(viewLifecycleOwner) { recipes ->
            featuredAdapter.updateData(recipes)
        }

        // Observer for Recommended Recipes (the main list to be filtered)
        viewModel.recommendedRecipes.observe(viewLifecycleOwner) { allRecipes ->
            recommendedAdapter.updateData(allRecipes)

            // The onCategoryClick lambda is now simpler
            categoryAdapter.onCategoryClick = { category ->
                val filteredList = if (category.name.equals("All", ignoreCase = true)) {
                    allRecipes
                } else {
                    allRecipes.filter { it.category.equals(category.name, ignoreCase = true) }
                }
                recommendedAdapter.updateData(filteredList)
            }
        }

        // Observer for Categories
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.updateData(categories)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // This assumes your ViewModel has an isLoading LiveData
            swipeRefreshLayout.isRefreshing = isLoading
        }

        setupTimeOfDaySection(timeOfDayTitle)
    }

    private fun setupTimeOfDaySection(titleView: TextView) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        titleView.text = GreetingManager.getRandomGreetingForCurrentTime()

        lifecycleScope.launch {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val recipeList = getRecipesForTimeOfDay(hour)
            Log.d("HomeFragment", "Time of Day section fetched ${recipeList.size} recipes.")
            if (recipeList.isNotEmpty()) {
                timeOfDayAdapter.updateData(recipeList)
            }
        }
    }

    private suspend fun getRecipesForTimeOfDay(hour: Int): List<Recipe> {
        return when (hour) {
            in 5..10 -> viewModel.repository.getBreakfastRecipes()
            in 11..13 -> viewModel.repository.getLunchRecipes()
            in 14..17 -> viewModel.repository.getSnackRecipes()
            in 18..21 -> viewModel.repository.getDinnerRecipes()
            else -> viewModel.repository.getSnackRecipes()
        }
    }
}