import SwiftUI

/// What to search for on the results screen.
struct ScanQuery: Hashable {
    var ingredients: [String]
    /// False when re-opening a history entry, so it is not saved twice.
    var saveToHistory = true
}

struct ScanView: View {
    enum Mode: Hashable { case camera, manual, history }

    @Environment(AppServices.self) private var services
    @Environment(ToastCenter.self) private var toasts

    @State private var mode: Mode = .camera
    @State private var ingredients: [String] = []
    @State private var query: ScanQuery?

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $mode) {
                Text(L("scan_tab_item")).tag(Mode.camera)
                Text(L("scan_tab_type_voice")).tag(Mode.manual)
                Text(L("scan_tab_history")).tag(Mode.history)
            }
            .pickerStyle(.segmented)
            .padding()

            switch mode {
            case .camera:
                ScanCameraTab(ingredients: $ingredients, onFind: findRecipes)
            case .manual:
                ScanManualTab(ingredients: $ingredients, onFind: findRecipes)
            case .history:
                ScanHistoryTab { item in
                    query = ScanQuery(ingredients: item.ingredients, saveToHistory: false)
                }
            }
        }
        .navigationTitle(L("what_s_in_your_kitchen"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(item: $query) { query in
            ScanResultsView(query: query)
        }
    }

    private func findRecipes() {
        guard !ingredients.isEmpty else {
            toasts.show(L("scan_manual_add_ingredients_first"))
            return
        }
        query = ScanQuery(ingredients: ingredients)
    }
}

/// The ingredients collected so far, with remove buttons, and the search button.
struct IngredientTray: View {
    @Binding var ingredients: [String]
    var onFind: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !ingredients.isEmpty {
                FlowLayout {
                    ForEach(ingredients, id: \.self) { ingredient in
                        Chip(title: ingredient) {
                            ingredients.removeAll { $0 == ingredient }
                        }
                    }
                }
            }
            Button(L("find_recipes"), action: onFind)
                .buttonStyle(PrimaryButtonStyle())
                .disabled(ingredients.isEmpty)
        }
    }
}

extension Array where Element == String {
    /// Appends ingredients that are not already present (case-insensitive). Returns those added.
    @discardableResult
    mutating func addIngredients(_ new: [String]) -> [String] {
        var added: [String] = []
        for raw in new {
            let item = TextCleaner.capitalizedFirst(raw.trimmingCharacters(in: .whitespacesAndNewlines))
            guard !item.isEmpty, !contains(where: { $0.caseInsensitiveCompare(item) == .orderedSame }) else { continue }
            append(item)
            added.append(item)
        }
        return added
    }
}

// MARK: - Camera

struct ScanCameraTab: View {
    @Binding var ingredients: [String]
    var onFind: () -> Void

    @Environment(AppServices.self) private var services
    @Environment(ToastCenter.self) private var toasts
    @State private var photo: UIImage?
    @State private var showSources = false
    @State private var isRecognizing = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Button { showSources = true } label: {
                    ZStack {
                        if let photo {
                            Color.clear.overlay { Image(uiImage: photo).resizable().scaledToFill() }
                        } else {
                            VStack(spacing: 12) {
                                Image(systemName: "camera.viewfinder").font(.system(size: 64))
                                Text(L("point_at_a_list_of_ingredients")).font(.headline)
                            }
                            .foregroundStyle(Color.brand)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(Color.chip)
                        }
                        if isRecognizing {
                            Color.black.opacity(0.35)
                            ProgressView().tint(.white).controlSize(.large)
                        }
                    }
                    .frame(height: 260)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
                .buttonStyle(.plain)
                .disabled(isRecognizing)

                IngredientTray(ingredients: $ingredients, onFind: onFind)
            }
            .padding()
        }
        .photoSourceDialog(isPresented: $showSources, image: $photo, title: L("scan_text"), allowsRemove: false)
        .onChange(of: photo) { _, newPhoto in
            guard let newPhoto else { return }
            Task { await recognize(newPhoto) }
        }
    }

    private func recognize(_ image: UIImage) async {
        isRecognizing = true
        defer { isRecognizing = false }
        let vocabulary = await services.repository.ingredientVocabulary()
        let found = await IngredientRecognizer.ingredients(in: image, vocabulary: vocabulary)
        let added = ingredients.addIngredients(found)
        if found.isEmpty {
            toasts.show(L("scan_camera_no_ingredients_identified"))
        } else {
            toasts.show(LF("scan_camera_ingredients_found", (added.isEmpty ? found : added).joined(separator: ", ")))
        }
    }
}

// MARK: - Typing and voice

struct ScanManualTab: View {
    @Binding var ingredients: [String]
    var onFind: () -> Void

    @Environment(ToastCenter.self) private var toasts
    @State private var text = ""
    @State private var speech = SpeechRecognizer()
    @FocusState private var fieldFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(spacing: 10) {
                    TextField(L("type_an_ingredient"), text: $text)
                        .textFieldStyle(.roundedBorder)
                        .focused($fieldFocused)
                        .submitLabel(.done)
                        .onSubmit(addTyped)
                    Button(action: addTyped) {
                        Image(systemName: "plus.circle.fill").font(.title2)
                    }
                    .disabled(text.trimmed.isEmpty)
                    .accessibilityLabel(L("add"))
                    Button(action: toggleListening) {
                        Image(systemName: speech.isRecording ? "stop.circle.fill" : "mic.circle.fill")
                            .font(.title)
                            .foregroundStyle(speech.isRecording ? .red : Color.brand)
                            .symbolEffect(.pulse, isActive: speech.isRecording)
                    }
                    .accessibilityLabel(L("scan_manual_voice_prompt"))
                }
                if speech.isRecording {
                    Text(speech.transcript.isEmpty ? L("scan_manual_voice_prompt") : speech.transcript)
                        .font(.callout.italic())
                        .foregroundStyle(.secondary)
                }
                IngredientTray(ingredients: $ingredients, onFind: onFind)
            }
            .padding()
        }
        .onDisappear {
            if speech.isRecording { speech.stop() }
        }
    }

    private func addTyped() {
        ingredients.addIngredients([text])
        text = ""
        fieldFocused = true
    }

    private func toggleListening() {
        if speech.isRecording {
            ingredients.addIngredients(SpeechRecognizer.ingredients(fromSpeech: speech.stop()))
            return
        }
        Task {
            do {
                try await speech.start()
            } catch {
                toasts.show(L("scan_manual_voice_not_available"))
            }
        }
    }
}

// MARK: - History

struct ScanHistoryTab: View {
    var onSelect: (ScanHistoryItem) -> Void
    @Environment(ScanHistoryStore.self) private var history

    var body: some View {
        if history.items.isEmpty {
            ContentUnavailableView(L("your_scan_history"), systemImage: "clock.arrow.circlepath")
        } else {
            List {
                ForEach(history.items) { item in
                    Button { onSelect(item) } label: {
                        HStack(spacing: 12) {
                            RemoteImage(urlString: item.imageUrl, maxPixelSize: 180)
                                .frame(width: 56, height: 56)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.title).font(.headline).lineLimit(2)
                                Text(item.timestamp.formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
                .onDelete { offsets in
                    for item in offsets.map({ history.items[$0] }) {
                        history.delete(item)
                    }
                }
            }
            .listStyle(.plain)
        }
    }
}

// MARK: - Results

struct ScanResultsView: View {
    let query: ScanQuery

    @Environment(AppServices.self) private var services
    @Environment(ScanHistoryStore.self) private var history
    @State private var matches: [RecipeMatch]?

    var body: some View {
        Group {
            if let matches {
                if matches.isEmpty {
                    ContentUnavailableView(L("ios_no_matches_title"), systemImage: "fork.knife", description: Text(L("ios_no_matches_message")))
                } else {
                    List(matches) { match in
                        NavigationLink(value: AppRoute.recipe(id: match.recipe.id, missingIngredients: match.missingIngredients)) {
                            HStack(spacing: 12) {
                                RemoteImage(urlString: match.recipe.imageUrl, maxPixelSize: 240)
                                    .frame(width: 72, height: 72)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(match.recipe.title).font(.headline).lineLimit(2)
                                    Text(match.missingIngredients.isEmpty
                                         ? L("recipe_match_all_ingredients")
                                         : LP("recipe_match_missing_ingredients", match.missingIngredients.count))
                                        .font(.subheadline)
                                        .foregroundStyle(match.missingIngredients.isEmpty ? .green : .secondary)
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle(L("recipe_matches"))
        .task {
            guard matches == nil else { return }
            let found = await services.repository.matches(for: query.ingredients)
            matches = found
            if query.saveToHistory {
                history.add(ingredients: query.ingredients, imageUrl: found.first?.recipe.imageUrl)
            }
        }
    }
}
