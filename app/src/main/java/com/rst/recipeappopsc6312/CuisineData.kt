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
    // English names: they are stored on the user profile and compared with recipe data.
    fun getAllCuisineNames(context: android.content.Context): List<String> {
        return CuisineData.getAllCuisines().map { context.englishString(it.name) }
    }
}

/**
 * The English text of a string resource, whatever language the app is shown in.
 * Diet and cuisine preferences used to be saved in the display language, so in Sepedi,
 * Xitsonga or Tshivenda the filters (which compare against "Vegan", "Gluten-Free", ...) never matched.
 */
fun android.content.Context.englishString(resId: Int): String {
    val config = android.content.res.Configuration(resources.configuration)
    config.setLocale(java.util.Locale.ENGLISH)
    return createConfigurationContext(config).getString(resId)
}