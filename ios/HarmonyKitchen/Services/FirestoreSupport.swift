import FirebaseFirestore
import Foundation

/// Firestore paths. Identical to the Android app so both apps share data.
enum FS {
    static var db: Firestore { Firestore.firestore() }

    static var recipes: CollectionReference { db.collection("recipes") }
    static func user(_ uid: String) -> DocumentReference { db.collection("users").document(uid) }
    static func favorites(_ uid: String) -> CollectionReference { user(uid).collection("favorites") }
    static func activityLog(_ uid: String) -> CollectionReference { user(uid).collection("activity_log") }
    static func progress(_ uid: String) -> DocumentReference { user(uid).collection("progress").document("main") }
    static func notifications(_ uid: String) -> CollectionReference { user(uid).collection("notifications") }
    static func shoppingLists(_ uid: String) -> CollectionReference { user(uid).collection("shopping_lists") }
    static func items(_ uid: String, listID: String) -> CollectionReference {
        shoppingLists(uid).document(listID).collection("items")
    }

    /// Enables the on-disk cache with no size limit, so favourites and shopping lists stay
    /// available offline (Android kept a separate Room copy for this). Must run before any query.
    static func configureCache() {
        let settings = FirestoreSettings()
        settings.cacheSettings = PersistentCacheSettings(sizeBytes: NSNumber(value: FirestoreCacheSizeUnlimited))
        db.settings = settings
    }

    // MARK: Writes
    //
    // The async Firestore write APIs wait for the server to acknowledge, which never happens
    // offline (Android's save spinner hung forever without a connection). Offline, these helpers
    // queue the write, which is applied to the local cache at once, and return immediately.

    static func set(_ data: [String: Any], at ref: DocumentReference, merge: Bool = false) async throws {
        if NetworkMonitor.currentlyOnline {
            try await ref.setData(data, merge: merge)
        } else {
            ref.setData(data, merge: merge, completion: nil)
        }
    }

    static func update(_ data: [AnyHashable: Any], at ref: DocumentReference) async throws {
        if NetworkMonitor.currentlyOnline {
            try await ref.updateData(data)
        } else {
            ref.updateData(data, completion: nil)
        }
    }

    static func delete(_ ref: DocumentReference) async throws {
        if NetworkMonitor.currentlyOnline {
            try await ref.delete()
        } else {
            ref.delete(completion: nil)
        }
    }

    static func commit(_ batch: WriteBatch) async throws {
        if NetworkMonitor.currentlyOnline {
            try await batch.commit()
        } else {
            batch.commit(completion: nil)
        }
    }

    // MARK: Reads

    /// Reads from the server when online and straight from the cache when offline.
    /// (Firestore's default source waits several seconds for the network before falling back.)
    /// Other server errors, such as a missing index or a permission problem, are passed on
    /// rather than hidden behind possibly incomplete cached results.
    static func getDocuments(_ query: Query) async throws -> QuerySnapshot {
        if NetworkMonitor.currentlyOnline {
            do {
                return try await query.getDocuments()
            } catch let error where isConnectivityError(error) {
                return try await query.getDocuments(source: .cache)
            }
        }
        return try await query.getDocuments(source: .cache)
    }

    static func getDocument(_ ref: DocumentReference) async throws -> DocumentSnapshot {
        if NetworkMonitor.currentlyOnline {
            do {
                return try await ref.getDocument()
            } catch let error where isConnectivityError(error) {
                return try await ref.getDocument(source: .cache)
            }
        }
        return try await ref.getDocument(source: .cache)
    }

    /// Firestore's UNAVAILABLE (14) and DEADLINE_EXCEEDED (4) mean the server could not be reached.
    private static func isConnectivityError(_ error: Error) -> Bool {
        let nsError = error as NSError
        return nsError.domain == "FIRFirestoreErrorDomain" && [4, 14].contains(nsError.code)
    }

    // MARK: Coding

    static func recipe(from document: DocumentSnapshot) -> Recipe? {
        guard document.exists, var recipe = try? document.data(as: Recipe.self) else { return nil }
        if recipe.id.isEmpty { recipe.id = document.documentID }
        return recipe
    }

    static func recipes(from snapshot: QuerySnapshot) -> [Recipe] {
        snapshot.documents.compactMap { recipe(from: $0) }
    }

    static func encode(_ recipe: Recipe) throws -> [String: Any] {
        try Firestore.Encoder().encode(recipe)
    }
}
