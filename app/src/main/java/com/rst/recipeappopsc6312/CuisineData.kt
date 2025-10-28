package com.rst.recipeappopsc6312

object CuisineData {
    // This is now the single source of truth for all cuisines in the app.
    fun getAllCuisines(): List<Cuisine> {
        return listOf(
            Cuisine(R.string.cuisine_italian, R.drawable.ic_italian),
            Cuisine(R.string.cuisine_mexican, R.drawable.ic_mexican),
            Cuisine(R.string.cuisine_chinese, R.drawable.ic_chinese),
            Cuisine(R.string.cuisine_japanese, R.drawable.ic_japanese),
            Cuisine(R.string.cuisine_indian, R.drawable.ic_indian),
            Cuisine(R.string.cuisine_thai, R.drawable.ic_thai),
            Cuisine(R.string.cuisine_french, R.drawable.ic_french),
            Cuisine(R.string.cuisine_spanish, R.drawable.ic_spanish),
            Cuisine(R.string.cuisine_greek, R.drawable.ic_greek),
            Cuisine(R.string.cuisine_american, R.drawable.ic_american),
            Cuisine(R.string.cuisine_korean, R.drawable.ic_korean),
            Cuisine(R.string.cuisine_vietnamese, R.drawable.ic_vietnamese),
            Cuisine(R.string.cuisine_mediterranean, R.drawable.ic_mediterranean),
            Cuisine(R.string.cuisine_caribbean, R.drawable.ic_caribbean),
            Cuisine(R.string.cuisine_african, R.drawable.ic_african)
        )
    }
}

object CuisineHelper {
    // New function to get String names
    fun getAllCuisineNames(context: android.content.Context): List<String> {
        return CuisineData.getAllCuisines().map { context.getString(it.name) }
    }
}