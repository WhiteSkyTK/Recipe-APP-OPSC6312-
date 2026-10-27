import Foundation

/// The `users/{uid}` document.
struct UserProfile: Codable, Equatable, Sendable {
    var username: String = ""
    var email: String = ""
    var fullName: String?
    var phoneNumber: String?
    var country: String?
    var gender: String?
    var dateOfBirth: String?
    var profileImageUrl: String?
    var selectedCuisines: [String] = []
    var selectedDiets: [String] = []

    init() {}

    enum CodingKeys: String, CodingKey {
        case username, email, country, gender, profileImageUrl
        case fullName = "full_name"
        case phoneNumber = "phone_number"
        case dateOfBirth = "date_of_birth"
        case selectedCuisines = "selected_cuisines"
        case selectedDiets = "selected_diets"
        // Android's Google sign-in wrote the country under this key instead of `country`.
        case legacySelectedCountry = "selected_country"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        username = c.lossyString(.username)
        email = c.lossyString(.email)
        fullName = try? c.decodeIfPresent(String.self, forKey: .fullName)
        phoneNumber = try? c.decodeIfPresent(String.self, forKey: .phoneNumber)
        country = (try? c.decodeIfPresent(String.self, forKey: .country))
            ?? (try? c.decodeIfPresent(String.self, forKey: .legacySelectedCountry))
        gender = try? c.decodeIfPresent(String.self, forKey: .gender)
        dateOfBirth = try? c.decodeIfPresent(String.self, forKey: .dateOfBirth)
        profileImageUrl = try? c.decodeIfPresent(String.self, forKey: .profileImageUrl)
        // Older Android builds saved these in the user's display language; store English IDs.
        let rawCuisines = (try? c.decodeIfPresent([String].self, forKey: .selectedCuisines)) ?? []
        let rawDiets = (try? c.decodeIfPresent([String].self, forKey: .selectedDiets)) ?? []
        selectedCuisines = rawCuisines.map(CuisineCatalog.canonicalID(for:))
        selectedDiets = rawDiets.map(DietCatalog.canonicalID(for:))
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(username, forKey: .username)
        try c.encode(email, forKey: .email)
        try c.encodeIfPresent(fullName, forKey: .fullName)
        try c.encodeIfPresent(phoneNumber, forKey: .phoneNumber)
        try c.encodeIfPresent(country, forKey: .country)
        try c.encodeIfPresent(gender, forKey: .gender)
        try c.encodeIfPresent(dateOfBirth, forKey: .dateOfBirth)
        try c.encodeIfPresent(profileImageUrl, forKey: .profileImageUrl)
        try c.encode(selectedCuisines, forKey: .selectedCuisines)
        try c.encode(selectedDiets, forKey: .selectedDiets)
    }

    var displayName: String {
        let name = fullName?.trimmingCharacters(in: .whitespaces) ?? ""
        return name.isEmpty ? username : name
    }
}

/// A document in `users/{uid}/notifications`.
struct AppNotification: Identifiable, Equatable, Sendable {
    var id: String
    var title: String
    var message: String
    var iconName: String
    var timestamp: Date?
    var isRead: Bool
}

/// A document in `users/{uid}/activity_log`, used by the recommendation engine.
struct UserActivity: Sendable {
    enum Kind: String {
        case viewRecipe = "VIEW_RECIPE"
        case searchQuery = "SEARCH_QUERY"
        case favoriteRecipe = "FAVORITE_RECIPE"
        case unfavoriteRecipe = "UNFAVORITE_RECIPE"
    }

    var type: String
    var value: String
    var durationSeconds: Int?
}

/// `users/{uid}/progress/main`.
struct UserProgress: Equatable, Sendable {
    var recipesCreated: Int = 0
    var recipesFavorited: Int = 0
    var loginStreak: Int = 0
    var lastLogin: Date?
    var earnedBadgeIDs: [String] = []
}

struct Badge: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let description: String
    /// SF Symbol shown for the badge.
    let symbol: String
}

/// A recipe returned by the ingredient scanner, with what the user still needs to buy.
struct RecipeMatch: Identifiable, Hashable, Sendable {
    let recipe: Recipe
    let missingIngredients: [String]

    var id: String { recipe.id }
}

/// A past ingredient search, kept only on this device.
struct ScanHistoryItem: Identifiable, Codable, Hashable, Sendable {
    var id = UUID()
    var title: String
    var imageUrl: String?
    var ingredients: [String]
    var timestamp = Date()
}
