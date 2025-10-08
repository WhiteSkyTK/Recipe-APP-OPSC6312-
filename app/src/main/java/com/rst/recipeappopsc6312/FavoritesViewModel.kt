package com.rst.recipeappopsc6312

class FavoritesViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {
    // This is simple: it just exposes the LiveData of all favorite recipes
    // directly from the repository, which gets them from the local Room database.
    val favoriteRecipes = repository.getAllFavorites()
}

