package com.rst.recipeappopsc6312

object CuisineData {
    // This is now the single source of truth for all cuisines in the app.
    fun getAllCuisines(): List<Cuisine> {
        return listOf(
            Cuisine("Italian", R.drawable.ic_italian),
            Cuisine("Mexican", R.drawable.ic_mexican),
            Cuisine("Chinese", R.drawable.ic_chinese),
            Cuisine("Japanese", R.drawable.ic_japanese),
            Cuisine("Indian", R.drawable.ic_indian),
            Cuisine("Thai", R.drawable.ic_thai),
            Cuisine("French", R.drawable.ic_french),
            Cuisine("Spanish", R.drawable.ic_spanish),
            Cuisine("Greek", R.drawable.ic_greek),
            Cuisine("American", R.drawable.ic_american),
            Cuisine("Korean", R.drawable.ic_korean),
            Cuisine("Vietnamese", R.drawable.ic_vietnamese),
            Cuisine("Mediterranean", R.drawable.ic_mediterranean),
            Cuisine("Caribbean", R.drawable.ic_caribbean),
            Cuisine("African", R.drawable.ic_african)
        )
    }
}
