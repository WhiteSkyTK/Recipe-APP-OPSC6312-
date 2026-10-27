import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import FirebaseMessaging
import Foundation
import Observation
import UIKit
import UserNotifications

/// Owns the app-wide services and starts/stops the per-user stores as the user signs in and out.
@Observable
@MainActor
final class AppServices {
    enum AuthState: Equatable {
        case unknown
        case signedOut
        case signedIn(uid: String)
    }

    let network = NetworkMonitor()
    let toasts = ToastCenter()
    let repository = RecipeRepository()
    let userStore = UserStore()
    let shopping = ShoppingStore()
    let scanHistory = ScanHistoryStore()

    private(set) var authState: AuthState = .unknown

    @ObservationIgnored private var authHandle: AuthStateDidChangeListenerHandle?

    init() {
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            let uid = user?.uid
            Task { @MainActor in self?.authChanged(uid: uid) }
        }
    }

    var uid: String? {
        if case .signedIn(let uid) = authState { return uid }
        return nil
    }

    private func authChanged(uid: String?) {
        guard let uid else {
            if authState != .signedOut {
                userStore.stop()
                shopping.stop()
                let repository = repository
                Task { await repository.invalidateCaches() }
            }
            authState = .signedOut
            return
        }
        guard authState != .signedIn(uid: uid) else { return }
        authState = .signedIn(uid: uid)
        userStore.start(uid: uid)
        shopping.start(uid: uid)
        PushService.shared.userDidSignIn(uid: uid)
        let gamification = userStore.gamification
        Task.detached(priority: .utility) {
            await gamification.recordLogin(uid: uid)
        }
    }

    /// Signs out and clears data kept only on this device.
    func signOut() {
        AuthService.signOut()
        scanHistory.clear()
    }
}

/// Starts Firebase once; safe to call more than once.
enum FirebaseBootstrap {
    @MainActor
    static func configureIfNeeded() -> Bool {
        guard AppConfig.hasFirebaseConfig, !AppConfig.isRunningUnitTests else { return false }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
            FS.configureCache()
            PushService.shared.configure()
        }
        return true
    }
}

/// Push notifications through Firebase Cloud Messaging (the Android "new_recipes" topic).
///
/// Real pushes need the Push Notifications capability and an APNs key in the Firebase console
/// (see README). Without them registration fails quietly and in-app notifications still work.
final class PushService: NSObject, UNUserNotificationCenterDelegate, MessagingDelegate {
    static let shared = PushService()
    static let topic = "new_recipes"

    private var uid: String?
    private var token: String?

    func configure() {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
    }

    /// Asks for permission (Android 13+ asked on the main screen too) and applies the topic setting.
    func requestAuthorization() async {
        let granted = (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])) ?? false
        if granted {
            await MainActor.run { UIApplication.shared.registerForRemoteNotifications() }
        }
        let enabled = UserDefaults.standard.object(forKey: SettingsKey.notificationsEnabled) as? Bool ?? true
        setTopicSubscription(enabled)
    }

    func setTopicSubscription(_ enabled: Bool) {
        if enabled {
            Messaging.messaging().subscribe(toTopic: Self.topic) { _ in }
        } else {
            Messaging.messaging().unsubscribe(fromTopic: Self.topic) { _ in }
        }
    }

    func userDidSignIn(uid: String) {
        self.uid = uid
        saveToken()
    }

    private func saveToken() {
        guard let uid, let token else { return }
        FS.user(uid).setData(["fcmToken": token], merge: true, completion: nil)
    }

    // MARK: MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        token = fcmToken
        saveToken()
    }

    // MARK: UNUserNotificationCenterDelegate

    /// Shows pushes while the app is open and copies them into the in-app notification list,
    /// as the Android FirebaseMessagingService did.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        let content = notification.request.content
        if let uid {
            await GamificationEngine.createNotification(
                uid: uid,
                title: content.title.isEmpty ? "New Notification" : content.title,
                message: content.body.isEmpty ? "Check it out!" : content.body,
                iconName: "ic_notification"
            )
        }
        return [.banner, .list, .sound]
    }
}
