package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeAdapter(private val recipeList: List<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.imageViewRecipe)
        val recipeTitle: TextView = itemView.findViewById(R.id.textViewRecipeTitle)
        val recipeTime: TextView = itemView.findViewById(R.id.textViewTime)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.imageViewFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_recommended, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.recipeTitle.text = recipe.title
        holder.recipeTime.text = "${recipe.timeInMins} Min"

        // Use Glide to load the image from a URL
        Glide.with(holder.itemView.context)
            .load(recipe.imageUrl)
            .into(holder.recipeImage)

        // Set the heart icon based on favorite status
        val heartIcon = if (recipe.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        holder.favoriteIcon.setImageResource(heartIcon)

        // Handle clicks on the heart icon
        holder.favoriteIcon.setOnClickListener {
            recipe.isFavorite = !recipe.isFavorite
            notifyItemChanged(position)
            // TODO: Add logic to save the favorite status to Supabase/SQLite
        }
    }

    override fun getItemCount() = recipeList.size
}