import SwiftUI

struct SplashView: View {
    var animate: Bool
    @State private var appeared = false

    var body: some View {
        ZStack {
            Color.welcomeBackground.ignoresSafeArea()
            VStack(spacing: 16) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 180, height: 180)
                    .clipShape(Circle())
                    .scaleEffect(appeared || !animate ? 1 : 0.6)
                    .opacity(appeared || !animate ? 1 : 0)
                Text(L("app_name"))
                    .font(.brandScript(size: 52))
                    .foregroundStyle(Color.welcomeButtonDark)
                    .opacity(appeared || !animate ? 1 : 0)
            }
        }
        .onAppear {
            guard animate else { return }
            withAnimation(.spring(response: 0.7, dampingFraction: 0.65)) { appeared = true }
        }
    }
}

/// Steps pushed onto the signed-out navigation stack.
enum AuthStep: Hashable {
    case login
    case forgotPassword
    case country
    case cuisines
    case diets
    case profile
    case createAccount
}

/// Holds the answers from the sign-up steps until the account is created.
@Observable
@MainActor
final class RegistrationModel {
    var data = RegistrationData()
}

struct WelcomeView: View {
    @Environment(ToastCenter.self) private var toasts
    @State private var path: [AuthStep] = []
    @State private var registration = RegistrationModel()
    @State private var isWaving = false
    @State private var isSigningIn = false

    var body: some View {
        NavigationStack(path: $path) {
            ZStack {
                Color.welcomeBackground.ignoresSafeArea()
                VStack(spacing: 18) {
                    Spacer()
                    Text(L("welcome_emoji"))
                        .font(.system(size: 72))
                        .rotationEffect(.degrees(isWaving ? 18 : -12), anchor: .bottomTrailing)
                        .animation(.easeInOut(duration: 0.45).repeatCount(5, autoreverses: true), value: isWaving)
                    Text(L("welcome_to_nharmony_kitchen"))
                        .font(.largeTitle.bold())
                        .multilineTextAlignment(.center)
                        .foregroundStyle(Color.welcomeButtonDark)
                    Text(L("your_daily_dose_of_delicious"))
                        .font(.title3)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button(L("get_started")) { path.append(.country) }
                        .buttonStyle(PrimaryButtonStyle())
                    Button(L("i_already_have_an_account")) { path.append(.login) }
                        .buttonStyle(PrimaryButtonStyle(background: .welcomeButtonLight, foreground: Color(hex: 0x343A40)))
                    GoogleSignInButton(isWorking: $isSigningIn)
                }
                .padding(24)
            }
            .navigationDestination(for: AuthStep.self) { step in
                switch step {
                case .login: LoginView(path: $path)
                case .forgotPassword: ForgotPasswordView()
                case .country: CountryPickerView(mode: .register) { path.append(.cuisines) }
                case .cuisines: PreferenceGridView(kind: .cuisines, mode: .register) { path.append(.diets) }
                case .diets: PreferenceGridView(kind: .diets, mode: .register) { path.append(.profile) }
                case .profile: ProfileCompletionView { path.append(.createAccount) }
                case .createAccount: CreateAccountView()
                }
            }
        }
        .environment(registration)
        .onAppear { isWaving = true }
        .overlay { if isSigningIn { LoadingOverlay() } }
    }
}

struct GoogleSignInButton: View {
    @Environment(ToastCenter.self) private var toasts
    @Binding var isWorking: Bool

    var body: some View {
        Button {
            Task {
                isWorking = true
                defer { isWorking = false }
                do {
                    try await AuthService.signInWithGoogle()
                } catch {
                    // Closing the Google sheet is not an error worth showing.
                    if (error as NSError).code != -5 {
                        toasts.show(LF("google_sign_in_failed", error.localizedDescription))
                    }
                }
            }
        } label: {
            HStack(spacing: 10) {
                Text("G")
                    .font(.system(size: 18, weight: .bold, design: .rounded))
                    .foregroundStyle(.blue)
                    .frame(width: 28, height: 28)
                    .background(.white, in: Circle())
                Text(L("continue_with_google"))
            }
        }
        .buttonStyle(PrimaryButtonStyle(background: Color(uiColor: .secondarySystemBackground), foreground: .primary))
        .disabled(isWorking)
    }
}

struct LoginView: View {
    @Environment(ToastCenter.self) private var toasts
    @Binding var path: [AuthStep]

    @State private var email = ""
    @State private var password = ""
    @State private var emailError: String?
    @State private var passwordError: String?
    @State private var isWorking = false

    var body: some View {
        Form {
            Section {
                Text(L("please_enter_your_username_email_and_password_to_sign_in"))
                    .foregroundStyle(.secondary)
                    .listRowBackground(Color.clear)
            }
            Section {
                TextField(L("email"), text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if let emailError { FieldError(text: emailError) }
                SecureField(L("password"), text: $password)
                    .textContentType(.password)
                if let passwordError { FieldError(text: passwordError) }
            }
            Section {
                Button(L("log_in"), action: logIn)
                    .buttonStyle(PrimaryButtonStyle())
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                    .disabled(isWorking)
                Button(L("forgot_passwordbtn")) { path.append(.forgotPassword) }
                    .frame(maxWidth: .infinity)
            }
            Section(L("or_continue_with")) {
                GoogleSignInButton(isWorking: $isWorking)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
        }
        .navigationTitle(L("hey_there"))
        .overlay { if isWorking { LoadingOverlay() } }
    }

    private func logIn() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        // Firebase signs in with an email address only. (Android's form also accepted a
        // username, which always failed.)
        emailError = trimmedEmail.isEmpty ? L("validation_email_empty")
            : Validators.isValidEmail(trimmedEmail) ? nil : L("validation_email_invalid")
        passwordError = password.isEmpty ? L("validation_password_empty") : nil
        guard emailError == nil, passwordError == nil else { return }

        isWorking = true
        Task {
            defer { isWorking = false }
            do {
                try await AuthService.signIn(email: trimmedEmail, password: password)
            } catch {
                toasts.show(LF("login_auth_failed", error.localizedDescription))
            }
        }
    }
}

struct FieldError: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(.red)
    }
}

/// Firebase emails a reset link. (Android also had OTP and "new password" screens that only
/// pretended to work; they are not needed with Firebase's reset link.)
struct ForgotPasswordView: View {
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var error: String?
    @State private var isWorking = false

    var body: some View {
        Form {
            Section {
                Text(L("enter_your_email_address_we_will_send_a_link_for_verification"))
                    .foregroundStyle(.secondary)
                    .listRowBackground(Color.clear)
            }
            Section {
                TextField(L("email"), text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if let error { FieldError(text: error) }
            }
            Section {
                Button(L("send_link"), action: send)
                    .buttonStyle(PrimaryButtonStyle())
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                    .disabled(isWorking)
            }
        }
        .navigationTitle(L("forgot_password"))
        .overlay { if isWorking { LoadingOverlay() } }
    }

    private func send() {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        error = trimmed.isEmpty ? L("validation_email_empty")
            : Validators.isValidEmail(trimmed) ? nil : L("validation_email_invalid")
        guard error == nil else { return }
        isWorking = true
        Task {
            defer { isWorking = false }
            do {
                try await AuthService.sendPasswordReset(to: trimmed)
                toasts.show(L("forgot_password_link_sent"))
                dismiss()
            } catch {
                toasts.show(LF("forgot_password_link_failed", error.localizedDescription))
            }
        }
    }
}
