import SwiftUI

/// The picker screens are used both during sign-up and later from Preferences.
enum PickerMode {
    case register
    case edit
}

// MARK: - Country

struct CountryPickerView: View {
    let mode: PickerMode
    var onContinue: (() -> Void)?

    @Environment(RegistrationModel.self) private var registration: RegistrationModel?
    @Environment(UserStore.self) private var userStore: UserStore?
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss

    @State private var search = ""
    @State private var selected: Country?
    @State private var isSaving = false

    private var countries: [Country] {
        let query = search.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { return CountryCatalog.all }
        return CountryCatalog.all.filter {
            $0.localizedName.localizedCaseInsensitiveContains(query) || $0.englishName.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        List(countries) { country in
            Button {
                selected = country
            } label: {
                HStack {
                    Text(country.flag).font(.title2)
                    Text(country.localizedName).foregroundStyle(.primary)
                    Spacer()
                    if selected == country {
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(Color.brand)
                    }
                }
            }
        }
        .listStyle(.plain)
        .searchable(text: $search, placement: .navigationBarDrawer(displayMode: .always), prompt: L("select_country"))
        .navigationTitle(L("which_country_are_you_from"))
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .top) {
            if mode == .register {
                RegistrationProgress(step: 1)
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button(mode == .edit ? L("save_changes") : L("continuebtn"), action: submit)
                .buttonStyle(PrimaryButtonStyle())
                .disabled(selected == nil || isSaving)
                .padding()
                .background(.bar)
        }
        .onAppear {
            if selected == nil {
                selected = mode == .edit
                    ? CountryCatalog.country(named: userStore?.profile?.country)
                    : registration?.data.country
            }
        }
    }

    private func submit() {
        guard let selected else {
            toasts.show(L("country_toast_selection_required"))
            return
        }
        switch mode {
        case .register:
            registration?.data.country = selected
            onContinue?()
        case .edit:
            isSaving = true
            Task {
                defer { isSaving = false }
                do {
                    try await userStore?.updateProfile(["country": selected.englishName])
                    toasts.show(L("country_toast_update_success"))
                    dismiss()
                } catch {
                    toasts.show(LF("country_toast_update_failed", error.localizedDescription))
                }
            }
        }
    }
}

struct RegistrationProgress: View {
    let step: Int

    var body: some View {
        ProgressView(value: Double(step), total: 5)
            .tint(Color.brand)
            .padding(.horizontal)
            .padding(.vertical, 6)
            .background(.bar)
    }
}

// MARK: - Cuisines and diets

struct PreferenceGridView: View {
    enum Kind {
        case cuisines
        case diets

        var options: [PreferenceOption] { self == .cuisines ? CuisineCatalog.all : DietCatalog.all }
        var columns: Int { self == .cuisines ? 3 : 2 }
        var firestoreField: String { self == .cuisines ? "selected_cuisines" : "selected_diets" }
        var title: String { L(self == .cuisines ? "select_your_cuisines_preferences" : "any_dietary_preferences") }
        var subtitle: String {
            L(self == .cuisines
              ? "select_your_cuisines_preferences_for_better_recommendations_or_you_can_skip"
              : "select_your_dietary_preferences_for_better_recommendations_or_you_can_skip_it")
        }
        var emptySelectionMessage: String {
            L(self == .cuisines ? "cuisine_selection_toast_at_least_one" : "diet_selection_toast_at_least_one")
        }
    }

    let kind: Kind
    let mode: PickerMode
    var onContinue: (() -> Void)?

    @Environment(RegistrationModel.self) private var registration: RegistrationModel?
    @Environment(UserStore.self) private var userStore: UserStore?
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss

    @State private var selected: Set<String> = []
    @State private var loaded = false
    @State private var isSaving = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(kind.subtitle)
                    .foregroundStyle(.secondary)
                if kind == .cuisines {
                    Button(selected.count == kind.options.count ? L("deselect_all") : L("select_all")) {
                        selected = selected.count == kind.options.count ? [] : Set(kind.options.map(\.id))
                    }
                }
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: kind.columns), spacing: 14) {
                    ForEach(kind.options) { option in
                        PreferenceTile(option: option, isSelected: selected.contains(option.id), size: kind == .cuisines ? 84 : 110)
                            .onTapGesture {
                                if selected.contains(option.id) { selected.remove(option.id) } else { selected.insert(option.id) }
                            }
                    }
                }
            }
            .padding()
        }
        .navigationTitle(kind.title)
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .top) {
            if mode == .register { RegistrationProgress(step: kind == .cuisines ? 2 : 3) }
        }
        .safeAreaInset(edge: .bottom) {
            HStack(spacing: 12) {
                if mode == .register {
                    Button(L("skip")) { save([]) }
                        .buttonStyle(PrimaryButtonStyle(background: .welcomeButtonLight, foreground: Color(hex: 0x343A40)))
                }
                Button(mode == .edit ? L("save_changes") : L("continuebtn")) {
                    guard !selected.isEmpty else {
                        toasts.show(kind.emptySelectionMessage)
                        return
                    }
                    save(kind.options.map(\.id).filter { selected.contains($0) })
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(isSaving)
            }
            .padding()
            .background(.bar)
        }
        .onAppear(perform: loadInitialSelection)
    }

    private func loadInitialSelection() {
        guard !loaded else { return }
        loaded = true
        switch (mode, kind) {
        case (.edit, .cuisines): selected = Set(userStore?.cuisines ?? [])
        case (.edit, .diets): selected = Set(userStore?.diets ?? [])
        case (.register, .cuisines): selected = Set(registration?.data.cuisines ?? [])
        case (.register, .diets): selected = Set(registration?.data.diets ?? [])
        }
    }

    /// Stores English IDs, never translated names.
    private func save(_ ids: [String]) {
        switch mode {
        case .register:
            if kind == .cuisines { registration?.data.cuisines = ids } else { registration?.data.diets = ids }
            onContinue?()
        case .edit:
            isSaving = true
            Task {
                defer { isSaving = false }
                do {
                    try await userStore?.updateProfile([kind.firestoreField: ids])
                    toasts.show(L("cuisine_selection_toast_preferences_updated"))
                    dismiss()
                } catch {
                    toasts.show(LF("cuisine_selection_toast_update_failed", error.localizedDescription))
                }
            }
        }
    }
}

struct PreferenceTile: View {
    let option: PreferenceOption
    let isSelected: Bool
    let size: CGFloat

    var body: some View {
        VStack(spacing: 8) {
            Image(option.imageName)
                .resizable()
                .scaledToFill()
                .frame(width: size, height: size)
                .clipShape(Circle())
                .overlay(Circle().stroke(isSelected ? Color.brand : .clear, lineWidth: 4))
                .overlay(alignment: .topTrailing) {
                    if isSelected {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.title3)
                            .foregroundStyle(.white, Color.brand)
                    }
                }
            Text(option.title)
                .font(.footnote.weight(isSelected ? .bold : .regular))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .padding(6)
        .frame(maxWidth: .infinity)
        .background(isSelected ? Color.brand.opacity(0.12) : .clear, in: RoundedRectangle(cornerRadius: 16))
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

// MARK: - Profile details

struct ProfileCompletionView: View {
    var onContinue: () -> Void

    @Environment(RegistrationModel.self) private var registration
    @State private var fullName = ""
    @State private var phone = ""
    @State private var gender = ""
    @State private var hasBirthday = false
    @State private var birthday = Calendar.current.date(byAdding: .year, value: -18, to: Date()) ?? Date()
    @State private var photo: UIImage?
    @State private var showPhotoOptions = false
    @State private var nameError: String?
    @State private var phoneError: String?

    private var dialCode: String { registration.data.country?.dialCode ?? "+27" }

    /// Users must be 13 to 100 years old (same limits as the Android date picker).
    private var birthdayRange: ClosedRange<Date> {
        let calendar = Calendar.current
        let oldest = calendar.date(byAdding: .year, value: -100, to: Date()) ?? Date()
        let youngest = calendar.date(byAdding: .year, value: -13, to: Date()) ?? Date()
        return oldest...youngest
    }

    var body: some View {
        Form {
            Section {
                Button { showPhotoOptions = true } label: {
                    ProfilePhoto(image: photo, urlString: nil, size: 110)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
                .listRowBackground(Color.clear)
            }
            Section {
                TextField(L("full_name"), text: $fullName)
                    .textContentType(.name)
                if let nameError { FieldError(text: nameError) }
                HStack {
                    Text("\(registration.data.country?.flag ?? "🇿🇦") \(dialCode)")
                        .foregroundStyle(.secondary)
                    TextField(L("phone_number"), text: $phone)
                        .keyboardType(.phonePad)
                        .textContentType(.telephoneNumber)
                }
                if let phoneError { FieldError(text: phoneError) }
                Picker(L("gender_optional"), selection: $gender) {
                    Text("—").tag("")
                    ForEach(LocalizedArray.items("gender_options"), id: \.self) { Text($0).tag($0) }
                }
                Toggle(L("date_of_birth_optional"), isOn: $hasBirthday.animation())
                if hasBirthday {
                    DatePicker(L("date_of_birth_optional"), selection: $birthday, in: birthdayRange, displayedComponents: .date)
                }
            } footer: {
                Text(L("don_t_worry_only_you_can_see_your_personal_data_no_one_else_will_be_able_to_see_it"))
            }
        }
        .navigationTitle(L("complete_your_profile"))
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .top) { RegistrationProgress(step: 4) }
        .safeAreaInset(edge: .bottom) {
            Button(L("continuebtn"), action: submit)
                .buttonStyle(PrimaryButtonStyle())
                .padding()
                .background(.bar)
        }
        .photoSourceDialog(isPresented: $showPhotoOptions, image: $photo, title: L("profile_completion_set_photo_title"))
        .onAppear {
            fullName = registration.data.fullName
            photo = registration.data.profileImage
        }
    }

    private func submit() {
        let name = fullName.trimmingCharacters(in: .whitespaces)
        let e164 = Validators.e164(dialCode: dialCode, national: phone)
        nameError = name.isEmpty ? L("validation_fullname_empty") : nil
        phoneError = e164 == nil ? L("validation_phone_invalid") : nil
        guard nameError == nil, let e164 else { return }

        registration.data.fullName = name
        registration.data.phoneNumber = e164
        registration.data.gender = gender.isEmpty ? nil : gender
        registration.data.dateOfBirth = hasBirthday ? birthday : nil
        registration.data.profileImage = photo
        // Default kitchen units from the country, as on Android.
        UserDefaults.standard.set(
            UnitSystem.defaultFor(countryCode: registration.data.country?.code).rawValue,
            forKey: SettingsKey.unitSystem
        )
        onContinue()
    }
}

struct ProfilePhoto: View {
    let image: UIImage?
    let urlString: String?
    var size: CGFloat = 96

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            } else if let urlString, !urlString.isEmpty {
                RemoteImage(urlString: urlString, maxPixelSize: size * 3)
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(Color.brand)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color(hex: 0xE0E0E0), lineWidth: 2))
        .overlay(alignment: .bottomTrailing) {
            Image(systemName: "camera.circle.fill")
                .font(.title2)
                .foregroundStyle(.white, Color.brand)
        }
    }
}

// MARK: - Account

struct CreateAccountView: View {
    @Environment(RegistrationModel.self) private var registration
    @Environment(ToastCenter.self) private var toasts

    @State private var username = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var errors: [Field: String] = [:]
    @State private var isWorking = false

    enum Field: Hashable { case username, email, password, confirm }

    var body: some View {
        Form {
            Section {
                TextField(L("username"), text: $username)
                    .textContentType(.nickname)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if let error = errors[.username] { FieldError(text: error) }
                TextField(L("email"), text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if let error = errors[.email] { FieldError(text: error) }
                SecureField(L("password"), text: $password)
                    .textContentType(.newPassword)
                if let error = errors[.password] { FieldError(text: error) }
                SecureField(L("confirm_password"), text: $confirmPassword)
                    .textContentType(.newPassword)
                if let error = errors[.confirm] { FieldError(text: error) }
            }
        }
        .navigationTitle(L("create_an_account"))
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .top) { RegistrationProgress(step: 5) }
        .safeAreaInset(edge: .bottom) {
            Button(L("sign_up"), action: signUp)
                .buttonStyle(PrimaryButtonStyle())
                .disabled(isWorking)
                .padding()
                .background(.bar)
        }
        .overlay { if isWorking { LoadingOverlay() } }
    }

    private func signUp() {
        let name = username.trimmingCharacters(in: .whitespaces)
        let mail = email.trimmingCharacters(in: .whitespaces)
        var found: [Field: String] = [:]
        if name.isEmpty { found[.username] = L("validation_username_empty") }
        if mail.isEmpty {
            found[.email] = L("validation_email_empty")
        } else if !Validators.isValidEmail(mail) {
            found[.email] = L("validation_email_invalid")
        }
        if let problem = Validators.passwordProblem(password) {
            found[.password] = problem == "validation_password_min_length" ? LF(problem, 8) : L(problem)
        }
        if confirmPassword.isEmpty {
            found[.confirm] = L("validation_password_confirm_empty")
        } else if confirmPassword != password {
            found[.confirm] = L("validation_passwords_no_match")
        }
        errors = found
        guard found.isEmpty else { return }

        isWorking = true
        let data = registration.data
        Task {
            defer { isWorking = false }
            do {
                try await AuthService.createAccount(username: name, email: mail, password: password, registration: data)
                toasts.show(L("create_account_toast_signup_success"))
                // The auth listener now switches the app to the main screen.
            } catch {
                toasts.show(LF("create_account_toast_signup_failed_exception", error.localizedDescription))
            }
        }
    }
}
