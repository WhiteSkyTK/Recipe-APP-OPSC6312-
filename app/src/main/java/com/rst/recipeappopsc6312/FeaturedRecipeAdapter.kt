package com.rst.recipeappopsc6312

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FeaturedRecipeAdapter(
    private var recipeList: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<FeaturedRecipeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.imageViewRecipe)
        val recipeTitle: TextView = itemView.findViewById(R.id.textViewRecipeTitle)
        val recipeSubtitle: TextView = itemView.findViewById(R.id.textViewSubtitle)
        val recipeTime: TextView = itemView.findViewById(R.id.textViewTime)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRecipeClick(recipeList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_featured, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.recipeTitle.text = recipe.title

        // This adapter sets the time
        holder.recipeTime.text = "${recipe.timeInMins} Min"

        // This adapter calculates and sets the short description
        val titleLength = recipe.title.length
        val totalCharLimit = 80
        val remainingChars = totalCharLimit - titleLength
        val descriptionLimit = if (remainingChars < 20) 20 else remainingChars
        val shortDescription = if (recipe.description.length > descriptionLimit) {
            recipe.description.substring(0, descriptionLimit) + "..."
        } else {
            recipe.description
        }
        holder.recipeSubtitle.text = shortDescription

        Glide.with(holder.itemView.context).load(recipe.imageUrl).into(holder.recipeImage)
    }

    override fun getItemCount() = recipeList.size

    fun updateData(newRecipes: List<Recipe>) {
        this.recipeList = newRecipes
        notifyDataSetChanged()
    }
}