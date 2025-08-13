package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFavorites)

        // In a real app, you would fetch the user's favorited recipes from Firestore
        // where the 'isFavorite' field is true.

        // TODO: Set up RecyclerView with a RecipeAdapter and the fetched data
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        return view
    }
}