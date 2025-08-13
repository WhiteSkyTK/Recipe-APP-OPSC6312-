package com.rst.recipeappopsc6312

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeAdapter(
    private val recipeList: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit // Listener for item clicks
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>(), Filterable {

    var recipeFilterList: List<Recipe> = recipeList

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.imageViewRecipe)
        val recipeTitle: TextView = itemView.findViewById(R.id.textViewRecipeTitle)
        val recipeTime: TextView = itemView.findViewById(R.id.textViewTime)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.imageViewFavorite)

        init {
            // Set a click listener on the entire card view
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Call the lambda function, passing the clicked recipe
                    onRecipeClick(recipeFilterList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_recommended, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // CORRECTED: Bind data from the filtered list
        val recipe = recipeFilterList[position]
        val context = holder.itemView.context

        // Check favorite status from local storage
        recipe.isFavorite = FavoritesManager.isFavorite(context, recipe.id)

        holder.recipeTitle.text = recipe.title
        holder.recipeTime.text = "${recipe.timeInMins} Min"
        Glide.with(context).load(recipe.imageUrl).into(holder.recipeImage)

        val heartIcon = if (recipe.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        holder.favoriteIcon.setImageResource(heartIcon)

        holder.favoriteIcon.setOnClickListener {
            FavoritesManager.toggleFavorite(context, recipe.id)
            recipe.isFavorite = !recipe.isFavorite
            notifyItemChanged(position)
        }
    }

    // CORRECTED: Return the size of the filtered list
    override fun getItemCount() = recipeFilterList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                recipeFilterList = if (charSearch.isEmpty()) {
                    recipeList
                } else {
                    val resultList = ArrayList<Recipe>()
                    for (row in recipeList) {
                        if (row.title.lowercase().contains(charSearch.lowercase())) {
                            resultList.add(row)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = recipeFilterList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                recipeFilterList = results?.values as? List<Recipe> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}