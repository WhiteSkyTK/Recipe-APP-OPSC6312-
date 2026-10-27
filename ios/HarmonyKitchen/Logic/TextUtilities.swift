import Foundation

enum Validators {
    static func isValidEmail(_ email: String) -> Bool {
        let pattern = #"^[A-Z0-9a-z._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$"#
        return email.range(of: pattern, options: .regularExpression) != nil
    }

    /// Returns the localization key of the first rule the password breaks, or nil if it is valid.
    /// Same rules as the Android sign-up screen.
    static func passwordProblem(_ password: String, minLength: Int = 8) -> String? {
        if password.isEmpty { return "validation_password_empty" }
        if password.count < minLength { return "validation_password_min_length" }
        if !password.contains(where: \.isUppercase) { return "validation_password_no_uppercase" }
        if !password.contains(where: \.isLowercase) { return "validation_password_no_lowercase" }
        if !password.contains(where: \.isNumber) { return "validation_password_no_digit" }
        if !password.contains(where: { !$0.isLetter && !$0.isNumber }) { return "validation_password_no_special" }
        return nil
    }

    /// Builds an E.164 number ("+27821234567") from a dial code and what the user typed,
    /// or nil if it cannot be a valid phone number.
    static func e164(dialCode: String, national: String) -> String? {
        let codeDigits = dialCode.filter(\.isNumber)
        var digits = national.filter(\.isNumber)
        // A typed international prefix: "+27 82..." or "0027 82..."
        if national.trimmingCharacters(in: .whitespaces).hasPrefix("+") {
            guard digits.hasPrefix(codeDigits) else { return nil }
            digits.removeFirst(codeDigits.count)
        } else if digits.hasPrefix("00" + codeDigits) {
            digits.removeFirst(codeDigits.count + 2)
        }
        // Drop the national trunk prefix ("082 123 4567" -> "821234567"), except in Italy.
        if digits.hasPrefix("0"), codeDigits != "39" { digits.removeFirst() }
        let total = codeDigits.count + digits.count
        guard !codeDigits.isEmpty, digits.count >= 6, total <= 15 else { return nil }
        return "+\(codeDigits)\(digits)"
    }
}

enum TextCleaner {
    /// Turns Spoonacular's HTML summary into plain text.
    static func plainText(fromHTML html: String) -> String {
        var text = html.replacingOccurrences(of: "<br\\s*/?>", with: "\n", options: .regularExpression)
        text = text.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        let entities = [
            "&amp;": "&", "&quot;": "\"", "&#39;": "'", "&apos;": "'",
            "&lt;": "<", "&gt;": ">", "&nbsp;": " ",
        ]
        for (entity, value) in entities {
            text = text.replacingOccurrences(of: entity, with: value)
        }
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// "chicken BREAST" -> "Chicken BREAST" (only the first letter changes, like Kotlin's replaceFirstChar).
    static func capitalizedFirst(_ text: String) -> String {
        guard let first = text.first else { return text }
        return first.uppercased() + text.dropFirst()
    }
}

/// Greetings that depend on the time of day.
enum Greeting {
    /// The large header on the home screen ("☀️ Good Morning").
    static func header(hour: Int = Calendar.current.component(.hour, from: Date())) -> String {
        switch hour {
        case 2...11: L("main_greeting_morning")
        case 12...17: L("main_greeting_afternoon")
        default: L("main_greeting_evening")
        }
    }

    /// A random line for the time-of-day recipe row ("Rise and shine! 🌅").
    static func randomLine(hour: Int = Calendar.current.component(.hour, from: Date())) -> String {
        let array: String
        switch hour {
        case 5...10: array = "morning_greetings"
        case 11...13: array = "lunch_greetings"
        case 14...17: array = "afternoon_greetings"
        case 18...21: array = "dinner_greetings"
        default: array = "night_greetings"
        }
        return LocalizedArray.randomItem(array)
    }

    /// Formats the user's name the way the Android header did (ALL CAPS or Title Case).
    static func displayName(_ name: String, allCaps: Bool) -> String {
        if allCaps { return name.uppercased() }
        return name.split(separator: " ").map { $0.lowercased().capitalized }.joined(separator: " ")
    }
}

/// Scales ingredient amounts and nutrition values when the user changes the servings.
enum ServingsScaler {
    /// Display text for an ingredient at `ratio` (new servings / original servings), in `system`.
    static func ingredientText(_ ingredient: Ingredient, ratio: Double, system: UnitSystem) -> String {
        guard let amount = UnitConverter.parseQuantity(ingredient.quantity), amount > 0, ratio.isFinite, ratio > 0 else {
            return UnitConverter.convert(quantity: ingredient.quantity, unit: ingredient.unit, to: system)
        }
        return UnitConverter.convert(amount: amount * ratio, unit: ingredient.unit, to: system)
    }

    /// "250" -> "500", "12g" -> "24g", "12.5 g" -> "25 g". Text without a number is returned unchanged.
    static func nutritionValue(_ value: String, ratio: Double) -> String {
        guard ratio.isFinite, ratio > 0,
              let range = value.range(of: #"[0-9]+(\.[0-9]+)?"#, options: .regularExpression),
              let number = Double(value[range]) else { return value }
        let scaled = UnitConverter.decimalString(number * ratio)
        return value.replacingCharacters(in: range, with: scaled)
    }
}
