import Foundation

/// A recipe document in the shared `recipes` collection (and in `users/{uid}/favorites`).
///
/// The coding keys match what the Android app writes. Firestore's Android mapper strips the
/// `is` prefix from Kotlin boolean properties, so `isPublic` is stored as `public`,
/// `isPopular` as `popular`, `isVegan` as `vegan`, and so on.
struct Recipe: Identifiable, Hashable, Codable, Sendable {
    var id: String = ""
    var userId: String = ""
    var title: String = ""
    var isPublic: Bool = false
    var imageUrl: String = ""
    var timeInMins: Int = 0
    var author: String = ""
    var description: String = ""
    var servings: Int = 0
    var category: String = ""
    var mealType: String = ""
    var nutrition: [NutritionFact] = []
    var ingredients: [Ingredient] = []
    var dietTags: [String] = []
    var diets: [String] = []
    var isVegan: Bool = false
    var isVegetarian: Bool = false
    var isGlutenFree: Bool = false
    var isDairyFree: Bool = false
    var isKeto: Bool = false
    var isLowFodmap: Bool = false
    var isPaleo: Bool = false
    var isPopular: Bool = false
    var method: [MethodStep] = []

    init() {}

    enum CodingKeys: String, CodingKey {
        case id, userId, title, imageUrl, timeInMins, author, description, servings, category
        case mealType, nutrition, ingredients, dietTags, diets, method
        case isPublic = "public"
        case isVegan = "vegan"
        case isVegetarian = "vegetarian"
        case isGlutenFree = "glutenFree"
        case isDairyFree = "dairyFree"
        case isKeto = "keto"
        case isLowFodmap = "lowFodmap"
        case isPaleo = "paleo"
        case isPopular = "popular"
        // Android writes a `favorite` flag on every recipe; favourites are tracked per user instead.
        case favorite
    }

    // Every field is optional on decode: documents come from three sources (Spoonacular,
    // Tasty and user-created) and older documents miss newer fields.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.lossyString(.id)
        userId = c.lossyString(.userId)
        title = c.lossyString(.title)
        isPublic = c.lossyBool(.isPublic)
        imageUrl = c.lossyString(.imageUrl)
        timeInMins = c.lossyInt(.timeInMins)
        author = c.lossyString(.author)
        description = c.lossyString(.description)
        servings = c.lossyInt(.servings)
        category = c.lossyString(.category)
        mealType = c.lossyString(.mealType)
        nutrition = (try? c.decodeIfPresent([NutritionFact].self, forKey: .nutrition)) ?? []
        ingredients = (try? c.decodeIfPresent([Ingredient].self, forKey: .ingredients)) ?? []
        dietTags = (try? c.decodeIfPresent([String].self, forKey: .dietTags)) ?? []
        diets = (try? c.decodeIfPresent([String].self, forKey: .diets)) ?? []
        isVegan = c.lossyBool(.isVegan)
        isVegetarian = c.lossyBool(.isVegetarian)
        isGlutenFree = c.lossyBool(.isGlutenFree)
        isDairyFree = c.lossyBool(.isDairyFree)
        isKeto = c.lossyBool(.isKeto)
        isLowFodmap = c.lossyBool(.isLowFodmap)
        isPaleo = c.lossyBool(.isPaleo)
        isPopular = c.lossyBool(.isPopular)
        method = (try? c.decodeIfPresent([MethodStep].self, forKey: .method)) ?? []
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(userId, forKey: .userId)
        try c.encode(title, forKey: .title)
        try c.encode(isPublic, forKey: .isPublic)
        try c.encode(imageUrl, forKey: .imageUrl)
        try c.encode(timeInMins, forKey: .timeInMins)
        try c.encode(author, forKey: .author)
        try c.encode(description, forKey: .description)
        try c.encode(servings, forKey: .servings)
        try c.encode(category, forKey: .category)
        try c.encode(mealType, forKey: .mealType)
        try c.encode(nutrition, forKey: .nutrition)
        try c.encode(ingredients, forKey: .ingredients)
        try c.encode(dietTags, forKey: .dietTags)
        try c.encode(diets, forKey: .diets)
        try c.encode(isVegan, forKey: .isVegan)
        try c.encode(isVegetarian, forKey: .isVegetarian)
        try c.encode(isGlutenFree, forKey: .isGlutenFree)
        try c.encode(isDairyFree, forKey: .isDairyFree)
        try c.encode(isKeto, forKey: .isKeto)
        try c.encode(isLowFodmap, forKey: .isLowFodmap)
        try c.encode(isPaleo, forKey: .isPaleo)
        try c.encode(isPopular, forKey: .isPopular)
        try c.encode(method, forKey: .method)
        try c.encode(false, forKey: .favorite)
    }
}

struct NutritionFact: Hashable, Codable, Sendable {
    var label: String = ""
    var value: String = ""

    init(label: String, value: String) {
        self.label = label
        self.value = value
    }

    enum CodingKeys: String, CodingKey { case label, value }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        label = c.lossyString(.label)
        value = c.lossyString(.value)
    }
}

struct Ingredient: Hashable, Codable, Sendable {
    var name: String = ""
    /// Stored as text: "2", "0.5", "1-2", or free text such as "a pinch".
    var quantity: String = ""
    var unit: String = ""

    init(name: String, quantity: String, unit: String) {
        self.name = name
        self.quantity = quantity
        self.unit = unit
    }

    enum CodingKeys: String, CodingKey { case name, quantity, unit }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        name = c.lossyString(.name)
        quantity = c.lossyString(.quantity)
        unit = c.lossyString(.unit)
    }
}

struct MethodStep: Hashable, Codable, Sendable {
    var step: String = ""

    init(step: String) {
        self.step = step
    }

    enum CodingKeys: String, CodingKey {
        case step
        // Android's MethodStep.isCompleted; written for compatibility, never read.
        case completed
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        step = c.lossyString(.step)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(step, forKey: .step)
        try c.encode(false, forKey: .completed)
    }
}

extension Recipe {
    /// A short teaser for cards: the first sentence of the description, capped at 90 characters.
    var shortDescription: String {
        let firstSentence = description.split(separator: ".", maxSplits: 1).first.map(String.init) ?? description
        let trimmed = firstSentence.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.count > 90 ? String(trimmed.prefix(87)) + "…" : trimmed
    }
}

// MARK: - Lenient decoding helpers

extension KeyedDecodingContainer {
    /// Decodes a string, accepting numbers too (some quantities were stored as numbers).
    func lossyString(_ key: Key) -> String {
        if let value = try? decodeIfPresent(String.self, forKey: key) { return value }
        if let value = try? decodeIfPresent(Int.self, forKey: key) { return String(value) }
        if let value = try? decodeIfPresent(Double.self, forKey: key) { return String(value) }
        return ""
    }

    func lossyInt(_ key: Key) -> Int {
        if let value = try? decodeIfPresent(Int.self, forKey: key) { return value }
        if let value = try? decodeIfPresent(Double.self, forKey: key), value.isFinite { return Int(value) }
        if let value = try? decodeIfPresent(String.self, forKey: key) { return Int(value) ?? 0 }
        return 0
    }

    func lossyBool(_ key: Key) -> Bool {
        if let value = try? decodeIfPresent(Bool.self, forKey: key) { return value }
        if let value = try? decodeIfPresent(Int.self, forKey: key) { return value != 0 }
        return false
    }
}
