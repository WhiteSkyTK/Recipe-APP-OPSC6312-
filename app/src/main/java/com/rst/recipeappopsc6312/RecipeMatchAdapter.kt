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
    private val onClick: (RecipeMatch) -> Unit
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
        val context = holder.itemView.context
        holder.title.text = match.recipe.title
        Glide.with(context).load(match.recipe.imageUrl).into(holder.image)

        val missingCount = match.missingIngredients.size
        holder.missing.text = when {
            missingCount == 0 -> context.getString(R.string.recipe_match_all_ingredients)
            else -> context.resources.getQuantityString(R.plurals.recipe_match_missing_ingredients, missingCount, missingCount)
        }

        holder.itemView.setOnClickListener { onClick(match) }
    }

    override fun getItemCount() = matches.size

    fun updateMatches(newMatches: List<RecipeMatch>) {
        this.matches = newMatches
        notifyDataSetChanged()
    }
}