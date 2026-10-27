import FirebaseAuth
import SwiftUI
import UIKit

struct ProfileView: View {
    @Environment(AppServices.self) private var services
    @Environment(UserStore.self) private var userStore
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.openURL) private var openURL
    @AppStorage(SettingsKey.notificationsEnabled) private var notificationsEnabled = true
    @State private var showLogout = false

    var body: some View {
        List {
            Section {
                HStack(spacing: 16) {
                    ProfilePhoto(image: nil, urlString: userStore.profile?.profileImageUrl, size: 72)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(userStore.profile?.displayName.nonEmpty ?? L("profile_default_name"))
                            .font(.title3.bold())
                        Text(AuthService.currentUser?.email ?? userStore.profile?.email ?? "")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 6)
                NavigationLink(value: AppRoute.editProfile) { Label(L("edit_profile_setting"), systemImage: "pencil") }
                NavigationLink(value: AppRoute.myRecipes) { Label(L("my_recipes"), systemImage: "book") }
            }

            Section(L("account")) {
                NavigationLink(value: AppRoute.badges) { Label(L("my_badges_achievementsbtn"), systemImage: "rosette") }
                NavigationLink(value: AppRoute.preferences) { Label(L("preferences_setting"), systemImage: "slider.horizontal.3") }
                Button {
                    // iOS shows a per-app language choice in Settings when the app has several languages.
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                } label: {
                    Label(L("language"), systemImage: "globe")
                }
                Toggle(isOn: $notificationsEnabled) {
                    Label(L("notifications_setting"), systemImage: "bell")
                }
                .onChange(of: notificationsEnabled) { _, enabled in
                    PushService.shared.setTopicSubscription(enabled)
                }
            }

            Section(L("support_amp_about")) {
                NavigationLink(value: AppRoute.about) { Label(L("about_us"), systemImage: "info.circle") }
                Button(action: reportIssue) { Label(L("report_an_issue"), systemImage: "envelope") }
            }

            Section {
                Button(L("log_out"), role: .destructive) { showLogout = true }
                    .frame(maxWidth: .infinity)
            } footer: {
                Text(LF("profile_version", AppConfig.appVersion))
                    .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle(L("profile"))
        .confirmationDialog(L("profile_logout_title"), isPresented: $showLogout, titleVisibility: .visible) {
            Button(L("profile_logout_button"), role: .destructive) { services.signOut() }
        } message: {
            Text(L("profile_logout_message"))
        }
    }

    private func reportIssue() {
        let body = """


        --- Please Do Not Edit Below This Line ---
        App Version: \(AppConfig.appVersion)
        Device: \(UIDevice.current.model)
        iOS Version: \(UIDevice.current.systemVersion)
        User ID: \(services.uid ?? "Not Logged In")
        --- Please Explain Above ---
        """
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = AppConfig.supportEmail
        components.queryItems = [
            URLQueryItem(name: "subject", value: "Harmony Kitchen iOS - Bug Report / Feedback"),
            URLQueryItem(name: "body", value: body),
        ]
        guard let url = components.url else { return }
        openURL(url) { accepted in
            if !accepted { toasts.show(L("report_issue_error")) }
        }
    }
}

struct EditProfileView: View {
    @Environment(UserStore.self) private var userStore
    @Environment(AppServices.self) private var services
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss

    @State private var fullName = ""
    @State private var username = ""
    @State private var phone = ""
    @State private var photo: UIImage?
    @State private var removePhoto = false
    @State private var showPhotoOptions = false
    @State private var isSaving = false
    @State private var loaded = false
    @State private var nameError: String?
    @State private var usernameError: String?

    var body: some View {
        Form {
            Section {
                Button { showPhotoOptions = true } label: {
                    ProfilePhoto(
                        image: photo,
                        urlString: removePhoto ? nil : userStore.profile?.profileImageUrl,
                        size: 110
                    )
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
                .listRowBackground(Color.clear)
            }
            Section(L("personal")) {
                TextField(L("full_name"), text: $fullName).textContentType(.name)
                if let nameError { FieldError(text: nameError) }
                TextField(L("username"), text: $username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if let usernameError { FieldError(text: usernameError) }
                TextField(L("phone_number"), text: $phone)
                    .keyboardType(.phonePad)
                    .textContentType(.telephoneNumber)
            }
            Section {
                Button(L("change_password"), action: sendPasswordReset)
            }
        }
        .navigationTitle(L("edit_profile"))
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(L("save_changes"), action: save).disabled(isSaving)
            }
        }
        .photoSourceDialog(
            isPresented: $showPhotoOptions,
            image: Binding(get: { photo }, set: { newValue in
                photo = newValue
                removePhoto = newValue == nil
            }),
            title: L("profile_completion_set_photo_title")
        )
        .overlay { if isSaving { LoadingOverlay() } }
        .onAppear {
            guard !loaded, let profile = userStore.profile else { return }
            loaded = true
            fullName = profile.fullName ?? ""
            username = profile.username
            phone = profile.phoneNumber ?? ""
        }
    }

    private func save() {
        // Android's edit screen had validation but never called it.
        nameError = fullName.trimmed.isEmpty ? L("validation_full_name_empty") : nil
        usernameError = username.trimmed.isEmpty ? L("validation_username_empty") : nil
        guard nameError == nil, usernameError == nil, let uid = services.uid else { return }

        isSaving = true
        let baseFields: [String: Any] = [
            "full_name": fullName.trimmed,
            "username": username.trimmed,
            "phone_number": phone.trimmed,
        ]
        let newPhoto = photo
        let shouldRemove = removePhoto
        Task {
            defer { isSaving = false }
            var fields = baseFields
            do {
                if let newPhoto {
                    fields["profileImageUrl"] = try await ImageUploader.upload(newPhoto, path: "profile_pictures/\(uid)/profile.jpg", maxDimension: 800)
                } else if shouldRemove {
                    fields["profileImageUrl"] = ""
                }
                try await userStore.updateProfile(fields)
                toasts.show(L("edit_profile_update_success"))
                dismiss()
            } catch {
                toasts.show(LF("edit_profile_update_failed", error.localizedDescription))
            }
        }
    }

    private func sendPasswordReset() {
        guard let email = AuthService.currentUser?.email else { return }
        Task {
            do {
                try await AuthService.sendPasswordReset(to: email)
                toasts.show(L("edit_profile_reset_link_sent"))
            } catch {
                toasts.show(LF("edit_profile_reset_link_failed", error.localizedDescription))
            }
        }
    }
}

struct PreferencesView: View {
    @Environment(UserStore.self) private var userStore
    @AppStorage(SettingsKey.themeMode) private var themeMode = ThemeMode.system.rawValue
    @AppStorage(SettingsKey.useAllCaps) private var useAllCaps = true
    @AppStorage(SettingsKey.unitSystem) private var unitSystem = UnitSystem.metric.rawValue

    var body: some View {
        Form {
            Section(L("appearance")) {
                Picker(L("appearance"), selection: $themeMode) {
                    ForEach(ThemeMode.allCases) { mode in
                        Text(mode.title).tag(mode.rawValue)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                Toggle(L("display_name_in_all_caps"), isOn: $useAllCaps)
            }
            Section(L("unit_system")) {
                Picker(L("unit_system"), selection: $unitSystem) {
                    ForEach(UnitSystem.allCases) { system in
                        Text(system.title).tag(system.rawValue)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            }
            Section(L("personal")) {
                NavigationLink(value: AppRoute.editCountry) {
                    LabeledContent(L("country"), value: CountryCatalog.country(named: userStore.profile?.country)?.localizedName ?? "—")
                }
                NavigationLink(value: AppRoute.editCuisines) {
                    LabeledContent(L("cuisines"), value: summary(userStore.cuisines, catalog: CuisineCatalog.all))
                }
                NavigationLink(value: AppRoute.editDiets) {
                    LabeledContent(L("dietary_preferences"), value: summary(userStore.diets, catalog: DietCatalog.all))
                }
            }
        }
        .navigationTitle(L("preferences"))
    }

    private func summary(_ ids: [String], catalog: [PreferenceOption]) -> String {
        guard !ids.isEmpty else { return "—" }
        let titles = ids.map { id in catalog.first { $0.id == id }?.title ?? id }
        return titles.count > 2 ? "\(titles.prefix(2).joined(separator: ", ")) +\(titles.count - 2)" : titles.joined(separator: ", ")
    }
}

struct AboutView: View {
    var body: some View {
        List {
            Section {
                VStack(spacing: 10) {
                    Image("Logo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 110, height: 110)
                        .clipShape(Circle())
                    Text(L("about_harmony_kitchen")).font(.title2.bold())
                    Text(LF("profile_version", AppConfig.appVersion)).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
            Section(L("our_mission")) {
                Text(L("harmony_kitchen_is_a_final_year_project_designed_to_bring_delicious_easy_to_make_recipes_to_your_fingertips_tailored_to_the_ingredients_you_have_and_the_flavors_you_love"))
            }
            Section(L("the_team_rst_innovations")) {
                Text(L("this_app_was_proudly_developed_by_students_of_rosebank_college_n_tokollo_nonyane_n_sagwadi_mashimbye_n_rinae_magadagela"))
            }
            Section(L("credits_amp_attributions")) {
                Text(L("ui_ux_design_figma_nphotos_unsplash_nicons_material_design_icons"))
            }
        }
        .navigationTitle(L("about_us"))
    }
}
