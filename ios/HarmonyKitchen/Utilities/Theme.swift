import SwiftUI
import UIKit

// Colours from the Android res/values/colors.xml, with dark-mode pairs from values-night.
extension Color {
    /// primary_blue
    static let brand = Color(hex: 0x88A0A8)
    /// light_card_background / dark_card_background
    static let card = Color(light: 0x90B5BB, dark: 0x3A4144)
    /// welcome_background
    static let welcomeBackground = Color(light: 0xE0EBED, dark: 0x1C1C1E)
    /// welcome_button_dark
    static let welcomeButtonDark = Color(light: 0x313742, dark: 0xE0EBED)
    /// welcome_button_light_blue
    static let welcomeButtonLight = Color(hex: 0xB0C4DE)
    /// light_chip_background / dark_chip_background
    static let chip = Color(light: 0xF0F0F0, dark: 0x3A3A3C)
    /// chip_selected_background_light
    static let chipSelected = Color(hex: 0x90B5BB)

    init(hex: UInt32) {
        self.init(uiColor: UIColor(hex: hex))
    }

    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

extension Font {
    /// The script face used for the app name on the splash screen.
    static func brandScript(size: CGFloat) -> Font {
        .custom("IslandMoments-Regular", size: size)
    }
}

/// Appearance preference; raw values are stored in UserDefaults under `SettingsKey.themeMode`.
enum ThemeMode: Int, CaseIterable, Identifiable {
    case system = 0
    case light = 1
    case dark = 2

    var id: Int { rawValue }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }

    var title: String {
        switch self {
        case .system: L("follow_system")
        case .light: L("light_mode")
        case .dark: L("dark_mode")
        }
    }
}

/// UserDefaults keys (names match the Android SharedPreferences keys).
enum SettingsKey {
    static let themeMode = "ThemeMode"
    static let useAllCaps = "UseAllCaps"
    static let unitSystem = "UnitSystem"
    static let notificationsEnabled = "NotificationsEnabled"
    static let hasSeenTour = "HasSeenTour"
}
