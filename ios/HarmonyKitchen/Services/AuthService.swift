import FirebaseAuth
import FirebaseCore
import Foundation
import GoogleSignIn
import UIKit

/// Data collected across the sign-up screens (Android's RegistrationData).
struct RegistrationData {
    var country: Country?
    var cuisines: [String] = []
    var diets: [String] = []
    var fullName = ""
    var phoneNumber = ""
    var gender: String?
    var dateOfBirth: Date?
    var profileImage: UIImage?
}

enum AuthError: LocalizedError {
    case missingGoogleClientID
    case missingGoogleToken
    case noWindow

    var errorDescription: String? {
        switch self {
        case .missingGoogleClientID: "Google Sign-In is not configured (GoogleService-Info.plist has no CLIENT_ID)."
        case .missingGoogleToken: "Google did not return an ID token."
        case .noWindow: "Could not find a window to present Google Sign-In."
        }
    }
}

/// Firebase Authentication: email/password, Google, and password reset.
enum AuthService {
    static var currentUser: User? { Auth.auth().currentUser }

    static func signIn(email: String, password: String) async throws {
        _ = try await Auth.auth().signIn(withEmail: email, password: password)
    }

    static func sendPasswordReset(to email: String) async throws {
        try await Auth.auth().sendPasswordReset(withEmail: email)
    }

    /// Creates the account, uploads the profile photo and writes `users/{uid}`.
    static func createAccount(username: String, email: String, password: String, registration: RegistrationData) async throws {
        let result = try await Auth.auth().createUser(withEmail: email, password: password)
        let uid = result.user.uid

        var imageURL: String?
        if let image = registration.profileImage {
            // A failed upload should not block the sign-up; the photo can be added later.
            imageURL = try? await ImageUploader.upload(image, path: "profile_pictures/\(uid)/profile.jpg", maxDimension: 800)
        }

        var data: [String: Any] = [
            "id": uid,
            "username": username,
            "email": email,
            "full_name": registration.fullName,
            "phone_number": registration.phoneNumber,
            "selected_cuisines": registration.cuisines,
            "selected_diets": registration.diets,
        ]
        if let country = registration.country?.englishName { data["country"] = country }
        if let gender = registration.gender { data["gender"] = gender }
        if let dateOfBirth = registration.dateOfBirth { data["date_of_birth"] = Self.formatDateOfBirth(dateOfBirth) }
        if let imageURL { data["profileImageUrl"] = imageURL }
        try await FS.set(data, at: FS.user(uid))
    }

    /// Signs in with Google. New users get the same default profile Android created.
    @MainActor
    static func signInWithGoogle() async throws {
        guard let clientID = FirebaseApp.app()?.options.clientID else { throw AuthError.missingGoogleClientID }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        guard let presenter = topViewController() else { throw AuthError.noWindow }

        let googleResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        guard let idToken = googleResult.user.idToken?.tokenString else { throw AuthError.missingGoogleToken }
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: googleResult.user.accessToken.tokenString
        )
        let authResult = try await Auth.auth().signIn(with: credential)
        guard authResult.additionalUserInfo?.isNewUser == true else { return }

        let user = authResult.user
        let username = user.email?.split(separator: "@").first.map(String.init) ?? "user_\(user.uid.prefix(6))"
        try await FS.set([
            "id": user.uid,
            "full_name": user.displayName ?? "",
            "email": user.email ?? "",
            "username": username,
            // Android wrote "selected_country" here; "country" is what the rest of the app reads.
            "country": "South Africa",
            "selected_cuisines": CuisineCatalog.all.map(\.id),
            "selected_diets": [String](),
        ], at: FS.user(user.uid))
    }

    static func signOut() {
        GIDSignIn.sharedInstance.signOut()
        try? Auth.auth().signOut()
    }

    /// Same "d/M/yyyy" format the Android date picker produced.
    static func formatDateOfBirth(_ date: Date) -> String {
        let parts = Calendar(identifier: .gregorian).dateComponents([.day, .month, .year], from: date)
        return "\(parts.day ?? 1)/\(parts.month ?? 1)/\(parts.year ?? 2000)"
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive } ?? UIApplication.shared.connectedScenes.first as? UIWindowScene
        var controller = scene?.keyWindow?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
