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

    private val viewModel: FavoritesViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance())
        FavoritesViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewFavorites)

        // Use the same RecipeAdapter you use elsewhere
        val favoriteAdapter = FavoriteRecipeAdapter(emptyList()) { recipe ->
            val intent = Intent(activity, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = favoriteAdapter

        // Observe the list of favorite recipes from the ViewModel
        viewModel.favoriteRecipes.observe(viewLifecycleOwner) { favorites ->
            favoriteAdapter.updateData(favorites)
        }

        return view
    }
}