package com.rst.recipeappopsc6312

import java.util.Calendar
import java.util.Date

/**
 * Decides whether a recipe fits a user's diets. A recipe must satisfy every selected diet.
 *
 * Recipes imported from Spoonacular ("sp_...") or Tasty ("tasty_...") carry diet flags set by
 * those services; they need the flag *and* must pass the keyword screen. Recipes written by users
 * have no flags, so the keyword screen alone decides for them. (Previously a flag-only Firestore
 * query hid every user recipe from anyone with a diet, and Discover's
 * whereArrayContainsAny("dietTags") matched *any* diet and nothing for Halal, Kosher, Nut Allergy,
 * Pescetarian or Low-Carb, which never appear in tags.)
 */
object DietFilter {
    fun isImported(recipe: Recipe) = recipe.id.startsWith("sp_") || recipe.id.startsWith("tasty_")

    fun matches(recipe: Recipe, diets: List<String>): Boolean {
        if (diets.isEmpty()) return true
        val names = recipe.ingredients.map { it.name.lowercase() }
        val title = recipe.title.lowercase()

        fun ingredientsContain(keywords: Set<String>) = names.any { name -> keywords.any { containsWord(name, it) } }
        fun titleContains(keywords: Set<String>) = keywords.any { containsWord(title, it) }
        fun freeOf(keywords: Set<String>) = !ingredientsContain(keywords) && !titleContains(keywords)
        val imported = isImported(recipe)
        fun passes(flag: Boolean, keywordsOk: Boolean) = if (imported) flag && keywordsOk else keywordsOk

        return diets.all { diet ->
            when (diet) {
                "Vegan" -> passes(
                    recipe.isVegan,
                    freeOf(DietaryKeywords.meatKeywords + DietaryKeywords.seafoodKeywords + DietaryKeywords.dairyKeywords + DietaryKeywords.eggKeywords)
                )
                "Vegetarian" -> passes(
                    recipe.isVegetarian || recipe.isVegan,
                    freeOf(DietaryKeywords.meatKeywords + DietaryKeywords.seafoodKeywords)
                )
                "Gluten-Free" -> passes(recipe.isGlutenFree, !ingredientsContain(DietaryKeywords.glutenKeywords))
                "Dairy-Free" -> passes(recipe.isDairyFree || recipe.isVegan, !ingredientsContain(DietaryKeywords.dairyKeywords))
                "Keto" -> passes(recipe.isKeto, !ingredientsContain(DietaryKeywords.highCarbKeywords))
                "Paleo" -> passes(recipe.isPaleo, !ingredientsContain(DietaryKeywords.nonPaleoKeywords))
                "Low-FODMAP" -> passes(recipe.isLowFodmap, !ingredientsContain(DietaryKeywords.highFodmapKeywords))
                "Pescetarian" -> freeOf(DietaryKeywords.meatKeywords)
                "Nut Allergy" -> freeOf(DietaryKeywords.nutKeywords)
                "Halal" -> freeOf(DietaryKeywords.haramKeywords)
                "Kosher" -> !ingredientsContain(DietaryKeywords.nonKosherKeywords) &&
                    !(ingredientsContain(DietaryKeywords.meatKeywords) && ingredientsContain(DietaryKeywords.dairyKeywords))
                "Low-Carb" -> !ingredientsContain(DietaryKeywords.highCarbKeywords)
                else -> true
            }
        }
    }

    fun filter(recipes: List<Recipe>, diets: List<String>): List<Recipe> =
        if (diets.isEmpty()) recipes else recipes.filter { matches(it, diets) }

    /**
     * Whole-word match, so "ham" does not match "graham" and "egg" does not match "eggplant".
     * Plural "s"/"es" endings still match ("eggs" contains "egg").
     */
    fun containsWord(text: String, keyword: String): Boolean {
        var start = 0
        while (true) {
            val index = text.indexOf(keyword, start, ignoreCase = true)
            if (index < 0) return false
            val beforeOk = index == 0 || !text[index - 1].isLetter()
            var after = index + keyword.length
            if (after < text.length && text[after] == 's') {
                after++
            } else if (text.startsWith("es", after)) {
                after += 2
            }
            val afterOk = after >= text.length || !text[after].isLetter()
            if (beforeOk && afterOk) return true
            start = index + 1
        }
    }
}

/**
 * Matches what the user has against recipe ingredients (the Scan feature). Matching is by word,
 * so "chicken" covers "chicken breast". A recipe is listed only if the user has at least one of
 * its ingredients and is missing at most [maxMissing]. (Previously names had to be identical and
 * recipes the user had nothing for were listed too.)
 */
object IngredientMatcher {
    fun matches(userIngredients: List<String>, recipes: List<Recipe>, maxMissing: Int = 5): List<RecipeMatch> {
        val have = userIngredients.map { normalize(it) }.filter { it.isNotEmpty() }
        if (have.isEmpty()) return emptyList()

        return recipes.mapNotNull { recipe ->
            val needed = recipe.ingredients.map { normalize(it.name) }.filter { it.isNotEmpty() }.distinct()
            if (needed.isEmpty()) return@mapNotNull null
            val missing = needed.filter { name -> have.none { userHas(it, name) } }
            if (missing.size < needed.size && missing.size <= maxMissing) RecipeMatch(recipe, missing) else null
        }.sortedWith(compareBy({ it.missingIngredients.size }, { it.recipe.title }))
    }

    fun userHas(userItem: String, recipeIngredient: String): Boolean {
        val item = normalize(userItem)
        val ingredient = normalize(recipeIngredient)
        if (item.isEmpty() || ingredient.isEmpty()) return false
        if (item == ingredient) return true
        return DietFilter.containsWord(ingredient, singular(item)) || DietFilter.containsWord(item, singular(ingredient))
    }

    fun normalize(text: String) = text.trim().trim { !it.isLetterOrDigit() }.lowercase()

    private fun singular(word: String): String = when {
        word.endsWith("oes") -> word.dropLast(2)
        word.endsWith("ies") -> word.dropLast(3) + "y"
        word.endsWith("s") && !word.endsWith("ss") && word.length > 3 -> word.dropLast(1)
        else -> word
    }
}

/** Daily login streak in calendar days. (Comparing day-of-year numbers reset it every 1 January.) */
object LoginStreak {
    fun next(current: Int, lastLogin: Date?, now: Date = Date()): Int {
        if (lastLogin == null) return 1
        return when (daysBetween(lastLogin, now)) {
            0L -> maxOf(current, 1)
            1L -> current + 1
            else -> 1
        }
    }

    fun daysBetween(from: Date, to: Date): Long =
        Math.round((startOfDay(to) - startOfDay(from)) / 86_400_000.0)

    private fun startOfDay(date: Date): Long = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
