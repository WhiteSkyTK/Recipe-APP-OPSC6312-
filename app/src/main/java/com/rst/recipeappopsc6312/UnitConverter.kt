package com.rst.recipeappopsc6312

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object UnitConverter {
    // --- System Constants ---
    const val METRIC = "METRIC"
    const val IMPERIAL = "IMPERIAL"

    // --- Private Constants for Maintainability ---
    private const val FRACTION_TOLERANCE = 0.02
    private const val OUNCES_TO_GRAMS = 28.3495
    private const val POUNDS_TO_GRAMS = 453.592
    private const val FL_OUNCES_TO_ML = 29.5735
    private const val TSP_TO_ML = 4.92892
    private const val TBSP_TO_ML = 14.7868
    private const val CUPS_TO_ML = 236.588
    private const val PINTS_TO_ML = 473.176
    private const val QUARTS_TO_ML = 946.353
    private const val GALLONS_TO_ML = 3785.41

    // --- Fraction Maps for Parsing and Formatting ---
    private val stringToFractionMap = mapOf(
        "½" to 0.5, "1/2" to 0.5, "⅓" to 0.333, "1/3" to 0.333,
        "⅔" to 0.667, "2/3" to 0.667, "¼" to 0.25, "1/4" to 0.25,
        "¾" to 0.75, "3/4" to 0.75, "⅛" to 0.125, "1/8" to 0.125
    )

    // Only use common, "kitchen-friendly" fractions for display
    private val allowedFractions = mapOf(
        0.25 to "¼", 0.333 to "⅓", 0.5 to "½", 0.667 to "⅔", 0.75 to "¾"
    )

    /**
     * The main public function.
     */
    fun convert(quantityString: String, unit: String, targetSystem: String): String {
        val amount = parseQuantity(quantityString)
        if (amount.isNaN()) return quantityString
        if (amount == 0.0 && quantityString.isNotBlank()) return "$quantityString $unit".trim()
        if (amount == 0.0) return ""

        val (convertedAmount, convertedUnit) = when (targetSystem) {
            IMPERIAL -> convertToImperial(amount, unit)
            METRIC -> convertToMetric(amount, unit)
            else -> amount to unit
        }

        return formatAmount(convertedAmount, convertedUnit)
    }

    private fun convertToImperial(amount: Double, unit: String): Pair<Double, String> {
        val baseMl = toBaseVolume(amount, unit)
        val baseGrams = toBaseWeight(amount, unit)
        return when {
            baseMl != null -> fromBaseVolume(baseMl, IMPERIAL)
            baseGrams != null -> fromBaseWeight(baseGrams, IMPERIAL)
            else -> amount to unit
        }
    }

    private fun convertToMetric(amount: Double, unit: String): Pair<Double, String> {
        val baseMl = toBaseVolume(amount, unit)
        val baseGrams = toBaseWeight(amount, unit)
        return when {
            baseMl != null -> fromBaseVolume(baseMl, METRIC)
            baseGrams != null -> fromBaseWeight(baseGrams, METRIC)
            else -> amount to unit
        }
    }

    // --- Base Unit Conversion Helpers ---
    private fun toBaseVolume(amount: Double, unit: String): Double? {
        return when (unit.lowercase().trim()) {
            "ml", "milliliter", "milliliters" -> amount
            "l", "liter", "liters" -> amount * 1000
            "fl oz", "fluid ounce" -> amount * FL_OUNCES_TO_ML
            "tsp", "teaspoon", "teaspoons" -> amount * TSP_TO_ML
            "tbsp", "tablespoon", "tablespoons" -> amount * TBSP_TO_ML
            "cup", "cups" -> amount * CUPS_TO_ML
            "pt", "pint", "pints" -> amount * PINTS_TO_ML
            "qt", "quart", "quarts" -> amount * QUARTS_TO_ML
            "gal", "gallon", "gallons" -> amount * GALLONS_TO_ML
            else -> null
        }
    }

    private fun toBaseWeight(amount: Double, unit: String): Double? {
        return when (unit.lowercase().trim()) {
            "g", "gram", "grams" -> amount
            "kg", "kilogram", "kilograms" -> amount * 1000
            "oz", "ounce", "ounces" -> amount * OUNCES_TO_GRAMS
            "lb", "lbs", "pound", "pounds" -> amount * POUNDS_TO_GRAMS
            else -> null
        }
    }

    private fun fromBaseVolume(baseMl: Double, system: String): Pair<Double, String> {
        return if (system == IMPERIAL) {
            when {
                baseMl < TSP_TO_ML * 3 -> baseMl / TSP_TO_ML to "tsp"
                baseMl < TBSP_TO_ML * 16 -> baseMl / TBSP_TO_ML to "tbsp"
                baseMl < CUPS_TO_ML * 4 -> baseMl / CUPS_TO_ML to "cups"
                baseMl < QUARTS_TO_ML -> baseMl / PINTS_TO_ML to "pints"
                baseMl < GALLONS_TO_ML -> baseMl / QUARTS_TO_ML to "quarts"
                else -> baseMl / GALLONS_TO_ML to "gallons"
            }
        } else { // METRIC
            if (baseMl >= 1000) baseMl / 1000 to "L" else baseMl to "ml"
        }
    }

    private fun fromBaseWeight(baseGrams: Double, system: String): Pair<Double, String> {
        return if (system == IMPERIAL) {
            if (baseGrams < POUNDS_TO_GRAMS) baseGrams / OUNCES_TO_GRAMS to "oz" else baseGrams / POUNDS_TO_GRAMS to "lbs"
        } else { // METRIC
            if (baseGrams >= 1000) baseGrams / 1000 to "kg" else baseGrams to "g"
        }
    }

    /**
     * Formats the final amount, applying the hybrid rule for fractions vs. decimals.
     */
    private fun formatAmount(amount: Double, unit: String): String {
        val useFractions = when (unit.lowercase()) {
            "tsp", "tbsp", "cups", "oz" -> amount < 16 // Use fractions for smaller amounts of common kitchen units
            else -> false
        }

        val finalNumber = if (useFractions) {
            doubleToFractionString(amount)
        } else {
            val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))
            var formatted = df.format(amount)
            if (formatted.endsWith(".0")) {
                formatted = formatted.dropLast(2)
            }
            formatted
        }
        return "$finalNumber $unit".trim()
    }

    private fun doubleToFractionString(number: Double): String {
        val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))
        val wholePart = number.toInt()
        val decimalPart = number - wholePart

        val fraction = allowedFractions.minByOrNull { (value, _) ->
            abs(decimalPart - value)
        }?.takeIf { abs(decimalPart - it.key) < FRACTION_TOLERANCE }?.value ?: ""

        return when {
            abs(decimalPart) < 0.01 -> wholePart.toString()
            wholePart == 0 && fraction.isNotEmpty() -> fraction
            wholePart > 0 && fraction.isNotEmpty() -> "$wholePart $fraction"
            else -> df.format(number).let { if (it.endsWith(".0")) it.dropLast(2) else it }
        }
    }

    /**
     * This is our robust parser that understands fractions, ranges, and mixed numbers.
     */
    internal fun parseQuantity(input: String?): Double {
        if (input.isNullOrBlank()) return 0.0
        var normalized = input.trim().lowercase()

        // Handle ranges like "1-2" or "1 to 2" first
        val rangeParts = normalized.split("-", " to ")
        if (rangeParts.size == 2) {
            val first = parseQuantity(rangeParts[0])
            val second = parseQuantity(rangeParts[1])
            if (!first.isNaN() && !second.isNaN()) {
                return (first + second) / 2.0
            }
        }

        // Fix stuck fractions like "1½" → "1 ½" before replacing them
        normalized = normalized.replace(
            Regex("([0-9])([¼½¾⅓⅔⅛])")
        ) { match -> "${match.groupValues[1]} ${match.groupValues[2]}" }

        // Replace common fractions ("½" → 0.5)
        for ((fractionStr, value) in stringToFractionMap) {
            if (normalized.contains(fractionStr)) {
                normalized = normalized.replace(fractionStr, value.toString())
            }
        }

        // Handle mixed numbers like "1 0.5" or "1 1 0.5" by summing all parts
        if (normalized.contains(" ")) {
            val total = normalized.split(" ").sumOf { it.toDoubleOrNull() ?: 0.0 }
            if (total > 0) return total
        }

        // Handle non-numeric text like "pinch" or "to taste" AFTER all number parsing
        if (normalized.matches(Regex(".*[a-zA-Z].*"))) {
            return Double.NaN
        }

        return normalized.toDoubleOrNull() ?: 0.0
    }
}
