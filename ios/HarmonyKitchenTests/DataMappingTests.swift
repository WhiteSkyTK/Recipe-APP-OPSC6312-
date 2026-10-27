import XCTest
@testable import HarmonyKitchen

final class RecipeCodingTests: XCTestCase {
    /// Firestore's Android mapper stores Kotlin's isPublic/isPopular/isVegan as public/popular/vegan.
    func testDecodesAndroidFieldNames() throws {
        let json = """
        {"id":"r1","title":"Toast","public":true,"popular":true,"vegan":true,"timeInMins":30,
         "ingredients":[{"name":"Bread","quantity":2,"unit":"slices"}],
         "method":[{"step":"Toast it","completed":false}],"favorite":false}
        """
        let recipe = try JSONDecoder().decode(Recipe.self, from: Data(json.utf8))
        XCTAssertTrue(recipe.isPublic)
        XCTAssertTrue(recipe.isPopular)
        XCTAssertTrue(recipe.isVegan)
        XCTAssertEqual(recipe.timeInMins, 30)
        XCTAssertEqual(recipe.servings, 0, "missing fields fall back to defaults")
        XCTAssertEqual(recipe.ingredients.first?.quantity, "2", "numeric quantities become text")
        XCTAssertEqual(recipe.method.first?.step, "Toast it")
    }

    func testEncodesAndroidFieldNames() throws {
        var recipe = Recipe()
        recipe.id = "r2"
        recipe.isPublic = true
        recipe.isPopular = true
        let data = try JSONEncoder().encode(recipe)
        let object = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        XCTAssertEqual(object["public"] as? Bool, true)
        XCTAssertEqual(object["popular"] as? Bool, true)
        XCTAssertNil(object["isPublic"])
        XCTAssertEqual(object["favorite"] as? Bool, false)
    }

    func testEmptyDocumentDecodes() throws {
        let recipe = try JSONDecoder().decode(Recipe.self, from: Data("{}".utf8))
        XCTAssertEqual(recipe.id, "")
        XCTAssertTrue(recipe.ingredients.isEmpty)
    }
}

final class RecipeMapperTests: XCTestCase {
    func testTastyMapping() throws {
        let json = """
        {"results":[
          {"id":1,"name":"Pancakes","thumbnail_url":"https://example.com/p.jpg","description":"Fluffy",
           "num_servings":4,"total_time_minutes":0,"cook_time_minutes":10,"prep_time_minutes":5,
           "sections":[{"components":[
             {"ingredient":{"name":"flour"},"measurements":[{"quantity":"1½","unit":{"name":"cup","display_singular":"cup"}}]},
             {"ingredient":{"name":"salt"},"measurements":[{"quantity":"0","unit":{"name":"","display_singular":""}}]}
           ]}],
           "instructions":[{"display_text":"Mix."}],
           "tags":[{"type":"dietary","name":"vegetarian","display_name":"Vegetarian"},
                   {"type":"meal","name":"breakfast","display_name":"Breakfast"}],
           "user_ratings":{"score":0.95}},
          {"id":2,"name":"A compilation"}
        ]}
        """
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let response = try decoder.decode(TastyResponse.self, from: Data(json.utf8))
        let recipes = response.results.compactMap(RecipeMapper.recipe(from:))

        XCTAssertEqual(recipes.count, 1, "compilations without ingredients or steps are skipped")
        let pancakes = try XCTUnwrap(recipes.first)
        XCTAssertEqual(pancakes.id, "tasty_1")
        XCTAssertEqual(pancakes.ingredients.count, 1, "zero quantities are dropped, as on Android")
        XCTAssertEqual(pancakes.ingredients.first?.name, "Flour")
        XCTAssertEqual(pancakes.ingredients.first?.quantity, "1.5")
        XCTAssertEqual(pancakes.timeInMins, 15)
        XCTAssertEqual(pancakes.mealType, "Breakfast")
        XCTAssertEqual(pancakes.category, "General")
        XCTAssertTrue(pancakes.isVegetarian)
        XCTAssertEqual(pancakes.dietTags, ["Vegetarian"])
        XCTAssertTrue(pancakes.isPopular)
    }

    func testSpoonacularMapping() throws {
        let json = """
        {"id":42,"title":"Soup","image":"https://img/42-312x231.jpg","imageType":"jpg","servings":2,
         "readyInMinutes":25,"summary":"A <b>tasty</b> soup &amp; more.",
         "extendedIngredients":[{"name":"carrot","amount":0.3333333,"unit":"cups"}],
         "analyzedInstructions":[{"steps":[{"number":1,"step":"Boil."}]}],
         "vegan":true,"vegetarian":true,"glutenFree":false,"dairyFree":true,"diets":["vegan"],
         "cuisines":["african"],"dishTypes":["lunch"],"spoonacularScore":90.0}
        """
        let source = try JSONDecoder().decode(SpoonacularRecipe.self, from: Data(json.utf8))
        let soup = RecipeMapper.recipe(from: source)

        XCTAssertEqual(soup.id, "sp_42")
        XCTAssertEqual(soup.imageUrl, "https://img.spoonacular.com/recipes/42-636x393.jpg")
        XCTAssertEqual(soup.description, "A tasty soup & more.")
        XCTAssertEqual(soup.category, "African")
        XCTAssertEqual(soup.mealType, "Lunch")
        XCTAssertEqual(soup.ingredients.first?.quantity, "0.333")
        XCTAssertEqual(soup.method.map(\.step), ["Boil."])
        XCTAssertTrue(soup.isPopular)
        XCTAssertEqual(soup.dietTags, ["Vegan", "Vegetarian", "Dairy-Free"])
        XCTAssertEqual(soup.author, "Spoonacular")
    }
}

final class ValidatorTests: XCTestCase {
    func testEmail() {
        XCTAssertTrue(Validators.isValidEmail("cook@example.co.za"))
        XCTAssertFalse(Validators.isValidEmail("cook@"))
        XCTAssertFalse(Validators.isValidEmail("username"))
    }

    func testPasswordRules() {
        XCTAssertNil(Validators.passwordProblem("Abcdef1!"))
        XCTAssertEqual(Validators.passwordProblem(""), "validation_password_empty")
        XCTAssertEqual(Validators.passwordProblem("Ab1!"), "validation_password_min_length")
        XCTAssertEqual(Validators.passwordProblem("abcdefg1!"), "validation_password_no_uppercase")
        XCTAssertEqual(Validators.passwordProblem("ABCDEFG1!"), "validation_password_no_lowercase")
        XCTAssertEqual(Validators.passwordProblem("Abcdefgh!"), "validation_password_no_digit")
        XCTAssertEqual(Validators.passwordProblem("Abcdefg12"), "validation_password_no_special")
    }

    func testPhoneNumbers() {
        XCTAssertEqual(Validators.e164(dialCode: "+27", national: "082 123 4567"), "+27821234567")
        XCTAssertEqual(Validators.e164(dialCode: "+27", national: "+27 82 123 4567"), "+27821234567")
        XCTAssertNil(Validators.e164(dialCode: "+1", national: "12345"))
    }
}

final class TextTests: XCTestCase {
    func testSpeechIsSplitIntoIngredients() {
        XCTAssertEqual(SpeechRecognizer.ingredients(fromSpeech: "chicken breast and rice, onions"), ["Chicken breast", "Rice", "Onions"])
        XCTAssertEqual(SpeechRecognizer.ingredients(fromSpeech: "tomato onion garlic"), ["Tomato", "Onion", "Garlic"])
    }

    func testPreferencesAreStoredInEnglish() {
        XCTAssertEqual(CuisineCatalog.canonicalID(for: "italian"), "Italian")
        XCTAssertEqual(DietCatalog.canonicalID(for: " vegan "), "Vegan")
        XCTAssertEqual(CuisineCatalog.canonicalID(for: "Sushi"), "Sushi", "unknown values are kept")
    }

    func testDisplayNameFormatting() {
        XCTAssertEqual(Greeting.displayName("tOKOLLO nonyane", allCaps: false), "Tokollo Nonyane")
        XCTAssertEqual(Greeting.displayName("Tokollo", allCaps: true), "TOKOLLO")
    }

    func testCategoriesAndSearch() {
        let recipes = ["Thai", "Italian", "", "Thai"].enumerated().map { index, category in
            var recipe = Recipe()
            recipe.id = "\(index)"
            recipe.category = category
            return recipe
        }
        XCTAssertEqual(RecipeRepository.categories(in: recipes), ["Italian", "Thai"])

        var dessert = Recipe()
        dessert.title = "Crème Brûlée"
        XCTAssertTrue(RecipeRepository.recipe(dessert, matches: "creme"))
    }

    func testHTMLIsStripped() {
        XCTAssertEqual(TextCleaner.plainText(fromHTML: "A <b>bold</b> &amp; <i>brave</i> dish"), "A bold & brave dish")
    }

    func testFlagEmoji() {
        XCTAssertEqual(Country(code: "ZA", englishName: "South Africa", localizedName: "South Africa").flag, "🇿🇦")
    }
}
