package com.rst.recipeappopsc6312

import java.text.DecimalFormat

object UnitConverter {
    // Define conversion factors
    private const val GRAMS_TO_OUNCES = 0.035274
    private const val ML_TO_FL_OUNCES = 0.033814

    // Define the systems
    const val METRIC = "METRIC"
    const val IMPERIAL = "IMPERIAL"

    fun convert(amount: Double, unit: String, targetSystem: String): String {
        val df = DecimalFormat("#.##") // Format to two decimal places

        return when (targetSystem) {
            IMPERIAL -> convertToImperial(amount, unit, df)
            METRIC -> convertToMetric(amount, unit, df)
            else -> "${df.format(amount)} $unit" // Default to original if system is unknown
        }
    }

    private fun convertToImperial(amount: Double, unit: String, df: DecimalFormat): String {
        return when (unit.lowercase()) {
            "g", "grams" -> "${df.format(amount * GRAMS_TO_OUNCES)} oz"
            "ml", "milliliters" -> "${df.format(amount * ML_TO_FL_OUNCES)} fl oz"
            // Add more metric to imperial conversions here if needed
            else -> "${df.format(amount)} $unit" // Return original if no conversion is available
        }
    }

    private fun convertToMetric(amount: Double, unit: String, df: DecimalFormat): String {
        // Note: Spoonacular API data is already mostly metric-friendly,
        // but this is where you would convert from imperial if needed.
        // For now, we'll just format it.
        return "${df.format(amount)} $unit"
    }
}