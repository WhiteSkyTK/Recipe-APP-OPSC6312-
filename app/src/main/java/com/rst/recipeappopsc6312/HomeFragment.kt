package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

    private val shoppingViewModel: ShoppingViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        // Provide the missing FirebaseStorage instance as the fourth argument
        val repository = ShoppingRepository(
            database.shoppingDao(),
            database.recipeDao(),
            database.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        ViewModelFactory(repository)
    }
    private val mainViewModel: MainViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ShoppingRepository(
            database.shoppingDao(), database.recipeDao(), database.scanHistoryDao(),
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance()
        )
        ViewModelFactory(repository)
    }
    private lateinit var featuredAdapter: FeaturedRecipeAdapter
    private lateinit var timeOfDayAdapter: HomeRecipeAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recommendedAdapter: HomeRecipeAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var contentScrollView: View
    private lateinit var offlineContainer: View
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        contentScrollView = view.findViewById(R.id.contentScrollView)
        offlineContainer = view.findViewById(R.id.offlineContainer)
        loadingOverlay = view.findViewById(R.id.loadingOverlayContainer)

        setupRecyclerViews(view)
        observeViewModel(view)

        swipeRefreshLayout.setOnRefreshListener {
            shoppingViewModel.refreshHomeScreenData()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.textViewTimeOfDayTitle)?.text = GreetingManager.getRandomGreetingForCurrentTime()
    }

    private fun setupRecyclerViews(view: View) {
        val onRecipeClicked = { recipe: Recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }
        val onFavoriteClicked = { recipe: Recipe -> shoppingViewModel.toggleFavorite(recipe) }

        timeOfDayAdapter = HomeRecipeAdapter(emptyList(), onRecipeClicked, onFavoriteClicked, shoppingViewModel.allFavorites, viewLifecycleOwner)
        recommendedAdapter = HomeRecipeAdapter(emptyList(), onRecipeClicked, onFavoriteClicked, shoppingViewModel.allFavorites, viewLifecycleOwner)
        featuredAdapter = FeaturedRecipeAdapter(emptyList(), onRecipeClicked)
        categoryAdapter = CategoryAdapter(emptyList()) { }

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

        mainViewModel.networkStatus.observe(viewLifecycleOwner) { isConnected ->
            contentScrollView.visibility = if (isConnected) View.VISIBLE else View.GONE
            offlineContainer.visibility = if (isConnected) View.GONE else View.VISIBLE
            swipeRefreshLayout.isEnabled = isConnected
        }

        shoppingViewModel.isInitiallyLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                contentScrollView.alpha = 0f
                contentScrollView.animate().alpha(1f).setDuration(500).start()
            }
        }

        shoppingViewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefreshLayout.isRefreshing = isRefreshing
        }

        // --- Data Observers ---
        shoppingViewModel.timeOfDayTitle.observe(viewLifecycleOwner) { title -> timeOfDayTitle.text = title }
        shoppingViewModel.timeOfDayRecipes.observe(viewLifecycleOwner) { recipes -> timeOfDayAdapter.updateData(recipes) }
        shoppingViewModel.featuredRecipes.observe(viewLifecycleOwner) { recipes -> featuredAdapter.updateData(recipes) }

        // ++ THIS IS THE NEW LOGIC ++
        // Observer for Recommended Recipes (the list that changes)
        shoppingViewModel.recommendedRecipes.observe(viewLifecycleOwner) { recipes ->
            recommendedAdapter.updateData(recipes)
        }

        // Observer for Categories (the list that triggers the change)
        shoppingViewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.updateData(categories)
            // The onCategoryClick lambda now tells the ViewModel to fetch new, filtered data.
            categoryAdapter.onCategoryClick = { category ->
                shoppingViewModel.onCategorySelected(category.name)
            }
        }
    }
}