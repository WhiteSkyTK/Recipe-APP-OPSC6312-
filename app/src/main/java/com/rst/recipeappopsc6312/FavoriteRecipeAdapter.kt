package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FavoriteRecipeAdapter(
    private var recipeList: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<FavoriteRecipeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.imageViewRecipe)
        val recipeTitle: TextView = itemView.findViewById(R.id.textViewRecipeTitle)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRecipeClick(recipeList[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Use the new layout file here
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.recipeTitle.text = recipe.title
        Glide.with(holder.itemView.context).load(recipe.imageUrl).into(holder.recipeImage)
    }

    override fun getItemCount() = recipeList.size

    fun updateData(newFavorites: List<Recipe>) {
        this.recipeList = newFavorites
        notifyDataSetChanged()
    }
}