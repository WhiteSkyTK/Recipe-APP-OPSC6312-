import FirebaseCore
import FirebaseMessaging
import GoogleSignIn
import SwiftUI
import UIKit

@main
struct HarmonyKitchenApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            AppRoot()
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        _ = FirebaseBootstrap.configureIfNeeded()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        guard FirebaseApp.app() != nil else { return }
        Messaging.messaging().apnsToken = deviceToken
    }
}

/// Creates the services once Firebase is running and applies the appearance setting.
struct AppRoot: View {
    @State private var services: AppServices?
    @AppStorage(SettingsKey.themeMode) private var themeMode = ThemeMode.system.rawValue

    var body: some View {
        Group {
            if let services {
                RootView()
                    .withServices(services)
            } else if AppConfig.isRunningUnitTests {
                Color.clear
            } else if !AppConfig.hasFirebaseConfig {
                SetupRequiredView()
            } else {
                SplashView(animate: false)
            }
        }
        .preferredColorScheme(ThemeMode(rawValue: themeMode)?.colorScheme)
        .task {
            if services == nil, FirebaseBootstrap.configureIfNeeded() {
                services = AppServices()
            }
        }
        .onOpenURL { url in
            _ = GIDSignIn.sharedInstance.handle(url)
        }
    }
}

extension View {
    /// Puts every app service into the SwiftUI environment.
    func withServices(_ services: AppServices) -> some View {
        environment(services)
            .environment(services.network)
            .environment(services.toasts)
            .environment(services.userStore)
            .environment(services.shopping)
            .environment(services.scanHistory)
    }
}

/// Decides between splash, sign-in and the main app.
struct RootView: View {
    @Environment(AppServices.self) private var services
    @State private var splashFinished = false

    var body: some View {
        ZStack {
            if !splashFinished || services.authState == .unknown {
                SplashView(animate: true)
                    .transition(.opacity)
            } else if services.uid != nil {
                MainTabView()
                    .transition(.opacity)
            } else {
                WelcomeView()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.35), value: splashFinished)
        .animation(.easeInOut(duration: 0.35), value: services.authState)
        .overlay(alignment: .bottom) {
            ToastOverlay()
        }
        .task {
            await runSplash()
        }
    }

    /// Shows the splash for at least 1.2 s while warming the recipe cache.
    /// (Android held it for 3 s and then threw the preloaded data away, because every screen
    /// created its own repository.)
    private func runSplash() async {
        let start = Date()
        while services.authState == .unknown, Date().timeIntervalSince(start) < 5 {
            try? await Task.sleep(for: .milliseconds(50))
        }
        if services.uid != nil {
            // Wait briefly for the profile so the first recommendations respect the user's diets.
            while services.userStore.profile == nil, Date().timeIntervalSince(start) < 2.5 {
                try? await Task.sleep(for: .milliseconds(50))
            }
            let repository = services.repository
            // Do not hold the splash longer than 4 s on a slow connection; loading carries on.
            await waitAtMost(seconds: 4) {
                _ = await repository.publicPool()
            }
        }
        let elapsed = Date().timeIntervalSince(start)
        if elapsed < 1.2 {
            try? await Task.sleep(for: .seconds(1.2 - elapsed))
        }
        splashFinished = true
    }
}

/// Shown when the build has no GoogleService-Info.plist.
struct SetupRequiredView: View {
    var body: some View {
        ContentUnavailableView {
            Label(L("ios_setup_title"), systemImage: "wrench.and.screwdriver")
        } description: {
            Text(L("ios_setup_message"))
        }
    }
}
