import XCTest
@testable import HarmonyKitchen

private func recipe(
    id: String,
    title: String = "Recipe",
    ingredients: [String] = [],
    category: String = "",
    configure: (inout Recipe) -> Void = { _ in }
) -> Recipe {
    var recipe = Recipe()
    recipe.id = id
    recipe.title = title
    recipe.category = category
    recipe.ingredients = ingredients.map { Ingredient(name: $0, quantity: "1", unit: "") }
    configure(&recipe)
    return recipe
}

final class DietFilterTests: XCTestCase {
    func testWholeWordMatching() {
        XCTAssertFalse(DietFilter.containsWord("graham crackers", "ham"))
        XCTAssertFalse(DietFilter.containsWord("eggplant", "egg"))
        XCTAssertTrue(DietFilter.containsWord("2 eggs", "egg"))
        XCTAssertTrue(DietFilter.containsWord("tomatoes", "tomato"))
        XCTAssertTrue(DietFilter.containsWord("smoked bacon", "bacon"))
    }

    func testHalalAndKosherUseKeywords() {
        let breakfast = recipe(id: "u1", ingredients: ["Bacon", "Eggs"])
        XCTAssertFalse(DietFilter.matches(breakfast, diets: ["Halal"]))

        let cheeseburger = recipe(id: "u2", ingredients: ["Beef mince", "Cheddar cheese"])
        XCTAssertFalse(DietFilter.matches(cheeseburger, diets: ["Kosher"]))

        let salad = recipe(id: "u3", ingredients: ["Lettuce", "Tomato"])
        XCTAssertTrue(DietFilter.matches(salad, diets: ["Halal", "Kosher", "Nut Allergy"]))
    }

    /// User recipes have no diet flags; Android's flag-only query hid them from every diet.
    func testUserRecipesAreJudgedByIngredients() {
        let beans = recipe(id: "user-recipe", title: "Rice and beans", ingredients: ["Rice", "Beans"])
        XCTAssertTrue(DietFilter.matches(beans, diets: ["Vegetarian"]))

        let stew = recipe(id: "user-stew", ingredients: ["Chicken thighs", "Onion"])
        XCTAssertFalse(DietFilter.matches(stew, diets: ["Vegetarian"]))
    }

    func testImportedRecipesNeedTheirFlag() {
        let unflagged = recipe(id: "sp_1", ingredients: ["Rice"])
        XCTAssertFalse(DietFilter.matches(unflagged, diets: ["Vegetarian"]))

        let tofu = recipe(id: "sp_2", ingredients: ["Tofu"]) { $0.isVegan = true }
        XCTAssertTrue(DietFilter.matches(tofu, diets: ["Vegan"]))
        XCTAssertTrue(DietFilter.matches(tofu, diets: ["Vegetarian"]), "vegan implies vegetarian")
    }

    func testEveryDietMustPass() {
        let pasta = recipe(id: "u4", ingredients: ["Pasta", "Tomato"])
        XCTAssertTrue(DietFilter.matches(pasta, diets: ["Vegetarian"]))
        XCTAssertFalse(DietFilter.matches(pasta, diets: ["Vegetarian", "Low-Carb"]))
        XCTAssertTrue(DietFilter.matches(pasta, diets: []))
    }
}

final class IngredientMatcherTests: XCTestCase {
    func testWordMatching() {
        XCTAssertTrue(IngredientMatcher.userHas("chicken", covers: "chicken breast"))
        XCTAssertTrue(IngredientMatcher.userHas("Tomatoes", covers: "tomato"))
        XCTAssertFalse(IngredientMatcher.userHas("rice", covers: "licorice"))
    }

    func testMatchesAreRankedAndFiltered() {
        let recipes = [
            recipe(id: "r1", title: "B", ingredients: ["Chicken breast", "Rice", "Salt"]),
            recipe(id: "r2", title: "C", ingredients: ["Beef", "Onion"]),
            recipe(id: "r3", title: "A", ingredients: ["Chicken", "Rice"]),
            recipe(id: "r4", title: "D", ingredients: ["Chicken", "a", "b", "c", "d", "e", "f"]),
        ]
        let matches = IngredientMatcher.matches(for: ["chicken", "rice"], in: recipes)
        XCTAssertEqual(matches.map(\.recipe.id), ["r3", "r1"], "r2 matches nothing, r4 misses six")
        XCTAssertEqual(matches.last?.missingIngredients, ["salt"])
    }

    func testNoIngredientsMeansNoMatches() {
        XCTAssertTrue(IngredientMatcher.matches(for: [" "], in: [recipe(id: "x", ingredients: ["Salt"])]).isEmpty)
    }
}

final class RecommendationTests: XCTestCase {
    func testScoringOrder() {
        let popular = recipe(id: "a", title: "Popular") { $0.isPopular = true }
        let italian = recipe(id: "b", title: "Italian", category: "Italian")
        let favourite = recipe(id: "c", title: "Favourite")
        var signals = RecommendationSignals()
        signals.cuisines = ["italian"]
        signals.favoriteIDs = ["c"]
        let ranked = RecommendationEngine.rank([popular, italian, favourite], signals: signals)
        XCTAssertEqual(ranked.map(\.id), ["c", "b", "a"])
    }

    func testLongViewsCountMore() {
        let glanced = recipe(id: "g")
        let studied = recipe(id: "s")
        var signals = RecommendationSignals()
        signals.viewedSeconds = ["g": 5, "s": 45]
        XCTAssertEqual(RecommendationEngine.rank([glanced, studied], signals: signals).map(\.id), ["s", "g"])
    }

    func testDietsAreApplied() {
        let chicken = recipe(id: "u1", ingredients: ["Chicken"])
        var signals = RecommendationSignals()
        signals.diets = ["Vegetarian"]
        XCTAssertTrue(RecommendationEngine.rank([chicken], signals: signals).isEmpty)
    }
}

final class LoginStreakTests: XCTestCase {
    private var calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }()

    private func date(_ year: Int, _ month: Int, _ day: Int, _ hour: Int) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day, hour: hour))!
    }

    /// Android compared day-of-year numbers, so the streak reset every 1 January.
    func testStreakContinuesAcrossNewYear() {
        let next = LoginStreak.next(current: 3, lastLogin: date(2025, 12, 31, 22), now: date(2026, 1, 1, 8), calendar: calendar)
        XCTAssertEqual(next, 4)
    }

    func testSameDayKeepsStreak() {
        XCTAssertEqual(LoginStreak.next(current: 4, lastLogin: date(2026, 3, 2, 7), now: date(2026, 3, 2, 21), calendar: calendar), 4)
    }

    func testGapResetsStreak() {
        XCTAssertEqual(LoginStreak.next(current: 9, lastLogin: date(2026, 3, 1, 7), now: date(2026, 3, 4, 7), calendar: calendar), 1)
        XCTAssertEqual(LoginStreak.next(current: 0, lastLogin: nil, calendar: calendar), 1)
    }
}
