package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeMatchAdapter(
    private var matches: List<RecipeMatch>,
    private val onClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeMatchAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageViewRecipe)
        val title: TextView = itemView.findViewById(R.id.textViewRecipeTitle)
        val missing: TextView = itemView.findViewById(R.id.textViewMissingIngredients)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_match, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val match = matches[position]
        holder.title.text = match.recipe.title
        Glide.with(holder.itemView.context).load(match.recipe.imageUrl).into(holder.image)

        val missingCount = match.missingIngredients.size
        holder.missing.text = when {
            missingCount == 0 -> "You have all ingredients! 🎉"
            missingCount == 1 -> "You need 1 more ingredient"
            else -> "You need $missingCount more ingredients"
        }

        holder.itemView.setOnClickListener { onClick(match.recipe) }
    }

    override fun getItemCount() = matches.size

    fun updateMatches(newMatches: List<RecipeMatch>) {
        this.matches = newMatches
        notifyDataSetChanged()
    }
}