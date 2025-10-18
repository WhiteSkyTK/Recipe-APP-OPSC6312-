package com.rst.recipeappopsc6312

object DietData {
    fun getAllDiets(): List<Diet> {
        return listOf(
            Diet(R.string.diet_vegetarian, R.drawable.ic_vegetarian),
            Diet(R.string.diet_vegan, R.drawable.ic_vegan),
            Diet(R.string.diet_gluten_free, R.drawable.ic_gluten_free),
            Diet(R.string.diet_keto, R.drawable.ic_keto),
            Diet(R.string.diet_paleo, R.drawable.ic_paleo),
            Diet(R.string.diet_pescetarian, R.drawable.ic_pescetarian),
            Diet(R.string.diet_low_carb, R.drawable.ic_low_carb),
            Diet(R.string.diet_dairy_free, R.drawable.ic_dairy_free),
            Diet(R.string.diet_nut_allergy, R.drawable.ic_nut_allergy),
            Diet(R.string.diet_low_fodmap, R.drawable.ic_low_fodmap),
            Diet(R.string.diet_halal, R.drawable.ic_halal),
            Diet(R.string.diet_kosher, R.drawable.ic_kosher)
        )
    }
}
