import Foundation

// MARK: - Spoonacular API models (every field optional: the API omits fields freely)

struct SpoonacularRecipe: Decodable, Sendable {
    let id: Int
    let title: String?
    let image: String?
    let imageType: String?
    let servings: Int?
    let readyInMinutes: Int?
    let sourceName: String?
    let summary: String?
    let extendedIngredients: [SpoonacularIngredient]?
    let analyzedInstructions: [SpoonacularInstruction]?
    let veryPopular: Bool?
    let spoonacularScore: Double?
    let vegetarian: Bool?
    let vegan: Bool?
    let glutenFree: Bool?
    let dairyFree: Bool?
    let diets: [String]?
    let ketogenic: Bool?
    let lowFodmap: Bool?
    let whole30: Bool?
    let cuisines: [String]?
    let dishTypes: [String]?
}

struct SpoonacularIngredient: Decodable, Sendable {
    let name: String?
    let amount: Double?
    let unit: String?
}

struct SpoonacularInstruction: Decodable, Sendable {
    let steps: [SpoonacularStep]?
}

struct SpoonacularStep: Decodable, Sendable {
    let number: Int?
    let step: String?
}

struct SpoonacularRandomResponse: Decodable, Sendable {
    let recipes: [SpoonacularRecipe]
}

struct SpoonacularSearchResponse: Decodable, Sendable {
    struct Result: Decodable, Sendable { let id: Int }
    let results: [Result]
}

// MARK: - Tasty API models

struct TastyResponse: Decodable, Sendable {
    let results: [TastyRecipe]
}

struct TastyRecipe: Decodable, Sendable {
    let id: Int
    let name: String?
    let thumbnailUrl: String?
    let description: String?
    let cookTimeMinutes: Int?
    let prepTimeMinutes: Int?
    let totalTimeMinutes: Int?
    let numServings: Int?
    let userRatings: UserRatings?
    let sections: [Section]?
    let nutrition: Nutrition?
    let instructions: [Instruction]?
    let tags: [Tag]?

    struct UserRatings: Decodable, Sendable { let score: Double? }
    struct Nutrition: Decodable, Sendable {
        let calories: Int?
        let fat: Int?
        let carbohydrates: Int?
        let protein: Int?
        let sugar: Int?
    }
    struct Section: Decodable, Sendable { let components: [Component]? }
    struct Component: Decodable, Sendable {
        let ingredient: NamedIngredient?
        let measurements: [Measurement]?
    }
    struct NamedIngredient: Decodable, Sendable { let name: String? }
    struct Measurement: Decodable, Sendable {
        let quantity: String?
        let unit: Unit?
    }
    struct Unit: Decodable, Sendable {
        let name: String?
        let displaySingular: String?
    }
    struct Instruction: Decodable, Sendable { let displayText: String? }
    struct Tag: Decodable, Sendable {
        let type: String?
        let name: String?
        let displayName: String?
    }
}

// MARK: - Mapping to the shared Recipe document

enum RecipeMapper {
    static func recipe(from source: SpoonacularRecipe) -> Recipe {
        var recipe = Recipe()
        recipe.id = "sp_\(source.id)"
        recipe.title = source.title ?? ""
        if source.image != nil, let type = source.imageType, !type.isEmpty {
            // 636x393 is the largest size Spoonacular serves.
            recipe.imageUrl = "https://img.spoonacular.com/recipes/\(source.id)-636x393.\(type)"
        } else {
            recipe.imageUrl = source.image ?? ""
        }
        recipe.timeInMins = source.readyInMinutes ?? 0
        recipe.servings = source.servings ?? 0
        recipe.author = source.sourceName ?? "Spoonacular"
        recipe.description = source.summary.map(TextCleaner.plainText(fromHTML:)) ?? "No description available."
        recipe.isPublic = true
        recipe.category = source.cuisines?.first.map(TextCleaner.capitalizedFirst) ?? "General"
        recipe.mealType = source.dishTypes?.first.map(TextCleaner.capitalizedFirst) ?? "Dinner"
        recipe.ingredients = (source.extendedIngredients ?? []).compactMap { item in
            guard let name = item.name, !name.isEmpty else { return nil }
            let quantity = item.amount.map { UnitConverter.decimalStringPrecise($0) } ?? ""
            return Ingredient(name: name, quantity: quantity, unit: item.unit ?? "")
        }
        recipe.method = (source.analyzedInstructions ?? [])
            .flatMap { $0.steps ?? [] }
            .compactMap { $0.step.map(MethodStep.init(step:)) }
        recipe.isVegan = source.vegan ?? false
        recipe.isVegetarian = source.vegetarian ?? false
        recipe.isGlutenFree = source.glutenFree ?? false
        recipe.isDairyFree = source.dairyFree ?? false
        recipe.isKeto = source.ketogenic ?? false
        recipe.isLowFodmap = source.lowFodmap ?? false
        recipe.isPaleo = source.whole30 ?? false // The Android app maps whole30 to Paleo.
        recipe.diets = source.diets ?? []
        recipe.isPopular = (source.veryPopular ?? false) || (source.spoonacularScore ?? 0) > 85

        var tags: [String] = []
        if recipe.isVegan { tags.append("Vegan") }
        if recipe.isVegetarian { tags.append("Vegetarian") }
        if recipe.isGlutenFree { tags.append("Gluten-Free") }
        if recipe.isKeto { tags.append("Keto") }
        if recipe.isPaleo { tags.append("Paleo") }
        if recipe.isDairyFree { tags.append("Dairy-Free") }
        if recipe.isLowFodmap { tags.append("Low-FODMAP") }
        recipe.dietTags = tags
        return recipe
    }

    /// Returns nil for Tasty "compilation" entries, which have no ingredients or steps.
    static func recipe(from source: TastyRecipe) -> Recipe? {
        let ingredients: [Ingredient] = (source.sections ?? [])
            .flatMap { $0.components ?? [] }
            .compactMap { component in
                guard let name = component.ingredient?.name, !name.isEmpty else { return nil }
                let measurement = component.measurements?.first
                // Tasty quantities look like "1 ½" or "1/2"; store them as decimals so servings scale.
                guard let raw = measurement?.quantity,
                      let amount = UnitConverter.parseQuantity(raw), amount > 0 else { return nil }
                return Ingredient(
                    name: TextCleaner.capitalizedFirst(name),
                    quantity: UnitConverter.decimalStringPrecise(amount),
                    unit: measurement?.unit?.displaySingular ?? ""
                )
            }
        let steps = (source.instructions ?? []).compactMap { $0.displayText.map(MethodStep.init(step:)) }
        guard !ingredients.isEmpty || !steps.isEmpty else { return nil }

        var recipe = Recipe()
        recipe.id = "tasty_\(source.id)"
        recipe.title = source.name ?? ""
        recipe.imageUrl = source.thumbnailUrl ?? ""
        let description = source.description?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        recipe.description = description.isEmpty ? "No description available." : TextCleaner.plainText(fromHTML: description)
        let total = source.totalTimeMinutes.flatMap { $0 > 0 ? $0 : nil }
            ?? ((source.cookTimeMinutes ?? 0) + (source.prepTimeMinutes ?? 0))
        recipe.timeInMins = total > 0 ? total : 30
        recipe.servings = source.numServings ?? 0
        recipe.author = "Tasty"
        recipe.isPublic = true
        recipe.category = source.tags?.first { $0.type == "cuisine" }?.displayName ?? "General"
        recipe.mealType = source.tags?.first { $0.type == "meal" }?.displayName ?? "General"
        recipe.ingredients = ingredients
        recipe.method = steps

        var nutrition: [NutritionFact] = []
        if let n = source.nutrition {
            if let v = n.calories { nutrition.append(NutritionFact(label: "Calories", value: "\(v)")) }
            if let v = n.fat { nutrition.append(NutritionFact(label: "Fat", value: "\(v)g")) }
            if let v = n.carbohydrates { nutrition.append(NutritionFact(label: "Carbs", value: "\(v)g")) }
            if let v = n.protein { nutrition.append(NutritionFact(label: "Protein", value: "\(v)g")) }
            if let v = n.sugar { nutrition.append(NutritionFact(label: "Sugar", value: "\(v)g")) }
        }
        recipe.nutrition = nutrition

        let tagNames = Set((source.tags ?? []).compactMap(\.name))
        recipe.isVegan = tagNames.contains("vegan")
        recipe.isVegetarian = tagNames.contains("vegetarian")
        recipe.isGlutenFree = tagNames.contains("gluten_free")
        recipe.isDairyFree = tagNames.contains("dairy_free")
        recipe.isKeto = tagNames.contains("keto")
        let dietary = (source.tags ?? []).filter { $0.type == "dietary" }.compactMap(\.displayName)
        recipe.diets = dietary
        recipe.dietTags = dietary
        recipe.isPopular = (source.userRatings?.score ?? 0) > 0.9
        return recipe
    }
}

extension UnitConverter {
    /// Up to three decimals without trailing zeros ("0.333", "2", "1.5"). Used for stored quantities.
    static func decimalStringPrecise(_ value: Double) -> String {
        let rounded = (value * 1000).rounded() / 1000
        if rounded == rounded.rounded() { return String(Int(rounded)) }
        var text = String(format: "%.3f", locale: Locale(identifier: "en_US_POSIX"), rounded)
        while text.hasSuffix("0") { text.removeLast() }
        return text
    }
}
