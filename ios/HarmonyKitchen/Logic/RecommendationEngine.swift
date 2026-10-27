import Foundation

/// Signals about the user that drive "Recommended for you".
struct RecommendationSignals: Sendable {
    var diets: [String] = []
    var cuisines: [String] = []
    var favoriteIDs: Set<String> = []
    /// Recipe ID -> longest view in seconds.
    var viewedSeconds: [String: Int] = [:]
    var searchQueries: [String] = []
}

/// Scores recipes the same way as the Android repository's getRecommendedForYou().
enum RecommendationEngine {
    static func rank(_ candidates: [Recipe], signals: RecommendationSignals, limit: Int = 20) -> [Recipe] {
        let eligible = DietFilter.filter(candidates, diets: signals.diets)
        let queries = signals.searchQueries.map { $0.lowercased() }.filter { !$0.isEmpty }
        let cuisines = signals.cuisines.map { $0.lowercased() }

        let scored = eligible.map { recipe -> (Recipe, Double) in
            var score = 0.0
            if signals.favoriteIDs.contains(recipe.id) { score += 50 }
            if let seconds = signals.viewedSeconds[recipe.id] {
                score += 10
                if seconds > 30 { score += 15 }
            }
            if cuisines.contains(recipe.category.lowercased()) { score += 25 }
            let title = recipe.title.lowercased()
            for query in queries where title.contains(query) { score += 5 }
            if recipe.isPopular { score += 5 }
            return (recipe, score)
        }

        // Stable order for equal scores keeps the list from reshuffling on every refresh.
        return scored
            .enumerated()
            .sorted { lhs, rhs in
                lhs.element.1 != rhs.element.1 ? lhs.element.1 > rhs.element.1 : lhs.offset < rhs.offset
            }
            .prefix(limit)
            .map { $0.element.0 }
    }
}

/// Matches what the user has against recipe ingredient lists (the Scan feature).
enum IngredientMatcher {
    /// A recipe is shown when the user has at least one of its ingredients and is missing at most
    /// `maxMissing`. Matching is word-based, so "chicken" covers "chicken breast" and "Tomatoes"
    /// covers "tomato". (Android required exact name equality and also listed recipes that
    /// matched none of the user's ingredients.)
    static func matches(for userIngredients: [String], in recipes: [Recipe], maxMissing: Int = 5) -> [RecipeMatch] {
        let have = userIngredients
            .map { normalize($0) }
            .filter { !$0.isEmpty }
        guard !have.isEmpty else { return [] }

        let matches = recipes.compactMap { recipe -> RecipeMatch? in
            let needed = uniqueNames(recipe.ingredients.map(\.name))
            guard !needed.isEmpty else { return nil }
            let missing = needed.filter { name in !have.contains { userHas($0, covers: name) } }
            guard missing.count < needed.count, missing.count <= maxMissing else { return nil }
            return RecipeMatch(recipe: recipe, missingIngredients: missing)
        }
        return matches.sorted {
            ($0.missingIngredients.count, $0.recipe.title) < ($1.missingIngredients.count, $1.recipe.title)
        }
    }

    /// True when an ingredient the user named covers a recipe ingredient.
    static func userHas(_ userItem: String, covers recipeIngredient: String) -> Bool {
        let item = normalize(userItem)
        let ingredient = normalize(recipeIngredient)
        guard !item.isEmpty, !ingredient.isEmpty else { return false }
        if item == ingredient { return true }
        return DietFilter.containsWord(ingredient, singular(item)) || DietFilter.containsWord(item, singular(ingredient))
    }

    static func normalize(_ text: String) -> String {
        text.lowercased()
            .folding(options: .diacriticInsensitive, locale: .current)
            .trimmingCharacters(in: .whitespacesAndNewlines.union(.punctuationCharacters))
    }

    private static func singular(_ word: String) -> String {
        if word.hasSuffix("oes") { return String(word.dropLast(2)) }
        if word.hasSuffix("ies") { return String(word.dropLast(3)) + "y" }
        if word.hasSuffix("s"), !word.hasSuffix("ss"), word.count > 3 { return String(word.dropLast()) }
        return word
    }

    private static func uniqueNames(_ names: [String]) -> [String] {
        var seen = Set<String>()
        return names.compactMap { name in
            let key = normalize(name)
            guard !key.isEmpty, seen.insert(key).inserted else { return nil }
            return key
        }
    }
}

/// Daily login streak, counted in calendar days in the user's time zone.
/// (Android compared day-of-year numbers, so the streak reset every 1 January.)
enum LoginStreak {
    static func next(current: Int, lastLogin: Date?, now: Date = Date(), calendar: Calendar = .current) -> Int {
        guard let lastLogin else { return 1 }
        let days = calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: lastLogin),
            to: calendar.startOfDay(for: now)
        ).day ?? 0
        switch days {
        case 0: return max(current, 1)
        case 1: return current + 1
        default: return 1
        }
    }
}
