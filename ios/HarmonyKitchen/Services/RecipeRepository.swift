import FirebaseFirestore
import Foundation

struct HomeContent: Sendable {
    var featured: [Recipe] = []
    var timeOfDay: [Recipe] = []
    /// Category names found in the data; the UI adds "All" in front.
    var categories: [String] = []
    var recommended: [Recipe] = []
}

/// Discover sort options. Raw values index into the `discover_sort_options` string array, so the
/// choice no longer depends on comparing translated labels (which broke sorting in Android's
/// Sepedi, Xitsonga and Tshivenda builds).
enum DiscoverSort: Int, CaseIterable, Identifiable, Sendable {
    case az = 0, recommended, popular, cookTime, za

    var id: Int { rawValue }
    var title: String { L("discover_sort_options.\(rawValue)") }

    fileprivate var field: String {
        switch self {
        case .az, .za, .recommended: "title"
        case .popular: "popular"
        case .cookTime: "timeInMins"
        }
    }

    fileprivate var descending: Bool { self == .za || self == .popular }
}

struct DiscoverPage: Sendable {
    var recipes: [Recipe]
    var hasMore: Bool
}

extension MealType {
    /// Values found in `mealType` for this meal. Spoonacular dish types and Tasty meal tags
    /// use several spellings ("Snack" vs "Snacks", "Main course"), so match all of them.
    var storedValues: [String] {
        switch self {
        case .breakfast: ["Breakfast", "Brunch", "Morning meal"]
        case .lunch: ["Lunch", "Salad", "Soup", "Main dish"]
        case .dinner: ["Dinner", "Main course", "Main dish"]
        case .snack, .dessert: ["Snack", "Snacks", "Dessert", "Desserts", "Appetizer", "Fingerfood", "Starter"]
        }
    }
}

/// All recipe reads and writes. One shared instance, so every screen reuses the same caches.
actor RecipeRepository {
    private let api = RecipeAPIClient()

    /// Up to `poolLimit` public recipes, reused by search, scan, categories and recommendations.
    private var pool: [Recipe] = []
    private var poolLoadedAt: Date?
    private var poolTask: Task<[Recipe], Never>?
    private let poolLimit = 400
    private let cacheLifetime: TimeInterval = 10 * 60

    private var homeCache: HomeContent?
    private var homeCacheKey: String?
    private var dietCandidates: [String: [Recipe]] = [:]

    private let discoverPageSize = 20
    private var discoverCursor: [DiscoverSort: DocumentSnapshot] = [:]
    private var discoverOffset: [DiscoverSort: Int] = [:]
    private var discoverUsesLocalSort = false

    func invalidateCaches() {
        homeCache = nil
        poolLoadedAt = nil
        dietCandidates = [:]
    }

    // MARK: - Recipe pool

    func publicPool(forceRefresh: Bool = false) async -> [Recipe] {
        if !forceRefresh, let loadedAt = poolLoadedAt, Date().timeIntervalSince(loadedAt) < cacheLifetime {
            return pool
        }
        if let poolTask { return await poolTask.value }
        let task = Task { await self.loadPool() }
        poolTask = task
        let result = await task.value
        poolTask = nil
        return result
    }

    private func loadPool() async -> [Recipe] {
        do {
            let query = FS.recipes.whereField("public", isEqualTo: true).limit(to: poolLimit)
            var recipes = FS.recipes(from: try await FS.getDocuments(query))
            if recipes.isEmpty, NetworkMonitor.currentlyOnline, AppConfig.hasRecipeAPIs {
                recipes = await importStarterRecipes()
            }
            pool = recipes
            poolLoadedAt = Date()
        } catch {
            print("Recipe pool load failed: \(error)")
        }
        return pool
    }

    // MARK: - Home

    func homeContent(signals: RecommendationSignals, uid: String?, forceRefresh: Bool) async -> HomeContent {
        // Preferences are part of the key so editing diets or cuisines refreshes recommendations.
        let key = [uid ?? "", signals.diets.sorted().joined(separator: ","), signals.cuisines.sorted().joined(separator: ",")]
            .joined(separator: "|")
        if !forceRefresh, let homeCache, homeCacheKey == key { return homeCache }
        homeCacheKey = key
        let pool = await publicPool(forceRefresh: forceRefresh)
        async let featuredRecipes = loadFeatured(pool: pool)
        async let timeOfDayRecipes = loadTimeOfDay(pool: pool)
        async let recommendations = self.recommended(signals: signals, uid: uid, category: nil)
        let content = HomeContent(
            featured: await featuredRecipes,
            timeOfDay: await timeOfDayRecipes,
            categories: Self.categories(in: pool),
            recommended: await recommendations
        )
        homeCache = content
        return content
    }

    private func loadFeatured(pool: [Recipe]) async -> [Recipe] {
        // Android queried "isPopular", a field that does not exist (Kotlin's isPopular is stored as
        // "popular"), so it always fell through to the random fallback.
        let query = FS.recipes
            .whereField("public", isEqualTo: true)
            .whereField("popular", isEqualTo: true)
            .limit(to: 10)
        if let snapshot = try? await FS.getDocuments(query) {
            let popular = FS.recipes(from: snapshot)
            if !popular.isEmpty { return popular.shuffled() }
        }
        let meal = MealType.forHour(Calendar.current.component(.hour, from: Date()))
        let timely = pool.filter { meal.storedValues.contains($0.mealType) }
        return Array((timely.count >= 5 ? timely : pool).shuffled().prefix(5))
    }

    private func loadTimeOfDay(pool: [Recipe]) async -> [Recipe] {
        let meal = MealType.forHour(Calendar.current.component(.hour, from: Date()))
        let query = FS.recipes
            .whereField("public", isEqualTo: true)
            .whereField("mealType", in: meal.storedValues)
            .limit(to: 12)
        if let snapshot = try? await FS.getDocuments(query) {
            let recipes = FS.recipes(from: snapshot)
            if !recipes.isEmpty { return recipes.shuffled() }
        }
        return Array(pool.filter { meal.storedValues.contains($0.mealType) }.shuffled().prefix(12))
    }

    /// Distinct categories of the pooled recipes. (Android downloaded every public recipe
    /// document just to build this list.)
    static func categories(in recipes: [Recipe]) -> [String] {
        let names = recipes
            .map { $0.category.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        return Array(Set(names)).sorted { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }
    }

    // MARK: - Recommendations

    /// "Recommended for you", optionally limited to one category (nil = all).
    func recommended(signals: RecommendationSignals, uid: String?, category: String?) async -> [Recipe] {
        var signals = signals
        if let uid {
            let activity = await recentActivity(uid: uid)
            signals.viewedSeconds = activity.viewed
            signals.searchQueries = activity.searches
        }

        var candidates = await publicPool()
        var seen = Set(candidates.map(\.id))
        for recipe in await flaggedCandidates(diets: signals.diets) where seen.insert(recipe.id).inserted {
            candidates.append(recipe)
        }
        if let category {
            candidates = candidates.filter { $0.category.caseInsensitiveCompare(category) == .orderedSame }
        }
        return RecommendationEngine.rank(candidates, signals: signals)
    }

    /// Extra candidates fetched with the diet flags as Firestore filters, so strict diets still
    /// get a full list when few pooled recipes qualify.
    private func flaggedCandidates(diets: [String]) async -> [Recipe] {
        let fields = diets.compactMap { DietFilter.firestoreField[$0] }.sorted()
        guard !fields.isEmpty else { return [] }
        let key = fields.joined(separator: ",")
        if let cached = dietCandidates[key] { return cached }
        var query: Query = FS.recipes.whereField("public", isEqualTo: true)
        for field in fields {
            query = query.whereField(field, isEqualTo: true)
        }
        guard let snapshot = try? await FS.getDocuments(query.limit(to: 100)) else { return [] }
        let recipes = FS.recipes(from: snapshot)
        dietCandidates[key] = recipes
        return recipes
    }

    private func recentActivity(uid: String) async -> (viewed: [String: Int], searches: [String]) {
        let query = FS.activityLog(uid).order(by: "timestamp", descending: true).limit(to: 50)
        guard let snapshot = try? await FS.getDocuments(query) else { return ([:], []) }
        var viewed: [String: Int] = [:]
        var searches: [String] = []
        for document in snapshot.documents {
            guard let type = document.get("type") as? String, let value = document.get("value") as? String else { continue }
            switch type {
            case UserActivity.Kind.viewRecipe.rawValue:
                let seconds = (document.get("durationSeconds") as? NSNumber)?.intValue ?? 0
                viewed[value] = max(viewed[value] ?? 0, seconds)
            case UserActivity.Kind.searchQuery.rawValue:
                searches.append(value)
            default:
                break
            }
        }
        return (viewed, searches)
    }

    // MARK: - Discover

    func discoverPage(
        sort: DiscoverSort,
        diets: [String],
        signals: RecommendationSignals,
        uid: String?,
        reset: Bool
    ) async -> DiscoverPage {
        if sort == .recommended {
            return DiscoverPage(recipes: await recommended(signals: signals, uid: uid, category: nil), hasMore: false)
        }
        if reset {
            discoverCursor[sort] = nil
            discoverOffset[sort] = 0
        }
        if discoverUsesLocalSort {
            return await locallySortedPage(sort: sort, diets: diets)
        }

        var collected: [Recipe] = []
        var hasMore = true
        // Strict diets can filter out most of a page; fetch a few more pages to fill the screen.
        var rounds = 0
        while collected.count < 10, hasMore, rounds < 4 {
            rounds += 1
            var query = FS.recipes
                .whereField("public", isEqualTo: true)
                .order(by: sort.field, descending: sort.descending)
            if let cursor = discoverCursor[sort] {
                query = query.start(afterDocument: cursor)
            }
            do {
                let snapshot = try await FS.getDocuments(query.limit(to: discoverPageSize))
                if let last = snapshot.documents.last { discoverCursor[sort] = last }
                hasMore = snapshot.documents.count == discoverPageSize
                collected += DietFilter.filter(FS.recipes(from: snapshot), diets: diets)
            } catch {
                // Almost always a missing composite index; deploy firestore.indexes.json to fix.
                // Until then, sort the cached pool on the device.
                print("Discover query failed, sorting locally instead: \(error)")
                discoverUsesLocalSort = true
                discoverOffset[sort] = 0
                return await locallySortedPage(sort: sort, diets: diets)
            }
        }
        return DiscoverPage(recipes: collected, hasMore: hasMore)
    }

    private func locallySortedPage(sort: DiscoverSort, diets: [String]) async -> DiscoverPage {
        let all = DietFilter.filter(await publicPool(), diets: diets)
        let sorted: [Recipe]
        switch sort {
        case .az, .recommended: sorted = all.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        case .za: sorted = all.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedDescending }
        case .cookTime: sorted = all.sorted { $0.timeInMins < $1.timeInMins }
        case .popular: sorted = all.sorted { ($0.isPopular ? 0 : 1, $0.title) < ($1.isPopular ? 0 : 1, $1.title) }
        }
        let start = discoverOffset[sort] ?? 0
        let end = min(start + discoverPageSize, sorted.count)
        guard start < end else { return DiscoverPage(recipes: [], hasMore: false) }
        discoverOffset[sort] = end
        return DiscoverPage(recipes: Array(sorted[start..<end]), hasMore: end < sorted.count)
    }

    // MARK: - Search

    /// Searches cached recipes first; only when nothing matches does it ask the recipe APIs,
    /// then stores what they return in Firestore for everyone.
    func search(_ text: String, diets: [String]) async -> [Recipe] {
        let query = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return [] }
        let local = DietFilter.filter(await publicPool().filter { Self.recipe($0, matches: query) }, diets: diets)
        guard local.isEmpty, NetworkMonitor.currentlyOnline, AppConfig.hasRecipeAPIs else { return local }

        async let spoonacular = try? api.spoonacularSearch(query, number: 10)
        async let tasty = try? api.tastyList(query: query, size: 10)
        let fetched = Self.unique((await spoonacular ?? []) + (await tasty ?? []))
        await store(fetched)
        return DietFilter.filter(fetched, diets: diets)
    }

    static func recipe(_ recipe: Recipe, matches query: String) -> Bool {
        let options: String.CompareOptions = [.caseInsensitive, .diacriticInsensitive]
        return recipe.title.range(of: query, options: options) != nil
            || recipe.ingredients.contains { $0.name.range(of: query, options: options) != nil }
    }

    // MARK: - Scan

    func matches(for ingredients: [String]) async -> [RecipeMatch] {
        IngredientMatcher.matches(for: ingredients, in: await publicPool())
    }

    /// Lower-case words that appear in ingredient names; used to pick ingredients out of OCR text.
    func ingredientVocabulary() async -> Set<String> {
        var words = Set<String>()
        for recipe in await publicPool() {
            for ingredient in recipe.ingredients {
                for word in ingredient.name.lowercased().split(whereSeparator: { !$0.isLetter }) where word.count >= 3 {
                    words.insert(String(word))
                }
            }
        }
        return words
    }

    // MARK: - Single recipes

    /// Loads a recipe; falls back to the user's saved favourite copy, which is how favourites
    /// stay readable offline or after the original is deleted.
    func recipe(id: String, uid: String?) async -> Recipe? {
        if let document = try? await FS.getDocument(FS.recipes.document(id)), let recipe = FS.recipe(from: document) {
            return recipe
        }
        if let uid, let document = try? await FS.getDocument(FS.favorites(uid).document(id)) {
            return FS.recipe(from: document)
        }
        return pool.first { $0.id == id }
    }

    func save(_ recipe: Recipe) async throws {
        try await FS.set(try FS.encode(recipe), at: FS.recipes.document(recipe.id))
        pool.removeAll { $0.id == recipe.id }
        if recipe.isPublic { pool.append(recipe) }
        homeCache = nil
    }

    func delete(_ recipe: Recipe) async throws {
        try await FS.delete(FS.recipes.document(recipe.id))
        pool.removeAll { $0.id == recipe.id }
        homeCache = nil
    }

    // MARK: - Recipe APIs

    /// Used only when the recipes collection is empty (for example a brand-new Firebase project).
    /// Android called the APIs for ~75 tags on every launch; this makes at most six requests.
    private func importStarterRecipes() async -> [Recipe] {
        let tags = ["african", "dinner", "breakfast"]
        var fetched: [Recipe] = []
        for tag in tags {
            // One tag per request: include-tags requires *all* listed tags, so Android's
            // comma-joined list of seven tags usually matched nothing.
            if let recipes = try? await api.spoonacularRandom(tags: [tag], number: 10) { fetched += recipes }
            if let recipes = try? await api.tastyList(query: tag, size: 10) { fetched += recipes }
        }
        let unique = Self.unique(fetched)
        await store(unique)
        return unique
    }

    /// Saves API results to Firestore in one batch so other users find them without an API call.
    private func store(_ recipes: [Recipe]) async {
        guard !recipes.isEmpty else { return }
        let batch = FS.db.batch()
        for recipe in recipes.prefix(450) {
            if let data = try? FS.encode(recipe) {
                batch.setData(data, forDocument: FS.recipes.document(recipe.id))
            }
        }
        do {
            try await FS.commit(batch)
        } catch {
            print("Caching API recipes failed: \(error)")
        }
        let known = Set(pool.map(\.id))
        pool += recipes.filter { !known.contains($0.id) }
    }

    static func unique(_ recipes: [Recipe]) -> [Recipe] {
        var seen = Set<String>()
        return recipes.filter { seen.insert($0.id).inserted }
    }
}
