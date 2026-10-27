import Foundation

/// Converts ingredient quantities between metric and imperial kitchen units.
/// Port of the Android UnitConverter, with the same fraction-friendly output.
enum UnitSystem: String, CaseIterable, Identifiable {
    case metric = "METRIC"
    case imperial = "IMPERIAL"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .metric: L("metric_grams_ml")
        case .imperial: L("imperial_ounces_cups")
        }
    }

    static func defaultFor(countryCode: String?) -> UnitSystem {
        guard let countryCode else { return .metric }
        return CountryCatalog.imperialCountries.contains(countryCode) ? .imperial : .metric
    }
}

enum UnitConverter {
    private static let fractionTolerance = 0.02
    private static let ouncesToGrams = 28.3495
    private static let poundsToGrams = 453.592
    private static let flOuncesToMl = 29.5735
    private static let tspToMl = 4.92892
    private static let tbspToMl = 14.7868
    private static let cupsToMl = 236.588
    private static let pintsToMl = 473.176
    private static let quartsToMl = 946.353
    private static let gallonsToMl = 3785.41

    private static let unicodeFractions: [(String, Double)] = [
        ("½", 0.5), ("⅓", 1.0 / 3), ("⅔", 2.0 / 3), ("¼", 0.25), ("¾", 0.75),
        ("⅛", 0.125), ("⅜", 0.375), ("⅝", 0.625), ("⅞", 0.875),
    ]

    /// Only common kitchen fractions are used for display.
    private static let displayFractions: [(Double, String)] = [
        (0.25, "¼"), (1.0 / 3, "⅓"), (0.5, "½"), (2.0 / 3, "⅔"), (0.75, "¾"),
    ]

    /// Converts a quantity + unit to the target system and formats it, e.g. ("2", "cups", .metric) -> "473.2 ml".
    /// Text that is not a number ("a pinch") is returned unchanged.
    static func convert(quantity: String, unit: String, to system: UnitSystem) -> String {
        let amount = parseQuantity(quantity)
        guard let amount else { return "\(quantity) \(unit)".trimmingCharacters(in: .whitespaces) }
        if amount == 0 { return quantity.isEmpty ? unit : "\(quantity) \(unit)".trimmingCharacters(in: .whitespaces) }
        return convert(amount: amount, unit: unit, to: system)
    }

    /// Converts an already-parsed amount (used when scaling servings).
    static func convert(amount: Double, unit: String, to system: UnitSystem) -> String {
        let converted: (Double, String)
        if let ml = toMilliliters(amount, unit) {
            converted = fromMilliliters(ml, system)
        } else if let grams = toGrams(amount, unit) {
            converted = fromGrams(grams, system)
        } else {
            converted = (amount, unit)
        }
        return format(amount: converted.0, unit: converted.1)
    }

    /// Parses "2", "0.5", "1/2", "1 1/2", "1½", "1-2" (averaged) and "1 to 2".
    /// Returns nil for text such as "pinch" or "to taste", and 0 for an empty string.
    static func parseQuantity(_ input: String?) -> Double? {
        guard var text = input?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), !text.isEmpty else {
            return 0
        }

        for separator in [" to ", "-", "–"] {
            let parts = text.components(separatedBy: separator)
            if parts.count == 2, !parts[0].trimmingCharacters(in: .whitespaces).isEmpty,
               let low = parseQuantity(parts[0]), let high = parseQuantity(parts[1]) {
                return (low + high) / 2
            }
        }

        // "1½" -> "1 ½" so the whole number and the fraction are summed, not concatenated.
        for (symbol, _) in unicodeFractions {
            text = text.replacingOccurrences(of: symbol, with: " \(symbol) ")
        }

        var total = 0.0
        var sawNumber = false
        for token in text.split(whereSeparator: { $0 == " " || $0 == "\u{00A0}" }) {
            let part = String(token)
            if let value = unicodeFractions.first(where: { $0.0 == part })?.1 {
                total += value
            } else if part.contains("/") {
                let pieces = part.split(separator: "/")
                guard pieces.count == 2, let numerator = Double(pieces[0]), let denominator = Double(pieces[1]),
                      denominator != 0 else { return nil }
                total += numerator / denominator
            } else if let value = Double(part.replacingOccurrences(of: ",", with: ".")) {
                total += value
            } else {
                return nil
            }
            sawNumber = true
        }
        return sawNumber ? total : nil
    }

    // MARK: - Base units

    private static func toMilliliters(_ amount: Double, _ unit: String) -> Double? {
        switch unit.lowercased().trimmingCharacters(in: .whitespaces) {
        case "ml", "milliliter", "milliliters", "millilitre", "millilitres": amount
        case "l", "liter", "liters", "litre", "litres": amount * 1000
        case "fl oz", "fluid ounce", "fluid ounces": amount * flOuncesToMl
        case "tsp", "teaspoon", "teaspoons", "tsps": amount * tspToMl
        case "tbsp", "tablespoon", "tablespoons", "tbsps", "tbs": amount * tbspToMl
        case "cup", "cups", "c": amount * cupsToMl
        case "pt", "pint", "pints": amount * pintsToMl
        case "qt", "quart", "quarts": amount * quartsToMl
        case "gal", "gallon", "gallons": amount * gallonsToMl
        default: nil
        }
    }

    private static func toGrams(_ amount: Double, _ unit: String) -> Double? {
        switch unit.lowercased().trimmingCharacters(in: .whitespaces) {
        case "g", "gram", "grams", "gr": amount
        case "kg", "kilogram", "kilograms": amount * 1000
        case "oz", "ounce", "ounces": amount * ouncesToGrams
        case "lb", "lbs", "pound", "pounds": amount * poundsToGrams
        default: nil
        }
    }

    private static func fromMilliliters(_ ml: Double, _ system: UnitSystem) -> (Double, String) {
        switch system {
        case .imperial:
            // The small tolerance stops rounding in the constants from turning exactly
            // 1 cup into "16 tbsp" or 1 tbsp into "3 tsp".
            let tolerance = 0.05
            if ml < tspToMl * 3 - tolerance { return (ml / tspToMl, "tsp") }
            if ml < tbspToMl * 16 - tolerance { return (ml / tbspToMl, "tbsp") }
            if ml < cupsToMl * 4 - tolerance { return (ml / cupsToMl, "cups") }
            if ml < quartsToMl - tolerance { return (ml / pintsToMl, "pints") }
            if ml < gallonsToMl - tolerance { return (ml / quartsToMl, "quarts") }
            return (ml / gallonsToMl, "gallons")
        case .metric:
            return ml >= 1000 ? (ml / 1000, "L") : (ml, "ml")
        }
    }

    private static func fromGrams(_ grams: Double, _ system: UnitSystem) -> (Double, String) {
        switch system {
        case .imperial:
            return grams < poundsToGrams ? (grams / ouncesToGrams, "oz") : (grams / poundsToGrams, "lbs")
        case .metric:
            return grams >= 1000 ? (grams / 1000, "kg") : (grams, "g")
        }
    }

    // MARK: - Formatting

    static func format(amount: Double, unit: String) -> String {
        let usesFractions = ["tsp", "tbsp", "cups", "oz"].contains(unit.lowercased()) && amount < 16
        let number = usesFractions ? fractionString(amount) : decimalString(amount)
        var shownUnit = unit
        // "1 cup", "½ cup", not "1 cups".
        if amount <= 1.005, ["cups", "pints", "quarts", "gallons", "lbs"].contains(unit) {
            shownUnit = unit == "lbs" ? "lb" : String(unit.dropLast())
        }
        return "\(number) \(shownUnit)".trimmingCharacters(in: .whitespaces)
    }

    /// One decimal place at most, no trailing ".0" ("2", "2.5").
    static func decimalString(_ value: Double) -> String {
        let rounded = (value * 10).rounded() / 10
        if rounded == rounded.rounded() { return String(Int(rounded)) }
        return String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), rounded)
    }

    /// 1.5 -> "1 ½", 0.333 -> "⅓"; falls back to a decimal when no kitchen fraction is close.
    static func fractionString(_ value: Double) -> String {
        let whole = Int(value)
        let remainder = value - Double(whole)
        if remainder < 0.01 { return String(whole) }
        if remainder > 0.99 { return String(whole + 1) }
        guard let closest = displayFractions.min(by: { abs($0.0 - remainder) < abs($1.0 - remainder) }),
              abs(closest.0 - remainder) < fractionTolerance else {
            return decimalString(value)
        }
        return whole == 0 ? closest.1 : "\(whole) \(closest.1)"
    }
}
