import FirebaseStorage
import SwiftUI

/// Create or edit a recipe (Android's AddRecipeFragment).
struct RecipeEditorView: View {
    let editing: Recipe?
    var onDeleted: (() -> Void)?

    init(editing: Recipe?, onDeleted: (() -> Void)? = nil) {
        self.editing = editing
        self.onDeleted = onDeleted
    }

    @Environment(\.dismiss) private var dismiss
    @Environment(AppServices.self) private var services
    @Environment(UserStore.self) private var userStore
    @Environment(ToastCenter.self) private var toasts

    struct IngredientRow: Identifiable {
        let id = UUID()
        var name = ""
        var quantity = ""
        var unit = ""
    }

    struct StepRow: Identifiable {
        let id = UUID()
        var text = ""
    }

    @State private var title = ""
    @State private var details = ""
    @State private var minutes = ""
    @State private var servings = ""
    @State private var category = ""
    @State private var mealType: MealType?
    @State private var ingredients: [IngredientRow] = [IngredientRow()]
    @State private var steps: [StepRow] = [StepRow()]
    @State private var calories = ""
    @State private var fat = ""
    @State private var carbs = ""
    @State private var salts = ""

    @State private var existingImageURL = ""
    @State private var photo: UIImage?
    @State private var showPhotoOptions = false
    @State private var categories: [String] = []
    @State private var isSaving = false
    @State private var showDeleteConfirm = false
    @State private var didLoad = false

    private static let units = ["g", "kg", "ml", "L", "tsp", "tbsp", "cup", "pinch", "piece(s)"]

    var body: some View {
        NavigationStack {
            Form {
                photoSection
                detailsSection
                ingredientsSection
                stepsSection
                nutritionSection
                if editing != nil {
                    Section {
                        Button(L("delete_recipe"), role: .destructive) { showDeleteConfirm = true }
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .navigationTitle(editing == nil ? L("add_your_recipe") : L("edit_recipe_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(editing == nil ? L("save_recipe") : L("save_changes"), action: save)
                        .bold()
                        .disabled(isSaving)
                }
            }
            .photoSourceDialog(
                isPresented: $showPhotoOptions,
                image: Binding(get: { photo }, set: { newValue in
                    photo = newValue
                    if newValue == nil { existingImageURL = "" }
                }),
                title: L("dialog_set_photo_title")
            )
            .alert(L("delete_recipe_dialog_title"), isPresented: $showDeleteConfirm) {
                Button(L("delete"), role: .destructive, action: deleteRecipe)
                Button(L("cancel"), role: .cancel) {}
            } message: {
                Text(L("delete_recipe_dialog_message"))
            }
            .overlay { if isSaving { LoadingOverlay() } }
            .interactiveDismissDisabled(isSaving)
            .task { await loadInitialState() }
        }
    }

    // MARK: Sections

    private var photoSection: some View {
        Section {
            Button { showPhotoOptions = true } label: {
                Group {
                    if let photo {
                        Color.clear.overlay { Image(uiImage: photo).resizable().scaledToFill() }
                    } else if !existingImageURL.isEmpty {
                        RemoteImage(urlString: existingImageURL, maxPixelSize: 1000)
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "photo.badge.plus").font(.system(size: 40))
                            Text(L("dialog_set_photo_title")).font(.subheadline)
                        }
                        .foregroundStyle(Color.brand)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.chip)
                    }
                }
                .frame(height: 200)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .listRowInsets(EdgeInsets())
        }
    }

    private var detailsSection: some View {
        Section {
            TextField(L("recipe_name"), text: $title)
            TextField(L("description"), text: $details, axis: .vertical)
                .lineLimit(3...8)
            TextField(L("time_to_make_e_g_45_mins"), text: $minutes)
                .keyboardType(.numberPad)
            TextField(L("servings"), text: $servings)
                .keyboardType(.numberPad)
            HStack {
                TextField(L("category"), text: $category)
                if !categories.isEmpty {
                    Menu {
                        ForEach(categories, id: \.self) { name in
                            Button(name) { category = name }
                        }
                    } label: {
                        Image(systemName: "chevron.down.circle")
                    }
                }
            }
            Picker(L("meal_type_e_g_breakfast_snack"), selection: $mealType) {
                Text("—").tag(MealType?.none)
                ForEach(MealType.allCases) { meal in
                    Text(meal.title).tag(MealType?.some(meal))
                }
            }
        }
    }

    private var ingredientsSection: some View {
        Section(L("ingredients")) {
            ForEach($ingredients) { $row in
                HStack(spacing: 8) {
                    TextField(L("ingredient_name"), text: $row.name)
                    TextField(L("qty"), text: $row.quantity)
                        .keyboardType(.decimalPad)
                        .frame(width: 56)
                        .multilineTextAlignment(.trailing)
                    Menu {
                        ForEach(Self.units, id: \.self) { unit in
                            Button(unit) { row.unit = unit }
                        }
                    } label: {
                        Text(row.unit.isEmpty ? L("unit") : row.unit)
                            .frame(minWidth: 52)
                    }
                }
            }
            .onDelete { ingredients.remove(atOffsets: $0) }
            Button {
                ingredients.append(IngredientRow())
            } label: {
                Label(L("add_ingredient"), systemImage: "plus.circle")
            }
        }
    }

    private var stepsSection: some View {
        Section(L("method_steps")) {
            ForEach($steps) { $step in
                HStack(alignment: .top) {
                    Text("\(stepNumber(of: step)).").foregroundStyle(.secondary)
                    TextField(L("step_details"), text: $step.text, axis: .vertical)
                }
            }
            .onDelete { steps.remove(atOffsets: $0) }
            Button {
                steps.append(StepRow())
            } label: {
                Label(L("add_step"), systemImage: "plus.circle")
            }
        }
    }

    private func stepNumber(of step: StepRow) -> Int {
        (steps.firstIndex { $0.id == step.id } ?? 0) + 1
    }

    private var nutritionSection: some View {
        Section(L("nutrition_facts_optional")) {
            TextField(L("calories"), text: $calories).keyboardType(.numberPad)
            TextField(L("fat_g"), text: $fat).keyboardType(.decimalPad)
            TextField(L("carbs_g"), text: $carbs).keyboardType(.decimalPad)
            TextField(L("salts_g"), text: $salts).keyboardType(.decimalPad)
        }
    }

    // MARK: Loading

    private func loadInitialState() async {
        guard !didLoad else { return }
        didLoad = true
        if let recipe = editing {
            title = recipe.title
            details = recipe.description
            minutes = recipe.timeInMins > 0 ? String(recipe.timeInMins) : ""
            servings = recipe.servings > 0 ? String(recipe.servings) : ""
            category = recipe.category
            mealType = MealType(rawValue: recipe.mealType)
            ingredients = recipe.ingredients.map { IngredientRow(name: $0.name, quantity: $0.quantity, unit: $0.unit) }
            if ingredients.isEmpty { ingredients = [IngredientRow()] }
            steps = recipe.method.map { StepRow(text: $0.step) }
            if steps.isEmpty { steps = [StepRow()] }
            existingImageURL = recipe.imageUrl
            for fact in recipe.nutrition {
                let number = fact.value.filter { $0.isNumber || $0 == "." }
                switch fact.label.lowercased() {
                case "calories": calories = number
                case "fat": fat = number
                case "carbs": carbs = number
                case "salts": salts = number
                default: break
                }
            }
        }
        let pool = await services.repository.publicPool()
        categories = RecipeRepository.categories(in: pool)
    }

    // MARK: Saving

    /// Same required fields as the Android form. Returns the first problem's message.
    private func validationProblem() -> String? {
        if title.trimmed.isEmpty { return L("validation_recipe_name_required") }
        if category.trimmed.isEmpty { return L("validation_category_required") }
        if minutes.trimmed.isEmpty { return L("validation_time_required") }
        if servings.trimmed.isEmpty { return L("validation_servings_required") }
        if mealType == nil { return L("validation_meal_type_required") }
        let filledIngredients = ingredients.filter { !$0.name.trimmed.isEmpty || !$0.quantity.trimmed.isEmpty }
        if filledIngredients.isEmpty { return L("validation_ingredient_min_required") }
        if filledIngredients.contains(where: { $0.name.trimmed.isEmpty }) { return L("validation_ingredient_name_required") }
        if filledIngredients.contains(where: { $0.quantity.trimmed.isEmpty }) { return L("validation_ingredient_qty_required") }
        if steps.allSatisfy({ $0.text.trimmed.isEmpty }) { return L("validation_step_min_required") }
        return nil
    }

    private func save() {
        if let problem = validationProblem() {
            toasts.show(problem)
            return
        }
        guard let uid = services.uid else {
            toasts.show(L("toast_login_required"))
            return
        }
        isSaving = true

        // Editing keeps the ID, owner, visibility and API-provided flags of the original.
        // (Android rebuilt the recipe from scratch, which made edited recipes private again.)
        var recipe = editing ?? Recipe()
        let isNew = editing == nil
        if isNew {
            recipe.id = UUID().uuidString
            recipe.userId = uid
            recipe.isPublic = false
            recipe.author = userStore.profile?.username.nonEmpty ?? userStore.profile?.displayName.nonEmpty ?? "Unknown Author"
        }
        recipe.title = title.trimmed
        recipe.description = details.trimmed
        recipe.timeInMins = Int(minutes.trimmed) ?? 0
        recipe.servings = Int(servings.trimmed) ?? 0
        recipe.category = category.trimmed
        recipe.mealType = mealType?.rawValue ?? ""
        recipe.ingredients = ingredients
            .filter { !$0.name.trimmed.isEmpty && !$0.quantity.trimmed.isEmpty }
            .map { Ingredient(name: $0.name.trimmed, quantity: $0.quantity.trimmed, unit: $0.unit) }
        recipe.method = steps.map(\.text.trimmed).filter { !$0.isEmpty }.map(MethodStep.init(step:))
        // Labels are stored in English so the data reads the same in every language.
        var nutrition: [NutritionFact] = []
        if !calories.trimmed.isEmpty { nutrition.append(NutritionFact(label: "Calories", value: calories.trimmed)) }
        if !fat.trimmed.isEmpty { nutrition.append(NutritionFact(label: "Fat", value: "\(fat.trimmed)g")) }
        if !carbs.trimmed.isEmpty { nutrition.append(NutritionFact(label: "Carbs", value: "\(carbs.trimmed)g")) }
        if !salts.trimmed.isEmpty { nutrition.append(NutritionFact(label: "Salts", value: "\(salts.trimmed)g")) }
        recipe.nutrition = nutrition
        recipe.imageUrl = existingImageURL

        let draft = recipe
        let photo = photo
        Task {
            defer { isSaving = false }
            var recipe = draft
            do {
                // Only a newly picked photo is uploaded; an unchanged photo keeps its URL.
                // (Android tried to upload the existing https URL as a file, so edits failed.)
                if let photo {
                    recipe.imageUrl = try await ImageUploader.upload(photo, path: "recipe_images/\(uid)/\(recipe.id).jpg")
                }
                try await services.repository.save(recipe)
                if isNew {
                    await userStore.gamification.recordRecipeCreated(uid: uid)
                    await userStore.addNotification(
                        title: L("notification_upload_success_title"),
                        message: LF("notification_upload_success_body", recipe.title)
                    )
                }
                toasts.show(L("toast_recipe_saved"))
                dismiss()
            } catch {
                toasts.show(LF("toast_operation_failed", error.localizedDescription))
            }
        }
    }

    private func deleteRecipe() {
        guard let recipe = editing, let uid = services.uid else { return }
        isSaving = true
        Task {
            defer { isSaving = false }
            do {
                try await services.repository.delete(recipe)
                // Remove the uploaded photo too (Android left it in Storage).
                if recipe.imageUrl.contains("firebasestorage") {
                    try? await Storage.storage().reference(withPath: "recipe_images/\(uid)/\(recipe.id).jpg").delete()
                }
                toasts.show(L("toast_recipe_deleted"))
                onDeleted?()
                dismiss()
            } catch {
                toasts.show(LF("toast_operation_failed", error.localizedDescription))
            }
        }
    }
}

extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }

    /// nil for an empty string, so `a.nonEmpty ?? b` falls through to b.
    var nonEmpty: String? { isEmpty ? nil : self }
}
