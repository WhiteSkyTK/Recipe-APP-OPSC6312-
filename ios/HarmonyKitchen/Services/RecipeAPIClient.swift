import Foundation

/// Spoonacular and Tasty (RapidAPI) clients.
///
/// These are only used when Firestore has nothing for a search, never on app start.
/// (The Android app called them from every launch while "seeding" the database.)
///
/// Note: any key shipped inside an app can be extracted from it. For production, move these calls
/// into a Cloud Function and keep the keys on the server.
actor RecipeAPIClient {
    enum APIError: Error {
        case notConfigured
        case http(Int)
    }

    private let session: URLSession
    private var spoonacularKeyIndex = 0

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 15
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        session = URLSession(configuration: configuration)
    }

    // MARK: Spoonacular

    func spoonacularRandom(tags: [String], number: Int) async throws -> [Recipe] {
        let data = try await spoonacular(path: "recipes/random", query: [
            URLQueryItem(name: "number", value: String(number)),
            URLQueryItem(name: "include-tags", value: tags.joined(separator: ",")),
        ])
        let response = try JSONDecoder().decode(SpoonacularRandomResponse.self, from: data)
        return response.recipes.map(RecipeMapper.recipe(from:))
    }

    /// complexSearch only returns IDs and titles, so the full recipes come from informationBulk.
    /// (Android decoded complexSearch results as full recipes; the missing ingredient lists made
    /// every Spoonacular search fail.)
    func spoonacularSearch(_ text: String, number: Int) async throws -> [Recipe] {
        let searchData = try await spoonacular(path: "recipes/complexSearch", query: [
            URLQueryItem(name: "query", value: text),
            URLQueryItem(name: "number", value: String(number)),
        ])
        let ids = try JSONDecoder().decode(SpoonacularSearchResponse.self, from: searchData).results.map(\.id)
        guard !ids.isEmpty else { return [] }
        let bulkData = try await spoonacular(path: "recipes/informationBulk", query: [
            URLQueryItem(name: "ids", value: ids.map(String.init).joined(separator: ",")),
        ])
        return try JSONDecoder().decode([SpoonacularRecipe].self, from: bulkData).map(RecipeMapper.recipe(from:))
    }

    /// Tries each configured key in turn, moving on when one is rejected or out of quota.
    private func spoonacular(path: String, query: [URLQueryItem]) async throws -> Data {
        let keys = AppConfig.spoonacularKeys
        guard !keys.isEmpty else { throw APIError.notConfigured }
        var lastError: Error = APIError.notConfigured
        for _ in 0..<keys.count {
            let key = keys[spoonacularKeyIndex % keys.count]
            var components = URLComponents(string: "https://api.spoonacular.com/\(path)")!
            components.queryItems = query + [URLQueryItem(name: "apiKey", value: key)]
            let (data, response) = try await session.data(from: components.url!)
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            if (200..<300).contains(status) { return data }
            lastError = APIError.http(status)
            // 401 bad key, 402 daily quota used up, 429 rate limited: try the next key.
            guard [401, 402, 429].contains(status) else { throw lastError }
            spoonacularKeyIndex += 1
        }
        throw lastError
    }

    // MARK: Tasty

    func tastyList(query: String, size: Int) async throws -> [Recipe] {
        let key = AppConfig.rapidAPIKey
        guard !key.isEmpty else { throw APIError.notConfigured }
        var components = URLComponents(string: "https://tasty.p.rapidapi.com/recipes/list")!
        components.queryItems = [
            URLQueryItem(name: "from", value: "0"),
            URLQueryItem(name: "size", value: String(size)),
            URLQueryItem(name: "q", value: query),
        ]
        var request = URLRequest(url: components.url!)
        request.setValue(key, forHTTPHeaderField: "x-rapidapi-key")
        request.setValue("tasty.p.rapidapi.com", forHTTPHeaderField: "x-rapidapi-host")
        let (data, response) = try await session.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(status) else { throw APIError.http(status) }
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try decoder.decode(TastyResponse.self, from: data).results.compactMap(RecipeMapper.recipe(from:))
    }
}
