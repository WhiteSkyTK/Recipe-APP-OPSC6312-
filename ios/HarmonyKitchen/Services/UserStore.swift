import FirebaseFirestore
import Foundation
import Observation

/// The signed-in user's profile, favourites and notifications, kept live with Firestore listeners.
/// Firestore's offline cache replaces the Room copies the Android app kept and synced by hand.
@Observable
@MainActor
final class UserStore {
    private(set) var uid: String?
    private(set) var profile: UserProfile?
    private(set) var favorites: [Recipe] = []
    private(set) var favoriteIDs: Set<String> = []
    private(set) var notifications: [AppNotification] = []

    @ObservationIgnored private var listeners: [ListenerRegistration] = []
    @ObservationIgnored private var pendingFavorites: Set<String> = []
    let gamification = GamificationEngine()

    var hasUnreadNotifications: Bool { notifications.contains { !$0.isRead } }
    var diets: [String] { profile?.selectedDiets ?? [] }
    var cuisines: [String] { profile?.selectedCuisines ?? [] }

    var recommendationSignals: RecommendationSignals {
        RecommendationSignals(diets: diets, cuisines: cuisines, favoriteIDs: favoriteIDs)
    }

    func start(uid: String) {
        stop()
        self.uid = uid

        listeners.append(FS.user(uid).addSnapshotListener { [weak self] snapshot, _ in
            let profile = try? snapshot?.data(as: UserProfile.self)
            Task { @MainActor in self?.profile = profile }
        })

        listeners.append(FS.favorites(uid).addSnapshotListener { [weak self] snapshot, _ in
            guard let snapshot else { return }
            let recipes = FS.recipes(from: snapshot)
                .sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
            Task { @MainActor in
                self?.favorites = recipes
                self?.favoriteIDs = Set(recipes.map(\.id))
            }
        })

        let notificationsQuery = FS.notifications(uid).order(by: "timestamp", descending: true).limit(to: 100)
        listeners.append(notificationsQuery.addSnapshotListener { [weak self] snapshot, _ in
            guard let snapshot else { return }
            let items = snapshot.documents.map(Self.notification(from:))
            Task { @MainActor in self?.notifications = items }
        })
    }

    func stop() {
        listeners.forEach { $0.remove() }
        listeners = []
        uid = nil
        profile = nil
        favorites = []
        favoriteIDs = []
        notifications = []
    }

    nonisolated private static func notification(from document: QueryDocumentSnapshot) -> AppNotification {
        let data = document.data()
        return AppNotification(
            id: document.documentID,
            title: data["title"] as? String ?? "",
            message: data["message"] as? String ?? "",
            iconName: data["iconName"] as? String ?? "",
            timestamp: (data["timestamp"] as? Timestamp)?.dateValue(),
            // Android wrote "isRead" but its model read "read", so it never saw anything as read.
            isRead: data["isRead"] as? Bool ?? data["read"] as? Bool ?? false
        )
    }

    // MARK: - Favourites

    func isFavorite(_ recipeID: String) -> Bool {
        favoriteIDs.contains(recipeID)
    }

    func toggleFavorite(_ recipe: Recipe) async {
        guard let uid, !pendingFavorites.contains(recipe.id) else { return }
        pendingFavorites.insert(recipe.id)
        defer { pendingFavorites.remove(recipe.id) }

        let adding = !favoriteIDs.contains(recipe.id)
        // Update the heart at once; the listener confirms it.
        if adding { favoriteIDs.insert(recipe.id) } else { favoriteIDs.remove(recipe.id) }
        do {
            let ref = FS.favorites(uid).document(recipe.id)
            if adding {
                try await FS.set(try FS.encode(recipe), at: ref)
            } else {
                try await FS.delete(ref)
            }
            logActivity(adding ? .favoriteRecipe : .unfavoriteRecipe, value: recipe.id)
            // Counted once per tap (Android counted each favourite twice).
            await gamification.recordFavorite(uid: uid, added: adding)
        } catch {
            if adding { favoriteIDs.remove(recipe.id) } else { favoriteIDs.insert(recipe.id) }
        }
    }

    // MARK: - Activity

    func logActivity(_ kind: UserActivity.Kind, value: String, durationSeconds: Int? = nil) {
        guard let uid else { return }
        var data: [String: Any] = [
            "type": kind.rawValue,
            "value": value,
            "timestamp": FieldValue.serverTimestamp(),
        ]
        if let durationSeconds { data["durationSeconds"] = durationSeconds }
        FS.activityLog(uid).document().setData(data, completion: nil)
    }

    // MARK: - Notifications

    func markAllNotificationsRead() async {
        guard let uid else { return }
        let unread = notifications.filter { !$0.isRead }
        guard !unread.isEmpty else { return }
        let batch = FS.db.batch()
        for notification in unread {
            batch.updateData(["isRead": true, "read": true], forDocument: FS.notifications(uid).document(notification.id))
        }
        try? await FS.commit(batch)
    }

    func addNotification(title: String, message: String, iconName: String = "ic_notification") async {
        guard let uid else { return }
        await GamificationEngine.createNotification(uid: uid, title: title, message: message, iconName: iconName)
    }

    // MARK: - Profile

    func updateProfile(_ fields: [String: Any]) async throws {
        guard let uid else { return }
        try await FS.set(fields, at: FS.user(uid), merge: true)
    }
}
