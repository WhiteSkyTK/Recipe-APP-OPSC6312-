package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FavoritesFragment : Fragment() {

    private lateinit var favoritesAdapter: HomeRecipeAdapter
    private lateinit var noFavoritesTextView: TextView

    private val viewModel: FavoritesViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(
            db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(),
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFavorites)
        noFavoritesTextView = view.findViewById(R.id.textViewNoFavorites)

        setupRecyclerView(recyclerView)
        observeViewModel()

        return view
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val onRecipeClicked = { recipe: Recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }

        favoritesAdapter = HomeRecipeAdapter(
            emptyList(),
            onRecipeClicked,
            onFavoriteClick = { recipe -> viewModel.toggleFavorite(recipe) },
            favoritesLiveData = viewModel.allFavorites,
            lifecycleOwner = viewLifecycleOwner
        )

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = favoritesAdapter
    }

    private fun observeViewModel() {
        viewModel.favoriteRecipes.observe(viewLifecycleOwner) { favorites ->
            if (favorites.isNullOrEmpty()) {
                noFavoritesTextView.visibility = View.VISIBLE
            } else {
                noFavoritesTextView.visibility = View.GONE
            }
            favoritesAdapter.updateData(favorites)
        }
    }
}

