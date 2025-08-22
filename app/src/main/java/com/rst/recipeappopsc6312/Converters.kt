package com.rst.recipeappopsc6312

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IngredientListConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromIngredientList(ingredients: List<Ingredient>?): String? {
        return ingredients?.let { gson.toJson(it) }
    }
    @TypeConverter
    fun toIngredientList(ingredientsString: String?): List<Ingredient>? {
        return ingredientsString?.let {
            val listType = object : TypeToken<List<Ingredient>>() {}.type
            gson.fromJson(it, listType)
        }
    }
}

class MethodStepListConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromMethodStepList(method: List<MethodStep>?): String? {
        return method?.let { gson.toJson(it) }
    }
    @TypeConverter
    fun toMethodStepList(methodString: String?): List<MethodStep>? {
        return methodString?.let {
            val listType = object : TypeToken<List<MethodStep>>() {}.type
            gson.fromJson(it, listType)
        }
    }
}

class NutritionFactListConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromNutritionFactList(nutrition: List<NutritionFact>?): String? {
        return nutrition?.let { gson.toJson(it) }
    }
    @TypeConverter
    fun toNutritionFactList(nutritionString: String?): List<NutritionFact>? {
        return nutritionString?.let {
            val listType = object : TypeToken<List<NutritionFact>>() {}.type
            gson.fromJson(it, listType)
        }
    }
}

