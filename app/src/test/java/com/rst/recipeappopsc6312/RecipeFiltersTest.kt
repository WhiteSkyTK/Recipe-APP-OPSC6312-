package com.rst.recipeappopsc6312

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class RecipeFiltersTest {

    private fun recipe(id: String, vararg ingredients: String, title: String = "Recipe", isVegan: Boolean = false) =
        Recipe(
            id = id,
            title = title,
            ingredients = ingredients.map { Ingredient(name = it, quantity = "1", unit = "") },
            isVegan = isVegan
        )

    @Test
    fun wholeWordMatching() {
        assertFalse(DietFilter.containsWord("graham crackers", "ham"))
        assertFalse(DietFilter.containsWord("eggplant", "egg"))
        assertTrue(DietFilter.containsWord("2 eggs", "egg"))
        assertTrue(DietFilter.containsWord("tomatoes", "tomato"))
    }

    @Test
    fun keywordDiets() {
        assertFalse(DietFilter.matches(recipe("u1", "Bacon", "Eggs"), listOf("Halal")))
        assertFalse(DietFilter.matches(recipe("u2", "Beef mince", "Cheddar cheese"), listOf("Kosher")))
        assertTrue(DietFilter.matches(recipe("u3", "Lettuce", "Tomato"), listOf("Halal", "Kosher", "Nut Allergy")))
    }

    @Test
    fun userRecipesAreJudgedByIngredients() {
        assertTrue(DietFilter.matches(recipe("user-recipe", "Rice", "Beans"), listOf("Vegetarian")))
        assertFalse(DietFilter.matches(recipe("user-stew", "Chicken thighs"), listOf("Vegetarian")))
    }

    @Test
    fun importedRecipesNeedTheirFlag() {
        assertFalse(DietFilter.matches(recipe("sp_1", "Rice"), listOf("Vegetarian")))
        assertTrue(DietFilter.matches(recipe("sp_2", "Tofu", isVegan = true), listOf("Vegan", "Vegetarian")))
    }

    @Test
    fun ingredientMatching() {
        assertTrue(IngredientMatcher.userHas("chicken", "chicken breast"))
        assertTrue(IngredientMatcher.userHas("Tomatoes", "tomato"))
        assertFalse(IngredientMatcher.userHas("rice", "licorice"))

        val recipes = listOf(
            recipe("r1", "Chicken breast", "Rice", "Salt", title = "B"),
            recipe("r2", "Beef", "Onion", title = "C"),
            recipe("r3", "Chicken", "Rice", title = "A")
        )
        val matches = IngredientMatcher.matches(listOf("chicken", "rice"), recipes)
        assertEquals(listOf("r3", "r1"), matches.map { it.recipe.id })
        assertEquals(listOf("salt"), matches.last().missingIngredients)
    }

    private fun date(year: Int, month: Int, day: Int, hour: Int): Date =
        Calendar.getInstance().apply { set(year, month - 1, day, hour, 0, 0) }.time

    @Test
    fun loginStreakContinuesAcrossNewYear() {
        assertEquals(4, LoginStreak.next(3, date(2025, 12, 31, 22), date(2026, 1, 1, 8)))
        assertEquals(4, LoginStreak.next(4, date(2026, 3, 2, 7), date(2026, 3, 2, 21)))
        assertEquals(1, LoginStreak.next(9, date(2026, 3, 1, 7), date(2026, 3, 4, 7)))
        assertEquals(1, LoginStreak.next(0, null))
    }

    @Test
    fun stuckFractionsParse() {
        assertEquals(1.5, UnitConverter.parseQuantity("1½"), 0.0001)
        assertEquals(0.5, UnitConverter.parseQuantity("1/2"), 0.0001)
    }
}
