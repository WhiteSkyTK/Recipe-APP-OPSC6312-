package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.bumptech.glide.Glide

// Pass the parameters directly to the BaseRecipeAdapter constructor
class DiscoverRecipeAdapter(
    recipeList: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit,
    private val favoritesLiveData: LiveData<List<Recipe>>,
    private val lifecycleOwner: LifecycleOwner
) : BaseRecipeAdapter(recipeList, onRecipeClick), Filterable {

    // This is the list that will be filtered by the search
    internal var recipeFilterList: List<Recipe> = recipeList

    // Special ViewHolder for this adapter that includes the favorite icon
    inner class DiscoverViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val favoriteIcon: ImageView = itemView.findViewById(R.id.imageViewFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_recommended, parent, false)
        return DiscoverViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val recipe = recipeFilterList[position]
        val discoverHolder = holder as DiscoverViewHolder

        discoverHolder.recipeTitle.text = recipe.title
        discoverHolder.recipeTime?.text = "${recipe.timeInMins} Min"
        Glide.with(discoverHolder.itemView.context).load(recipe.imageUrl).into(discoverHolder.recipeImage)

        favoritesLiveData.observe(lifecycleOwner) { favoritesList ->
            val isFavorited = favoritesList.any { it.id == recipe.id }
            val heartIcon = if (isFavorited) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            (holder as DiscoverViewHolder).favoriteIcon.setImageResource(heartIcon)
        }

        holder.itemView.setOnClickListener { onRecipeClick(recipe) }
        (holder as DiscoverViewHolder).favoriteIcon.setOnClickListener { onFavoriteClick(recipe) }
    }

    override fun getItemCount(): Int {
        return recipeFilterList.size
    }

    // This function updates both the original and filtered lists
    override fun updateData(newRecipes: List<Recipe>) {
        this.recipeList = newRecipes
        this.recipeFilterList = newRecipes
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                recipeFilterList = if (charSearch.isEmpty()) {
                    recipeList
                } else {
                    recipeList.filter {
                        it.title.lowercase().contains(charSearch.lowercase())
                    }
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