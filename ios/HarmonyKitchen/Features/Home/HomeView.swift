import SwiftUI

struct HomeView: View {
    @Environment(AppServices.self) private var services
    @Environment(UserStore.self) private var userStore
    @Environment(NetworkMonitor.self) private var network
    @AppStorage(SettingsKey.useAllCaps) private var useAllCaps = true

    @State private var content = HomeContent()
    @State private var hasLoaded = false
    @State private var selectedCategory: String?
    @State private var categoryRecipes: [Recipe]?
    @State private var isLoadingCategory = false
    @State private var timeOfDayTitle = Greeting.randomLine()
    @State private var showEditor = false

    /// Reloads when the user or their preferences change.
    private var preferenceKey: String {
        [services.uid ?? "", userStore.diets.joined(separator: ","), userStore.cuisines.joined(separator: ",")]
            .joined(separator: "|")
    }

    private var greeting: String {
        let name = userStore.profile?.displayName ?? ""
        let shown = name.isEmpty ? L("main_greeting_default_user") : Greeting.displayName(name, allCaps: useAllCaps)
        return "\(Greeting.header())\n\(shown)"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                Text(greeting)
                    .font(.title.bold())
                    .contentTransition(.opacity)

                if !hasLoaded {
                    ProgressView()
                        .frame(maxWidth: .infinity, minHeight: 240)
                } else if content.featured.isEmpty && content.recommended.isEmpty && content.timeOfDay.isEmpty {
                    emptyState
                } else {
                    featuredSection
                    timeOfDaySection
                    categorySection
                    recommendedSection
                }
            }
            .padding()
        }
        .refreshable { await load(force: true) }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarItems }
        .sheet(isPresented: $showEditor) {
            RecipeEditorView(editing: nil)
        }
        .task(id: preferenceKey) {
            await load(force: false)
        }
        .onAppear {
            timeOfDayTitle = Greeting.randomLine()
        }
    }

    // MARK: Sections

    @ViewBuilder
    private var featuredSection: some View {
        if !content.featured.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                SectionTitle(text: L("featured"))
                TabView {
                    ForEach(content.featured) { recipe in
                        NavigationLink(value: AppRoute.recipe(id: recipe.id)) {
                            FeaturedCard(recipe: recipe)
                                .padding(.horizontal, 2)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))
                .frame(height: 230)
            }
        }
    }

    @ViewBuilder
    private var timeOfDaySection: some View {
        if !content.timeOfDay.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                SectionTitle(text: timeOfDayTitle)
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 14) {
                        ForEach(content.timeOfDay) { recipe in
                            NavigationLink(value: AppRoute.recipe(id: recipe.id)) {
                                RecipeCard(recipe: recipe)
                                    .frame(width: 165)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var categorySection: some View {
        if !content.categories.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                SectionTitle(text: L("categoryhome"))
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Button { select(category: nil) } label: {
                            Chip(title: L("category_all"), isSelected: selectedCategory == nil)
                        }
                        ForEach(content.categories, id: \.self) { category in
                            Button { select(category: category) } label: {
                                Chip(title: category, isSelected: selectedCategory == category)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var recommendedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(text: L("recommended_recipes"))
            let recipes = categoryRecipes ?? content.recommended
            if isLoadingCategory {
                ProgressView().frame(maxWidth: .infinity, minHeight: 120)
            } else if recipes.isEmpty {
                Text(L("ios_no_recommendations"))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, minHeight: 80)
            } else {
                RecipeGrid(recipes: recipes)
            }
        }
    }

    @ViewBuilder
    private var emptyState: some View {
        if network.isOnline {
            ContentUnavailableView(L("ios_no_recipes_title"), systemImage: "fork.knife", description: Text(L("ios_pull_to_refresh")))
        } else {
            ContentUnavailableView(L("you_are_offline"), systemImage: "wifi.slash", description: Text(L("check_your_connection_and_try_again")))
        }
    }

    @ToolbarContentBuilder
    private var toolbarItems: some ToolbarContent {
        ToolbarItemGroup(placement: .topBarTrailing) {
            NavigationLink(value: AppRoute.favorites) {
                Image(systemName: "heart")
            }
            .accessibilityLabel(L("my_favorites"))
            NavigationLink(value: AppRoute.notifications) {
                Image(systemName: "bell")
                    .overlay(alignment: .topTrailing) {
                        if userStore.hasUnreadNotifications {
                            Circle().fill(.red).frame(width: 9, height: 9).offset(x: 3, y: -2)
                        }
                    }
            }
            .accessibilityLabel(L("notifications"))
            Button { showEditor = true } label: {
                Image(systemName: "plus.circle")
            }
            .accessibilityLabel(L("add_your_recipe"))
        }
    }

    // MARK: Loading

    private func load(force: Bool) async {
        let signals = userStore.recommendationSignals
        let uid = services.uid
        content = await services.repository.homeContent(signals: signals, uid: uid, forceRefresh: force)
        hasLoaded = true
        if let selectedCategory {
            categoryRecipes = await services.repository.recommended(signals: signals, uid: uid, category: selectedCategory)
        }
    }

    private func select(category: String?) {
        guard category != selectedCategory else { return }
        selectedCategory = category
        guard let category else {
            categoryRecipes = nil
            return
        }
        isLoadingCategory = true
        let signals = userStore.recommendationSignals
        let uid = services.uid
        Task {
            let recipes = await services.repository.recommended(signals: signals, uid: uid, category: category)
            if selectedCategory == category { categoryRecipes = recipes }
            isLoadingCategory = false
        }
    }
}
