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
        ShoppingViewModelFactory(repository)
    }
    private val mainViewModel: MainViewModel by activityViewModels()
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
        observeViewModel()

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
    private fun observeViewModel() {
        mainViewModel.networkStatus.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                offlineContainer.visibility = View.GONE
                contentScrollView.visibility = View.VISIBLE
            } else {
                offlineContainer.visibility = View.VISIBLE
                contentScrollView.visibility = View.GONE
                loadingOverlay.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        shoppingViewModel.isInitiallyLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                contentScrollView.animate().alpha(1f).setDuration(300).start()
                contentScrollView.visibility = View.VISIBLE
            } else {
                contentScrollView.alpha = 0f
            }
        }

        shoppingViewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefreshLayout.isRefreshing = isRefreshing
        }

        shoppingViewModel.timeOfDayRecipes.observe(viewLifecycleOwner) { recipes -> timeOfDayAdapter.updateData(recipes) }
        shoppingViewModel.featuredRecipes.observe(viewLifecycleOwner) { recipes -> featuredAdapter.updateData(recipes) }
        shoppingViewModel.recommendedRecipes.observe(viewLifecycleOwner) { allRecipes ->
            recommendedAdapter.updateData(allRecipes)
            categoryAdapter.onCategoryClick = { category ->
                val filteredList = if (category.name.equals("All", ignoreCase = true)) allRecipes
                else allRecipes.filter { it.category.equals(category.name, ignoreCase = true) }
                recommendedAdapter.updateData(filteredList)
            }
        }
        shoppingViewModel.categories.observe(viewLifecycleOwner) { categories -> categoryAdapter.updateData(categories) }
    }
}