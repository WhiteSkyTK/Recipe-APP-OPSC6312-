import SwiftUI

struct MainTabView: View {
    enum Tab: Hashable {
        case home, discover, scan, shopping, profile
    }

    @Environment(NetworkMonitor.self) private var network
    @AppStorage(SettingsKey.hasSeenTour) private var hasSeenTour = false
    @State private var selection: Tab = .home
    @State private var showTour = false

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack { HomeView().appDestinations() }
                .tabItem { Label(L("home"), systemImage: "house") }
                .tag(Tab.home)
            NavigationStack { DiscoverView().appDestinations() }
                .tabItem { Label(L("discover"), systemImage: "safari") }
                .tag(Tab.discover)
            NavigationStack { ScanView().appDestinations() }
                .tabItem { Label(L("scan"), systemImage: "viewfinder") }
                .tag(Tab.scan)
            NavigationStack { ShoppingListView() }
                .tabItem { Label(L("items"), systemImage: "cart") }
                .tag(Tab.shopping)
            NavigationStack { ProfileView().appDestinations() }
                .tabItem { Label(L("profile"), systemImage: "person") }
                .tag(Tab.profile)
        }
        .tint(Color.brand)
        .safeAreaInset(edge: .top, spacing: 0) {
            if !network.isOnline { OfflineBanner() }
        }
        .sheet(isPresented: $showTour) {
            TourView {
                hasSeenTour = true
                showTour = false
            }
            .interactiveDismissDisabled()
        }
        .task {
            if !hasSeenTour {
                try? await Task.sleep(for: .milliseconds(500))
                showTour = true
            }
            await PushService.shared.requestAuthorization()
        }
    }
}

/// First-launch walkthrough (Android used ShowcaseView spotlights).
struct TourView: View {
    var onFinish: () -> Void
    @State private var page = 0

    private let pages: [(symbol: String, title: String, text: String)] = [
        ("house.fill", "onboarding_home_title", "onboarding_home_text"),
        ("safari.fill", "onboarding_discover_title", "onboarding_discover_text"),
        ("plus.circle.fill", "onboarding_add_recipe_title", "onboarding_add_recipe_text"),
        ("viewfinder", "onboarding_scan_title", "onboarding_scan_text"),
        ("cart.fill", "onboarding_cart_title", "onboarding_cart_text"),
        ("person.fill", "onboarding_profile_title", "onboarding_profile_text"),
    ]

    var body: some View {
        VStack(spacing: 24) {
            TabView(selection: $page) {
                ForEach(pages.indices, id: \.self) { index in
                    VStack(spacing: 20) {
                        Image(systemName: pages[index].symbol)
                            .font(.system(size: 72))
                            .foregroundStyle(Color.brand)
                        Text(L(pages[index].title))
                            .font(.title2.bold())
                            .multilineTextAlignment(.center)
                        Text(L(pages[index].text))
                            .font(.body)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(32)
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .always))

            Button(page == pages.count - 1 ? L("get_started") : L("continuebtn")) {
                if page == pages.count - 1 {
                    onFinish()
                } else {
                    withAnimation { page += 1 }
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 24)
            Button(L("skip"), action: onFinish)
                .padding(.bottom)
        }
        .presentationDetents([.large])
    }
}
