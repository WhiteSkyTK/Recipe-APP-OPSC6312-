package com.rst.recipeappopsc6312

import java.text.DecimalFormat

object UnitConverter {
    // Define conversion factors as constants for clarity
    private const val GRAMS_TO_OUNCES = 0.035274
    private const val ML_TO_FL_OUNCES = 0.033814

    const val METRIC = "METRIC"
    const val IMPERIAL = "IMPERIAL"

    fun convert(quantityString: String, unit: String, targetSystem: String): String {
        val amount = parseQuantity(quantityString)
        val df = DecimalFormat("#.##")

        val (convertedAmount, convertedUnit) = when (targetSystem) {
            IMPERIAL -> convertToImperial(amount, unit)
            else -> amount to unit
        }

        return "${df.format(convertedAmount)} $convertedUnit".trim()
    }

    private fun convertToImperial(amount: Double, unit: String): Pair<Double, String> {
        return when (unit.lowercase()) {
            // Use the named constants
            "g", "grams" -> (amount * GRAMS_TO_OUNCES) to "oz"
            "ml", "milliliters" -> (amount * ML_TO_FL_OUNCES) to "fl oz"
            else -> amount to unit
        }
    }

    internal fun parseQuantity(input: String?): Double {
        if (input.isNullOrBlank()) return 0.0
        if (input.contains(" ")) {
            val parts = input.split(" ")
            if (parts.size == 2) {
                val whole = parts[0].toDoubleOrNull() ?: 0.0
                val fraction = parseQuantity(parts[1])
                return whole + fraction
            }
        }
        return when (input) {
            "½", "1/2" -> 0.5
            "⅓", "1/3" -> 0.333
            "⅔", "2/3" -> 0.667
            "¼", "1/4" -> 0.25
            "¾", "3/4" -> 0.75
            "⅛", "1/8" -> 0.125
            "⅜", "3/8" -> 0.375
            "⅝", "5/8" -> 0.625
            "⅞", "7/8" -> 0.875
            else -> input.toDoubleOrNull() ?: 0.0
        }
    }
}
