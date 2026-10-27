import FirebaseFirestore
import SwiftUI

struct FavoritesView: View {
    @Environment(UserStore.self) private var userStore

    var body: some View {
        ScrollView {
            if userStore.favorites.isEmpty {
                ContentUnavailableView(
                    L("my_favorites"),
                    systemImage: "heart",
                    description: Text(L("you_haven_t_saved_any_favorites_yet_tap_the_on_a_recipe_to_add_it_here"))
                )
                .padding(.top, 80)
            } else {
                RecipeGrid(recipes: userStore.favorites).padding()
            }
        }
        .navigationTitle(L("my_favorites"))
    }
}

struct NotificationsView: View {
    @Environment(UserStore.self) private var userStore

    var body: some View {
        List(userStore.notifications) { notification in
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: GamificationEngine.symbol(forIcon: notification.iconName))
                    .font(.title3)
                    .foregroundStyle(Color.brand)
                    .frame(width: 32)
                VStack(alignment: .leading, spacing: 4) {
                    Text(notification.title).font(.headline)
                    if !notification.message.isEmpty {
                        Text(notification.message).font(.subheadline)
                    }
                    if let date = notification.timestamp {
                        Text(date.formatted(.dateTime.month(.abbreviated).day().hour().minute()))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                if !notification.isRead {
                    Spacer()
                    Circle().fill(.red).frame(width: 8, height: 8)
                }
            }
            .padding(.vertical, 4)
        }
        .listStyle(.plain)
        .overlay {
            if userStore.notifications.isEmpty {
                ContentUnavailableView(L("no_notifications_yet"), systemImage: "bell.slash")
            }
        }
        .navigationTitle(L("notifications"))
        .task {
            // Let the unread dots show briefly before clearing them.
            try? await Task.sleep(for: .seconds(1))
            await userStore.markAllNotificationsRead()
        }
    }
}

struct BadgesView: View {
    @Environment(UserStore.self) private var userStore
    @Environment(AppServices.self) private var services
    @State private var earned: Set<String>?

    var body: some View {
        List {
            ForEach(sortedBadges) { badge in
                let isEarned = earned?.contains(badge.id) ?? false
                HStack(spacing: 16) {
                    Image(systemName: badge.symbol)
                        .font(.title)
                        .foregroundStyle(isEarned ? Color.brand : .secondary)
                        .frame(width: 52, height: 52)
                        .background((isEarned ? Color.brand : Color.gray).opacity(0.15), in: Circle())
                    VStack(alignment: .leading, spacing: 4) {
                        Text(badge.title).font(.headline)
                        Text(badge.description).font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer()
                    if isEarned {
                        Image(systemName: "checkmark.seal.fill").foregroundStyle(.green)
                    }
                }
                .opacity(isEarned ? 1 : 0.5)
                .padding(.vertical, 4)
            }
        }
        .overlay { if earned == nil { ProgressView() } }
        .navigationTitle(L("my_badges_achievements"))
        .task {
            guard let uid = services.uid else { return }
            let progress = await userStore.gamification.progress(uid: uid)
            earned = Set(progress.earnedBadgeIDs)
        }
    }

    /// Earned badges first, as on Android.
    private var sortedBadges: [Badge] {
        let all = GamificationEngine.allBadges
        guard let earned else { return all }
        return all.filter { earned.contains($0.id) } + all.filter { !earned.contains($0.id) }
    }
}

/// The signed-in user's own recipes, live from Firestore (works offline from the cache).
@Observable
@MainActor
final class MyRecipesModel {
    private(set) var recipes: [Recipe]?
    @ObservationIgnored private var listener: ListenerRegistration?

    func start(uid: String) {
        guard listener == nil else { return }
        listener = FS.recipes.whereField("userId", isEqualTo: uid).addSnapshotListener { [weak self] snapshot, _ in
            guard let snapshot else { return }
            let recipes = FS.recipes(from: snapshot)
                .sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
            Task { @MainActor in self?.recipes = recipes }
        }
    }

    func stop() {
        listener?.remove()
        listener = nil
    }
}

struct MyRecipesView: View {
    @Environment(AppServices.self) private var services
    @State private var model = MyRecipesModel()
    @State private var showEditor = false

    var body: some View {
        ScrollView {
            if let recipes = model.recipes {
                if recipes.isEmpty {
                    ContentUnavailableView(L("my_creations"), systemImage: "book", description: Text(L("my_recipes_empty_state")))
                        .padding(.top, 80)
                } else {
                    RecipeGrid(recipes: recipes).padding()
                }
            } else {
                ProgressView().padding(.top, 120)
            }
        }
        .navigationTitle(L("my_recipes"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showEditor = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel(L("add_your_recipe"))
            }
        }
        .sheet(isPresented: $showEditor) {
            RecipeEditorView(editing: nil)
        }
        .onAppear {
            if let uid = services.uid { model.start(uid: uid) }
        }
        .onDisappear { model.stop() }
    }
}
