import Foundation

/// Keyword lists used to screen recipes for diets that have no reliable flag in the data.
/// Copied from the Android DietaryKeywords object.
enum DietaryKeywords {
    static let meat: Set<String> = [
        "beef", "steak", "veal", "pork", "bacon", "ham", "prosciutto", "salami", "chorizo",
        "chicken", "poultry", "duck", "goose", "turkey", "lamb", "mutton", "venison", "rabbit",
        "sausage", "meatball", "jerky", "offal", "liver", "kidney", "tripe", "giblet",
        "pepperoni", "bresaola", "pastrami", "mortadella", "biltong", "kielbasa",
    ]

    static let seafood: Set<String> = [
        "fish", "salmon", "tuna", "cod", "sardine", "anchovy", "trout", "mackerel", "halibut",
        "haddock", "bass", "snapper", "catfish", "tilapia", "sole", "plaice", "swordfish",
        "shrimp", "prawn", "lobster", "crab", "oyster", "clam", "mussel", "scallop", "shellfish",
        "squid", "octopus", "cuttlefish", "eel", "roe", "caviar",
    ]

    static let dairy: Set<String> = [
        "milk", "cheese", "cheddar", "mozzarella", "parmesan", "feta", "brie", "gouda",
        "cream", "butter", "ghee", "yogurt", "curd", "custard", "whey", "casein", "kefir", "paneer",
        "sour cream", "buttermilk", "evaporated milk", "condensed milk", "milk powder",
        "ricotta", "mascarpone", "halloumi",
    ]

    static let egg: Set<String> = [
        "egg", "eggs", "mayonnaise", "mayo", "aioli", "meringue",
        "custard", "egg yolk", "egg white", "omelette", "frittata", "quiche",
    ]

    static let highCarb: Set<String> = [
        "sugar", "honey", "syrup", "molasses", "maple syrup", "corn syrup",
        "rice", "noodle", "pasta", "bread", "potato", "yam", "sweet potato",
        "flour", "tortilla", "bun", "cake", "pastry", "cookie", "cracker",
        "oats", "granola", "quinoa", "millet", "barley",
    ]

    static let gluten: Set<String> = [
        "wheat", "flour", "bread", "pasta", "spaghetti", "noodle", "biscuit", "cracker",
        "barley", "rye", "malt", "bulgur", "seitan", "farro", "couscous",
        "pastry", "cake", "cookie", "dough", "bagel", "pretzel",
    ]

    static let nonPaleo: Set<String> = [
        "bean", "lentil", "chickpea", "soy", "tofu", "edamame", "peanut",
        "milk", "cheese", "butter", "cream", "yogurt",
        "wheat", "corn", "oats", "rice", "quinoa", "barley", "rye",
        "sugar", "refined flour", "soda",
    ]

    static let highFodmap: Set<String> = [
        "milk", "soft cheese", "cream", "ice cream",
        "lentil", "chickpea", "kidney bean", "black bean",
        "apple", "pear", "mango", "watermelon", "cherry", "plum",
        "honey", "high-fructose corn syrup",
        "garlic", "onion", "cauliflower", "mushroom",
    ]

    static let nuts: Set<String> = [
        "nut", "peanut", "almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut",
        "macadamia", "brazil nut", "pine nut", "chestnut", "coconut",
    ]

    static let haram: Set<String> = [
        "pork", "bacon", "ham", "prosciutto", "salami", "lard",
        "alcohol", "wine", "beer", "whiskey", "brandy", "rum", "vodka", "gin", "liqueur",
        "champagne", "cognac", "tequila", "sake", "port", "sherry",
    ]

    static let nonKosher: Set<String> = [
        "pork", "bacon", "ham", "shellfish", "shrimp", "crab", "lobster", "clam", "oyster",
        "mussels", "scallops", "squid", "octopus", "cuttlefish",
    ]
}

/// Decides whether a recipe fits a user's diets. A recipe must satisfy every selected diet.
///
/// Recipes imported from Spoonacular or Tasty carry diet flags (vegan, keto, ...) set by those
/// services; they must have the flag *and* pass the keyword screen. Recipes written by users
/// have no flags, so for them the keyword screen alone decides (Android's flag-only query hid
/// every user recipe from anyone with a diet set).
///
/// The Android Discover screen used `whereArrayContainsAny("dietTags", diets)`, which returned
/// recipes matching *any* diet, and nothing at all for Halal, Kosher, Nut Allergy, Pescetarian
/// or Low-Carb because those never appear in tags.
enum DietFilter {
    /// Firestore field for diets that can be filtered server-side.
    static let firestoreField: [String: String] = [
        "Vegan": "vegan",
        "Vegetarian": "vegetarian",
        "Gluten-Free": "glutenFree",
        "Dairy-Free": "dairyFree",
        "Keto": "keto",
        "Paleo": "paleo",
        "Low-FODMAP": "lowFodmap",
    ]

    static func matches(_ recipe: Recipe, diets: [String]) -> Bool {
        guard !diets.isEmpty else { return true }
        let ingredientNames = recipe.ingredients.map { $0.name.lowercased() }
        let title = recipe.title.lowercased()

        func ingredientsContain(_ keywords: Set<String>) -> Bool {
            ingredientNames.contains { name in keywords.contains { containsWord(name, $0) } }
        }
        func titleContains(_ keywords: Set<String>) -> Bool {
            keywords.contains { containsWord(title, $0) }
        }
        func free(of keywords: Set<String>) -> Bool {
            !ingredientsContain(keywords) && !titleContains(keywords)
        }
        let fromAPI = isImported(recipe)
        func passes(flag: Bool, keywordsOK: Bool) -> Bool {
            fromAPI ? flag && keywordsOK : keywordsOK
        }

        return diets.allSatisfy { diet in
            switch diet {
            case "Vegan":
                let blocked = DietaryKeywords.meat.union(DietaryKeywords.seafood)
                    .union(DietaryKeywords.dairy).union(DietaryKeywords.egg)
                return passes(flag: recipe.isVegan, keywordsOK: free(of: blocked))
            case "Vegetarian":
                let blocked = DietaryKeywords.meat.union(DietaryKeywords.seafood)
                return passes(flag: recipe.isVegetarian || recipe.isVegan, keywordsOK: free(of: blocked))
            case "Gluten-Free":
                return passes(flag: recipe.isGlutenFree, keywordsOK: !ingredientsContain(DietaryKeywords.gluten))
            case "Dairy-Free":
                return passes(flag: recipe.isDairyFree || recipe.isVegan, keywordsOK: !ingredientsContain(DietaryKeywords.dairy))
            case "Keto":
                return passes(flag: recipe.isKeto, keywordsOK: !ingredientsContain(DietaryKeywords.highCarb))
            case "Paleo":
                return passes(flag: recipe.isPaleo, keywordsOK: !ingredientsContain(DietaryKeywords.nonPaleo))
            case "Low-FODMAP":
                return passes(flag: recipe.isLowFodmap, keywordsOK: !ingredientsContain(DietaryKeywords.highFodmap))
            case "Pescetarian": return free(of: DietaryKeywords.meat)
            case "Nut Allergy": return free(of: DietaryKeywords.nuts)
            case "Halal": return free(of: DietaryKeywords.haram)
            case "Kosher":
                let hasMeat = ingredientsContain(DietaryKeywords.meat)
                let hasDairy = ingredientsContain(DietaryKeywords.dairy)
                return !ingredientsContain(DietaryKeywords.nonKosher) && !(hasMeat && hasDairy)
            case "Low-Carb": return !ingredientsContain(DietaryKeywords.highCarb)
            default: return true
            }
        }
    }

    /// Spoonacular recipes are stored as "sp_<id>", Tasty recipes as "tasty_<id>".
    static func isImported(_ recipe: Recipe) -> Bool {
        recipe.id.hasPrefix("sp_") || recipe.id.hasPrefix("tasty_")
    }

    static func filter(_ recipes: [Recipe], diets: [String]) -> [Recipe] {
        diets.isEmpty ? recipes : recipes.filter { matches($0, diets: diets) }
    }

    /// Whole-word containment, so "ham" does not match "graham crackers" or "shallot" match "hallot".
    /// Plural "s"/"es" endings still match ("eggs" contains "egg").
    static func containsWord(_ text: String, _ keyword: String) -> Bool {
        var searchRange = text.startIndex..<text.endIndex
        while let range = text.range(of: keyword, options: .caseInsensitive, range: searchRange) {
            let beforeOK = range.lowerBound == text.startIndex || !text[text.index(before: range.lowerBound)].isLetter
            var after = range.upperBound
            // Allow plural endings.
            if after < text.endIndex, text[after] == "s" { after = text.index(after: after) }
            else if text[range.upperBound...].hasPrefix("es") { after = text.index(range.upperBound, offsetBy: 2) }
            let afterOK = after == text.endIndex || !text[after].isLetter
            if beforeOK && afterOK { return true }
            searchRange = range.upperBound..<text.endIndex
        }
        return false
    }
}
