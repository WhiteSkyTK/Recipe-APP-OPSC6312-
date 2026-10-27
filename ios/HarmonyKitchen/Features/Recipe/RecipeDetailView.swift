import SwiftUI

struct RecipeDetailView: View {
    let recipeID: String
    /// Normalised names the user lacks, when opened from scan results; shown in red.
    var missingIngredients: [String] = []

    @Environment(AppServices.self) private var services
    @Environment(UserStore.self) private var userStore
    @Environment(ShoppingStore.self) private var shopping
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss
    @AppStorage(SettingsKey.unitSystem) private var unitSystemRaw = UnitSystem.metric.rawValue

    @State private var recipe: Recipe?
    @State private var isLoading = true
    @State private var servings = 1
    @State private var descriptionExpanded = false
    @State private var completedSteps: Set<Int> = []
    @State private var isSelectingIngredients = false
    @State private var selectedIngredients: Set<Int> = []
    @State private var showAddOptions = false
    @State private var showPublishConfirm = false
    @State private var editing: Recipe?
    @State private var wasDeleted = false
    @State private var viewStart = Date()

    private var unitSystem: UnitSystem { UnitSystem(rawValue: unitSystemRaw) ?? .metric }
    private var isOwner: Bool { recipe != nil && recipe?.userId == services.uid }

    var body: some View {
        Group {
            if let recipe {
                content(recipe)
            } else if isLoading {
                ProgressView()
            } else {
                ContentUnavailableView(L("toast_could_not_load_recipe"), systemImage: "exclamationmark.triangle")
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar }
        .task { await load() }
        .onAppear { viewStart = Date() }
        .onDisappear(perform: logView)
        .confirmationDialog(L("recipe_detail_add_to_list_title"), isPresented: $showAddOptions, titleVisibility: .visible) {
            Button(L("recipe_detail_add_all_ingredients")) { addAllIngredients() }
            Button(L("recipe_detail_select_ingredients")) {
                selectedIngredients = []
                isSelectingIngredients = true
            }
        }
        .alert(L("recipe_detail_publish_dialog_title"), isPresented: $showPublishConfirm) {
            Button(L("recipe_detail_publish_button")) { setPublic(true) }
            Button(L("cancel"), role: .cancel) {}
        } message: {
            Text(L("recipe_detail_publish_dialog_message"))
        }
        .sheet(item: $editing, onDismiss: {
            if wasDeleted { dismiss() } else { Task { await load() } }
        }) { recipe in
            RecipeEditorView(editing: recipe) { wasDeleted = true }
        }
    }

    // MARK: Content

    private func content(_ recipe: Recipe) -> some View {
        let ratio = recipe.servings > 0 ? Double(servings) / Double(recipe.servings) : 1
        return ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                RemoteImage(urlString: recipe.imageUrl, maxPixelSize: 1200)
                    .frame(height: 280)
                    .frame(maxWidth: .infinity)

                VStack(alignment: .leading, spacing: 22) {
                    header(recipe)
                    descriptionSection(recipe)
                    if recipe.servings > 0 { servingsSection(recipe) }
                    nutritionSection(recipe, ratio: ratio)
                    ingredientsSection(recipe, ratio: ratio)
                    methodSection(recipe)
                }
                .padding(.horizontal)
            }
            .padding(.bottom, 32)
        }
        .ignoresSafeArea(edges: .top)
    }

    private func header(_ recipe: Recipe) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 6) {
                Text(recipe.title).font(.title2.bold())
                HStack(spacing: 12) {
                    CookTimeLabel(minutes: recipe.timeInMins)
                    if !recipe.author.isEmpty {
                        Text(LF("recipe_detail_author_prefix", recipe.author))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    if isOwner {
                        Label(recipe.isPublic ? L("ios_public") : L("ios_private"), systemImage: recipe.isPublic ? "globe" : "lock")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            Spacer()
            FavoriteButton(recipe: recipe)
                .scaleEffect(1.3)
        }
    }

    @ViewBuilder
    private func descriptionSection(_ recipe: Recipe) -> some View {
        if !recipe.description.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text(recipe.description)
                    .font(.body)
                    .lineLimit(descriptionExpanded ? nil : 4)
                if recipe.description.count > 220 {
                    Button(descriptionExpanded ? L("read_less") : L("read_more")) {
                        withAnimation { descriptionExpanded.toggle() }
                    }
                    .font(.subheadline.bold())
                }
            }
        }
    }

    private func servingsSection(_ recipe: Recipe) -> some View {
        Stepper(value: $servings, in: 1...100) {
            Text("\(L("servings")): \(servings)").font(.headline)
        }
    }

    private func nutritionSection(_ recipe: Recipe, ratio: Double) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("nutrition_facts"))
            if recipe.nutrition.isEmpty {
                Text(L("nutrition_facts_not_provided_for_this_recipe"))
                    .foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(recipe.nutrition, id: \.self) { fact in
                            VStack(spacing: 2) {
                                Text(ServingsScaler.nutritionValue(fact.value, ratio: ratio)).font(.headline)
                                Text(fact.label).font(.caption).foregroundStyle(.secondary)
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(Color.chip, in: RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }
            }
        }
    }

    private func ingredientsSection(_ recipe: Recipe, ratio: Double) -> some View {
        let missing = Set(missingIngredients)
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("ingredients"))
            if isSelectingIngredients {
                Text(L("recipe_detail_select_ingredients")).font(.footnote).foregroundStyle(.secondary)
            }
            ForEach(Array(recipe.ingredients.enumerated()), id: \.offset) { index, ingredient in
                let isMissing = missing.contains(IngredientMatcher.normalize(ingredient.name))
                HStack(spacing: 12) {
                    if isSelectingIngredients {
                        Image(systemName: selectedIngredients.contains(index) ? "checkmark.square.fill" : "square")
                            .foregroundStyle(Color.brand)
                    }
                    Text(ingredient.name)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(ServingsScaler.ingredientText(ingredient, ratio: ratio, system: unitSystem))
                        .foregroundStyle(isMissing ? .red : .secondary)
                        .multilineTextAlignment(.trailing)
                }
                .foregroundStyle(isMissing ? .red : .primary)
                .padding(.vertical, 4)
                .contentShape(Rectangle())
                .onTapGesture {
                    guard isSelectingIngredients else { return }
                    if selectedIngredients.contains(index) { selectedIngredients.remove(index) } else { selectedIngredients.insert(index) }
                }
                Divider()
            }
        }
    }

    private func methodSection(_ recipe: Recipe) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(text: L("method"))
            ForEach(Array(recipe.method.enumerated()), id: \.offset) { index, step in
                let done = completedSteps.contains(index)
                Button {
                    if done { completedSteps.remove(index) } else { completedSteps.insert(index) }
                } label: {
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: done ? "checkmark.circle.fill" : "\(min(index + 1, 50)).circle")
                            .font(.title3)
                            .foregroundStyle(Color.brand)
                        Text(step.step)
                            .strikethrough(done)
                            .foregroundStyle(done ? .secondary : .primary)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: Toolbar

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        if isSelectingIngredients {
            ToolbarItem(placement: .topBarLeading) {
                Button(L("cancel")) { isSelectingIngredients = false }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(L("confirm")) { addSelectedIngredients() }
                    .bold()
                    .disabled(selectedIngredients.isEmpty)
            }
        } else if let recipe {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button { showAddOptions = true } label: {
                        Label(L("add_to_shopping_list"), systemImage: "cart.badge.plus")
                    }
                    if isOwner {
                        if recipe.isPublic {
                            Button { setPublic(false) } label: { Label(L("make_private"), systemImage: "lock") }
                        } else {
                            Button { showPublishConfirm = true } label: { Label(L("publish_recipe"), systemImage: "globe") }
                        }
                        Button { editing = recipe } label: { Label(L("edit_recipe"), systemImage: "pencil") }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
    }

    // MARK: Actions

    private func load() async {
        let loaded = await services.repository.recipe(id: recipeID, uid: services.uid)
        if let loaded, recipe == nil || loaded != recipe {
            recipe = loaded
            servings = max(loaded.servings, 1)
        }
        isLoading = false
    }

    /// Feeds the recommendation engine. (On Android this never ran: the recipe ID it needed was
    /// shadowed by a local variable, so the property was always null.)
    private func logView() {
        let seconds = Int(Date().timeIntervalSince(viewStart))
        guard seconds > 1, recipe != nil else { return }
        userStore.logActivity(.viewRecipe, value: recipeID, durationSeconds: seconds)
    }

    private func addAllIngredients() {
        guard let recipe else { return }
        Task {
            await shopping.addRecipeIngredients(title: recipe.title, ingredients: recipe.ingredients.map(\.name))
            toasts.show(LF("recipe_detail_toast_all_ingredients_added", recipe.title))
        }
    }

    private func addSelectedIngredients() {
        guard let recipe else { return }
        let names = selectedIngredients.sorted().compactMap { recipe.ingredients.indices.contains($0) ? recipe.ingredients[$0].name : nil }
        isSelectingIngredients = false
        Task {
            await shopping.addRecipeIngredients(title: recipe.title, ingredients: names)
            toasts.show(LP("recipe_detail_toast_ingredients_added", names.count))
        }
    }

    private func setPublic(_ isPublic: Bool) {
        guard var draft = recipe else { return }
        draft.isPublic = isPublic
        let updated = draft
        Task {
            do {
                try await services.repository.save(updated)
                recipe = updated
                toasts.show(L(isPublic ? "recipe_detail_toast_published" : "recipe_detail_toast_private"))
            } catch {
                toasts.show(LF("toast_operation_failed", error.localizedDescription))
            }
        }
    }
}
