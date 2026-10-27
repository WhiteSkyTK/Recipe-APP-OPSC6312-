import FirebaseFirestore
import Foundation

/// Badges and progress, stored in `users/{uid}/progress/main` exactly like the Android app.
struct GamificationEngine: Sendable {
    /// Android drawable names are kept as notification icon names so the Android app can show them.
    static let badges: [(badge: Badge, androidIcon: String)] = [
        (Badge(id: "chef_apprentice", title: "Chef's Apprentice", description: "Create your first recipe.", symbol: "fork.knife"), "ic_badge_first_recipe"),
        (Badge(id: "sous_chef", title: "Sous Chef", description: "Create 5 recipes.", symbol: "flame"), "ic_badge_sous_chef"),
        (Badge(id: "executive_chef", title: "Executive Chef", description: "Create 20 recipes.", symbol: "crown"), "ic_badge_executive_chef"),
        (Badge(id: "first_favorite", title: "First Favorite", description: "Save your first favorite recipe.", symbol: "heart"), "ic_badge_first_favorite"),
        (Badge(id: "curious_cook", title: "Curious Cook", description: "Save 10 favorite recipes.", symbol: "heart.circle"), "ic_badge_curious_cook"),
        (Badge(id: "recipe_collector", title: "Recipe Collector", description: "Save 50 favorite recipes.", symbol: "books.vertical"), "ic_badge_recipe_collector"),
        (Badge(id: "welcome_back", title: "Welcome Back!", description: "Log in two days in a row.", symbol: "calendar"), "ic_badge_login_streak_2"),
        (Badge(id: "dedicated_diner", title: "Dedicated Diner", description: "Log in for 7 consecutive days.", symbol: "star.circle"), "ic_badge_login_streak_7"),
    ]

    static var allBadges: [Badge] { badges.map(\.badge) }

    /// SF Symbol for a notification's `iconName` (which uses Android drawable names).
    static func symbol(forIcon iconName: String) -> String {
        if let match = badges.first(where: { $0.androidIcon == iconName }) { return match.badge.symbol }
        return iconName == "ic_notification" ? "bell.fill" : "exclamationmark.circle"
    }

    static func isEarned(_ badgeID: String, progress: UserProgress) -> Bool {
        switch badgeID {
        case "chef_apprentice": progress.recipesCreated >= 1
        case "sous_chef": progress.recipesCreated >= 5
        case "executive_chef": progress.recipesCreated >= 20
        case "first_favorite": progress.recipesFavorited >= 1
        case "curious_cook": progress.recipesFavorited >= 10
        case "recipe_collector": progress.recipesFavorited >= 50
        case "welcome_back": progress.loginStreak >= 2
        case "dedicated_diner": progress.loginStreak >= 7
        default: false
        }
    }

    // MARK: - Events

    func recordLogin(uid: String) async {
        let progress = await progress(uid: uid)
        let streak = LoginStreak.next(current: progress.loginStreak, lastLogin: progress.lastLogin)
        try? await FS.set([
            "userId": uid,
            "loginStreak": streak,
            "lastLogin": FieldValue.serverTimestamp(),
        ], at: FS.progress(uid), merge: true)
        var updated = progress
        updated.loginStreak = streak
        await awardBadges(uid: uid, progress: updated)
    }

    /// Only for brand-new recipes. (Android also counted every edit as a new recipe.)
    func recordRecipeCreated(uid: String) async {
        try? await FS.set(["recipesCreated": FieldValue.increment(Int64(1))], at: FS.progress(uid), merge: true)
        await awardBadges(uid: uid, progress: await progress(uid: uid))
    }

    func recordFavorite(uid: String, added: Bool) async {
        try? await FS.set(["recipesFavorited": FieldValue.increment(Int64(added ? 1 : -1))], at: FS.progress(uid), merge: true)
        if added { await awardBadges(uid: uid, progress: await progress(uid: uid)) }
    }

    func progress(uid: String) async -> UserProgress {
        guard let document = try? await FS.getDocument(FS.progress(uid)), let data = document.data() else {
            return UserProgress()
        }
        return UserProgress(
            recipesCreated: (data["recipesCreated"] as? NSNumber)?.intValue ?? 0,
            recipesFavorited: max((data["recipesFavorited"] as? NSNumber)?.intValue ?? 0, 0),
            loginStreak: (data["loginStreak"] as? NSNumber)?.intValue ?? 0,
            lastLogin: (data["lastLogin"] as? Timestamp)?.dateValue(),
            earnedBadgeIDs: data["earnedBadgeIds"] as? [String] ?? []
        )
    }

    private func awardBadges(uid: String, progress: UserProgress) async {
        let earned = Set(progress.earnedBadgeIDs)
        let newBadges = Self.badges.filter { !earned.contains($0.badge.id) && Self.isEarned($0.badge.id, progress: progress) }
        guard let first = newBadges.first else { return }
        try? await FS.set(
            ["earnedBadgeIds": FieldValue.arrayUnion(newBadges.map(\.badge.id))],
            at: FS.progress(uid),
            merge: true
        )
        await Self.createNotification(
            uid: uid,
            title: L("gamification_badge_unlocked"),
            message: LF("gamification_badge_earned_message", first.badge.title),
            iconName: first.androidIcon
        )
    }

    /// Adds an in-app notification. Writes both `isRead` and `read`: Android reads the latter.
    static func createNotification(uid: String, title: String, message: String, iconName: String) async {
        try? await FS.set([
            "title": title,
            "message": message,
            "iconName": iconName,
            "isRead": false,
            "read": false,
            "timestamp": FieldValue.serverTimestamp(),
        ], at: FS.notifications(uid).document())
    }
}
