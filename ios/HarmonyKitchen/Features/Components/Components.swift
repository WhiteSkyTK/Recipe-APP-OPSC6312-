import PhotosUI
import SwiftUI
import UIKit

// MARK: - Navigation

/// Screens reachable from any tab's NavigationStack.
enum AppRoute: Hashable {
    case recipe(id: String, missingIngredients: [String] = [])
    case favorites
    case notifications
    case myRecipes
    case badges
    case preferences
    case editProfile
    case about
    case editCountry
    case editCuisines
    case editDiets
}

extension View {
    func appDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case let .recipe(id, missing): RecipeDetailView(recipeID: id, missingIngredients: missing)
            case .favorites: FavoritesView()
            case .notifications: NotificationsView()
            case .myRecipes: MyRecipesView()
            case .badges: BadgesView()
            case .preferences: PreferencesView()
            case .editProfile: EditProfileView()
            case .about: AboutView()
            case .editCountry: CountryPickerView(mode: .edit)
            case .editCuisines: PreferenceGridView(kind: .cuisines, mode: .edit)
            case .editDiets: PreferenceGridView(kind: .diets, mode: .edit)
            }
        }
    }
}

// MARK: - Recipe cards

/// Grid card: photo, title, cook time and a favourite heart.
struct RecipeCard: View {
    let recipe: Recipe

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RemoteImage(urlString: recipe.imageUrl, maxPixelSize: 500)
                .aspectRatio(4 / 3, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(alignment: .topTrailing) {
                    FavoriteButton(recipe: recipe)
                        .padding(6)
                }
            Text(recipe.title)
                .font(.subheadline.weight(.semibold))
                .lineLimit(2, reservesSpace: true)
                .multilineTextAlignment(.leading)
                .foregroundStyle(.primary)
            CookTimeLabel(minutes: recipe.timeInMins)
        }
        .contentShape(Rectangle())
    }
}

struct CookTimeLabel: View {
    let minutes: Int

    var body: some View {
        Label("\(minutes) Min", systemImage: "clock")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
}

/// Large card for the Featured carousel.
struct FeaturedCard: View {
    let recipe: Recipe

    var body: some View {
        RemoteImage(urlString: recipe.imageUrl, maxPixelSize: 1000)
            .overlay {
                LinearGradient(colors: [.clear, .black.opacity(0.75)], startPoint: .center, endPoint: .bottom)
            }
            .overlay(alignment: .bottomLeading) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(recipe.title)
                        .font(.title3.bold())
                        .lineLimit(2)
                    if !recipe.shortDescription.isEmpty {
                        Text(recipe.shortDescription)
                            .font(.footnote)
                            .lineLimit(2)
                            .opacity(0.9)
                    }
                    Label("\(recipe.timeInMins) Min", systemImage: "clock")
                        .font(.caption.weight(.medium))
                }
                .foregroundStyle(.white)
                .padding()
            }
            .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct FavoriteButton: View {
    @Environment(UserStore.self) private var userStore
    let recipe: Recipe

    var body: some View {
        let isFavorite = userStore.isFavorite(recipe.id)
        Button {
            Task { await userStore.toggleFavorite(recipe) }
        } label: {
            Image(systemName: isFavorite ? "heart.fill" : "heart")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(isFavorite ? .red : .primary)
                .frame(width: 32, height: 32)
                .background(.thinMaterial, in: Circle())
                .contentTransition(.symbolEffect(.replace))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L("toggle_favorite"))
        .sensoryFeedback(.selection, trigger: isFavorite)
    }
}

/// Two-column grid of recipe cards that open the detail screen.
struct RecipeGrid: View {
    let recipes: [Recipe]

    private let columns = [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 18) {
            ForEach(recipes) { recipe in
                NavigationLink(value: AppRoute.recipe(id: recipe.id)) {
                    RecipeCard(recipe: recipe)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct SectionTitle: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.title3.bold())
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Status views

struct OfflineBanner: View {
    var body: some View {
        Label(L("you_are_offline"), systemImage: "wifi.slash")
            .font(.footnote.weight(.semibold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
            .background(Color.orange)
    }
}

struct LoadingOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.25).ignoresSafeArea()
            ProgressView()
                .controlSize(.large)
                .padding(24)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct ToastOverlay: View {
    @Environment(ToastCenter.self) private var toasts

    var body: some View {
        ZStack {
            if let toast = toasts.current {
                toastView(toast)
            }
        }
        .animation(.spring(duration: 0.3), value: toasts.current?.id)
    }

    private func toastView(_ toast: ToastCenter.Toast) -> some View {
        HStack(spacing: 12) {
            Text(toast.message)
                .font(.subheadline)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let title = toast.actionTitle {
                Button(title) { toasts.performAction() }
                    .font(.subheadline.bold())
                    .foregroundStyle(Color.welcomeButtonLight)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.black.opacity(0.85), in: RoundedRectangle(cornerRadius: 14))
        .padding(.horizontal)
        .padding(.bottom, 70)
        .transition(.move(edge: .bottom).combined(with: .opacity))
        .id(toast.id)
        .onTapGesture { toasts.dismiss() }
    }
}

// MARK: - Buttons

struct PrimaryButtonStyle: ButtonStyle {
    var background: Color = .welcomeButtonDark
    var foreground: Color = .white
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 52)
            .foregroundStyle(foreground)
            .background(background.opacity(isEnabled ? 1 : 0.4), in: RoundedRectangle(cornerRadius: 14))
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

// MARK: - Chips

struct Chip: View {
    let title: String
    var isSelected = false
    var onRemove: (() -> Void)?

    var body: some View {
        HStack(spacing: 6) {
            Text(title).font(.subheadline)
            if let onRemove {
                Button(action: onRemove) {
                    Image(systemName: "xmark.circle.fill")
                }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .accessibilityLabel(L("remove_ingredient"))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(isSelected ? Color.chipSelected : Color.chip, in: Capsule())
        .foregroundStyle(isSelected ? .white : .primary)
    }
}

/// Lays out children left to right, wrapping onto new lines (for ingredient chips).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(proposal: proposal, subviews: subviews)
        let height = rows.last.map { $0.y + $0.height } ?? 0
        return CGSize(width: proposal.width ?? rows.map(\.width).max() ?? 0, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for row in arrange(proposal: proposal, subviews: subviews) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: bounds.minY + row.y), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
        }
    }

    private struct Row {
        var indices: [Int] = []
        var y: CGFloat = 0
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> [Row] {
        let maxWidth = proposal.width ?? .infinity
        var rows: [Row] = []
        var current = Row()
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            if !current.indices.isEmpty, current.width + spacing + size.width > maxWidth {
                rows.append(current)
                current = Row(y: current.y + current.height + spacing)
            }
            current.width = current.indices.isEmpty ? size.width : current.width + spacing + size.width
            current.height = max(current.height, size.height)
            current.indices.append(index)
        }
        if !current.indices.isEmpty { rows.append(current) }
        return rows
    }
}

// MARK: - Photo picking

/// Adds the Android "Take Photo / Choose from Gallery / Remove Photo" choice to a view.
struct PhotoSourceModifier: ViewModifier {
    @Binding var isPresented: Bool
    @Binding var image: UIImage?
    var title: String
    var allowsRemove: Bool

    @State private var showCamera = false
    @State private var showLibrary = false
    @State private var pickerItem: PhotosPickerItem?

    func body(content: Content) -> some View {
        content
            .confirmationDialog(title, isPresented: $isPresented, titleVisibility: .visible) {
                if UIImagePickerController.isSourceTypeAvailable(.camera) {
                    Button(L("dialog_option_take_photo")) { showCamera = true }
                }
                Button(L("dialog_option_choose_gallery")) { showLibrary = true }
                if allowsRemove {
                    Button(L("dialog_option_remove_photo"), role: .destructive) { image = nil }
                }
            }
            .photosPicker(isPresented: $showLibrary, selection: $pickerItem, matching: .images)
            .onChange(of: pickerItem) { _, item in
                guard let item else { return }
                Task {
                    if let data = try? await item.loadTransferable(type: Data.self), let picked = UIImage(data: data) {
                        image = ImageUploader.resized(picked, maxDimension: 2000)
                    }
                    pickerItem = nil
                }
            }
            .fullScreenCover(isPresented: $showCamera) {
                CameraPicker(image: $image) { showCamera = false }
                    .ignoresSafeArea()
            }
    }
}

extension View {
    func photoSourceDialog(isPresented: Binding<Bool>, image: Binding<UIImage?>, title: String, allowsRemove: Bool = true) -> some View {
        modifier(PhotoSourceModifier(isPresented: isPresented, image: image, title: title, allowsRemove: allowsRemove))
    }
}

/// The system camera (SwiftUI has no camera view).
struct CameraPicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    /// Called after a photo is taken or the camera is cancelled; the presenter closes the camera.
    var onFinish: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let controller = UIImagePickerController()
        controller.sourceType = .camera
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: UIImagePickerController, context: Context) {
        context.coordinator.parent = self
    }

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        var parent: CameraPicker

        init(parent: CameraPicker) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let photo = info[.originalImage] as? UIImage {
                parent.image = ImageUploader.resized(photo, maxDimension: 2000)
            }
            parent.onFinish()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onFinish()
        }
    }
}
