package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.bumptech.glide.Glide

// It now takes two click listeners
class HomeRecipeAdapter(
    recipeList: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit,
    // It now takes the LiveData list of full Recipe objects
    private val favoritesLiveData: LiveData<List<Recipe>>,
    private val lifecycleOwner: LifecycleOwner
) : BaseRecipeAdapter(recipeList, onRecipeClick) {

    inner class HomeViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val favoriteIcon: ImageView = itemView.findViewById(R.id.imageViewFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_recommended, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val recipe = recipeList[position]
        val homeHolder = holder as HomeViewHolder // Cast to our special holder

        homeHolder.recipeTitle.text = recipe.title
        homeHolder.recipeTime?.text = "${recipe.timeInMins} Min"
        Glide.with(homeHolder.itemView.context).load(recipe.imageUrl).into(homeHolder.recipeImage)

        favoritesLiveData.observe(lifecycleOwner) { favoritesList ->
            val isFavorited = favoritesList.any { it.id == recipe.id }
            val heartIcon = if (isFavorited) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            (holder as HomeViewHolder).favoriteIcon.setImageResource(heartIcon)
        }

        holder.itemView.setOnClickListener { onRecipeClick(recipe) }
        (holder as HomeViewHolder).favoriteIcon.setOnClickListener { onFavoriteClick(recipe) }
    }
}