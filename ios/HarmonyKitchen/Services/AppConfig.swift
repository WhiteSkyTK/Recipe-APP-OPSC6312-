import Foundation

/// Values injected at build time through Config/*.xcconfig -> Info.plist.
enum AppConfig {
    /// Spoonacular keys, tried in order; the client moves to the next key when one is out of quota.
    static let spoonacularKeys: [String] = infoString("SpoonacularAPIKeys")
        .split(separator: ",")
        .map { $0.trimmingCharacters(in: .whitespaces) }
        .filter { !$0.isEmpty }

    /// RapidAPI key for the Tasty API.
    static let rapidAPIKey: String = infoString("RapidAPIKey").trimmingCharacters(in: .whitespaces)

    static var hasRecipeAPIs: Bool { !spoonacularKeys.isEmpty || !rapidAPIKey.isEmpty }

    /// Firebase can only start when GoogleService-Info.plist is bundled.
    static var hasFirebaseConfig: Bool {
        Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil
    }

    static var isRunningUnitTests: Bool {
        ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil
    }

    static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }

    static let supportEmail = "whiteshytk@gmail.com"

    private static func infoString(_ key: String) -> String {
        // Unset xcconfig variables arrive as "" or, in some setups, as the literal "$(NAME)".
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String, !value.hasPrefix("$(") else {
            return ""
        }
        return value
    }
}
