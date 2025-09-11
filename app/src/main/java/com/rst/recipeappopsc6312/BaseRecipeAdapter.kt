package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// This is an abstract class, meaning it's a blueprint for other adapters.
abstract class BaseRecipeAdapter(
    protected var recipeList: List<Recipe>,
    protected val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<BaseRecipeAdapter.BaseViewHolder>() {

    // This ViewHolder is generic and can be used by all child adapters.
    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.imageViewRecipe)
        val recipeTitle: TextView = itemView.findViewById(R.id.textViewRecipeTitle)
        val recipeTime: TextView? = itemView.findViewById(R.id.textViewTime) // Optional
    }

    override fun getItemCount() = recipeList.size

    // Public function to update the data, usable by all child adapters.
    open fun updateData(newRecipes: List<Recipe>) {
        this.recipeList = newRecipes
        notifyDataSetChanged()
    }
}