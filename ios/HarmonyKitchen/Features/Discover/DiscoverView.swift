import SwiftUI

struct DiscoverView: View {
    @Environment(AppServices.self) private var services
    @Environment(UserStore.self) private var userStore
    @Environment(NetworkMonitor.self) private var network

    @State private var sort: DiscoverSort = .az
    @State private var recipes: [Recipe] = []
    @State private var hasMore = true
    @State private var isLoading = true
    @State private var isLoadingMore = false

    @State private var searchText = ""
    @State private var searchResults: [Recipe]?
    @State private var isSearching = false

    /// Reloads when the sort or the user's diets change.
    private var loadKey: String { "\(sort.rawValue)|\(userStore.diets.joined(separator: ","))" }

    var body: some View {
        ScrollView {
            Group {
                if let searchResults {
                    searchContent(searchResults)
                } else if isLoading {
                    ProgressView().frame(maxWidth: .infinity, minHeight: 300)
                } else if recipes.isEmpty {
                    ContentUnavailableView(
                        network.isOnline ? L("ios_no_recipes_title") : L("you_are_offline"),
                        systemImage: network.isOnline ? "fork.knife" : "wifi.slash",
                        description: Text(network.isOnline ? L("ios_pull_to_refresh") : L("check_your_connection_and_try_again"))
                    )
                } else {
                    VStack(spacing: 18) {
                        RecipeGrid(recipes: recipes)
                        if hasMore {
                            ProgressView()
                                .frame(maxWidth: .infinity, minHeight: 60)
                                .onAppear { Task { await loadMore() } }
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle(L("discover_recipes"))
        .searchable(text: $searchText, prompt: L("search_recipes"))
        .onSubmit(of: .search) { Task { await runSearch() } }
        .onChange(of: searchText) { _, text in
            if text.isEmpty { searchResults = nil }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Picker(L("ios_sort_by"), selection: $sort) {
                        ForEach(DiscoverSort.allCases) { option in
                            Text(option.title).tag(option)
                        }
                    }
                } label: {
                    Label(L("ios_sort_by"), systemImage: "arrow.up.arrow.down.circle")
                }
            }
        }
        .refreshable {
            if searchResults == nil { await reload() } else { await runSearch() }
        }
        .task(id: loadKey) {
            await reload()
        }
    }

    @ViewBuilder
    private func searchContent(_ results: [Recipe]) -> some View {
        if isSearching {
            ProgressView().frame(maxWidth: .infinity, minHeight: 300)
        } else if results.isEmpty {
            ContentUnavailableView.search(text: searchText)
        } else {
            RecipeGrid(recipes: results)
        }
    }

    private func reload() async {
        isLoading = recipes.isEmpty
        let page = await services.repository.discoverPage(
            sort: sort,
            diets: userStore.diets,
            signals: userStore.recommendationSignals,
            uid: services.uid,
            reset: true
        )
        recipes = page.recipes
        hasMore = page.hasMore
        isLoading = false
    }

    private func loadMore() async {
        guard hasMore, !isLoadingMore, !isLoading else { return }
        isLoadingMore = true
        let page = await services.repository.discoverPage(
            sort: sort,
            diets: userStore.diets,
            signals: userStore.recommendationSignals,
            uid: services.uid,
            reset: false
        )
        let known = Set(recipes.map(\.id))
        recipes += page.recipes.filter { !known.contains($0.id) }
        hasMore = page.hasMore
        isLoadingMore = false
    }

    private func runSearch() async {
        let query = searchText.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { return }
        isSearching = true
        searchResults = []
        // Android defined SEARCH_QUERY for recommendations but never logged it.
        userStore.logActivity(.searchQuery, value: query.lowercased())
        searchResults = await services.repository.search(query, diets: userStore.diets)
        isSearching = false
    }
}
