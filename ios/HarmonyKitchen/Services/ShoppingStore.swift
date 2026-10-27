import FirebaseFirestore
import Foundation
import Observation

enum ShoppingSelection: Hashable {
    case all
    case list(String)
}

/// Shopping lists, stored in Firestore under the user (same paths as Android) and kept live.
///
/// Fixes from the Android version: "My List" is now a real document under the user, so its
/// items sync to other devices (Android never wrote the list document, only its items, and its
/// fixed "MY_LIST_ID" was shared by every account on the phone); it is no longer shown twice;
/// and duplicate checks ignore case.
@Observable
@MainActor
final class ShoppingStore {
    private(set) var lists: [ShoppingList] = []
    private(set) var itemsByList: [String: [ShoppingItem]] = [:]

    @ObservationIgnored private var uid: String?
    @ObservationIgnored private var listsListener: ListenerRegistration?
    @ObservationIgnored private var itemListeners: [String: ListenerRegistration] = [:]

    private static let emojis = ["🥞", "🍕", "🍔", "🍣", "🌮", "🥗", "🍜", "🍰", "🍩", "🥐", "🍝", "🍛", "🍲"]

    var myList: ShoppingList? { lists.first(where: \.isMyList) }

    func start(uid: String) {
        stop()
        self.uid = uid
        // Creates "My List" if missing; merge keeps any existing data.
        FS.shoppingLists(uid).document(ShoppingList.myListID).setData([
            "listId": ShoppingList.myListID,
            "name": "My List",
            "userId": uid,
            "emoji": "📝",
        ], merge: true, completion: nil)

        listsListener = FS.shoppingLists(uid).addSnapshotListener { [weak self] snapshot, _ in
            guard let snapshot else { return }
            let lists = snapshot.documents.map { document -> ShoppingList in
                let data = document.data()
                return ShoppingList(
                    id: document.documentID,
                    name: data["name"] as? String ?? "",
                    userId: data["userId"] as? String ?? uid,
                    emoji: data["emoji"] as? String ?? "🛒"
                )
            }
            Task { @MainActor in self?.apply(lists: lists) }
        }
    }

    func stop() {
        listsListener?.remove()
        listsListener = nil
        itemListeners.values.forEach { $0.remove() }
        itemListeners = [:]
        uid = nil
        lists = []
        itemsByList = [:]
    }

    private func apply(lists newLists: [ShoppingList]) {
        guard let uid else { return }
        lists = newLists.sorted { lhs, rhs in
            if lhs.isMyList != rhs.isMyList { return lhs.isMyList }
            return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
        }
        let ids = Set(newLists.map(\.id))
        for (id, listener) in itemListeners where !ids.contains(id) {
            listener.remove()
            itemListeners[id] = nil
            itemsByList[id] = nil
        }
        for id in ids where itemListeners[id] == nil {
            itemListeners[id] = FS.items(uid, listID: id).addSnapshotListener { [weak self] snapshot, _ in
                guard let snapshot else { return }
                let items = snapshot.documents.map { Self.item(from: $0, listID: id) }
                Task { @MainActor in self?.itemsByList[id] = items }
            }
        }
    }

    nonisolated private static func item(from document: QueryDocumentSnapshot, listID: String) -> ShoppingItem {
        let data = document.data()
        return ShoppingItem(
            id: document.documentID,
            ownerListId: data["ownerListId"] as? String ?? listID,
            name: data["name"] as? String ?? "",
            // Kotlin's isChecked is stored as "checked".
            isChecked: data["checked"] as? Bool ?? data["isChecked"] as? Bool ?? false
        )
    }

    // MARK: - Reading

    /// Unchecked items first, then alphabetical.
    func items(for selection: ShoppingSelection) -> [ShoppingItem] {
        let items: [ShoppingItem]
        switch selection {
        case .all: items = itemsByList.values.flatMap { $0 }
        case .list(let id): items = itemsByList[id] ?? []
        }
        return items.sorted { lhs, rhs in
            if lhs.isChecked != rhs.isChecked { return !lhs.isChecked }
            return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
        }
    }

    func list(id: String) -> ShoppingList? {
        lists.first { $0.id == id }
    }

    // MARK: - Writing

    /// Adds items that are not already on the list. Returns how many were added.
    @discardableResult
    func addItems(_ names: [String], toList listID: String) async -> Int {
        guard let uid else { return 0 }
        var existing = Set((itemsByList[listID] ?? []).map { Self.key($0.name) })
        let batch = FS.db.batch()
        var added = 0
        for rawName in names {
            let name = rawName.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty, existing.insert(Self.key(name)).inserted else { continue }
            let ref = FS.items(uid, listID: listID).document()
            batch.setData([
                "itemId": ref.documentID,
                "ownerListId": listID,
                "name": name,
                "checked": false,
            ], forDocument: ref)
            added += 1
        }
        guard added > 0 else { return 0 }
        try? await FS.commit(batch)
        return added
    }

    /// Adds a recipe's ingredients to a list named after the recipe, creating it if needed.
    @discardableResult
    func addRecipeIngredients(title: String, ingredients: [String]) async -> Int {
        guard let uid else { return 0 }
        let listID: String
        if let existing = lists.first(where: { $0.name == title }) {
            listID = existing.id
        } else {
            let ref = FS.shoppingLists(uid).document()
            listID = ref.documentID
            try? await FS.set([
                "listId": listID,
                "name": title,
                "userId": uid,
                "emoji": Self.emojis.randomElement() ?? "🛒",
            ], at: ref)
        }
        return await addItems(ingredients, toList: listID)
    }

    func setChecked(_ item: ShoppingItem, _ checked: Bool) {
        guard let uid else { return }
        FS.items(uid, listID: item.ownerListId).document(item.id).setData(["checked": checked], merge: true, completion: nil)
    }

    func delete(_ items: [ShoppingItem]) {
        guard let uid, !items.isEmpty else { return }
        let batch = FS.db.batch()
        for item in items {
            batch.deleteDocument(FS.items(uid, listID: item.ownerListId).document(item.id))
        }
        batch.commit(completion: nil)
    }

    /// Puts back an item removed with a swipe (the Undo action).
    func restore(_ item: ShoppingItem) {
        guard let uid else { return }
        FS.items(uid, listID: item.ownerListId).document(item.id).setData([
            "itemId": item.id,
            "ownerListId": item.ownerListId,
            "name": item.name,
            "checked": item.isChecked,
        ], completion: nil)
    }

    /// Deletes a recipe list and its items. "My List" cannot be deleted.
    func deleteList(_ list: ShoppingList) {
        guard let uid, !list.isMyList else { return }
        let batch = FS.db.batch()
        for item in itemsByList[list.id] ?? [] {
            batch.deleteDocument(FS.items(uid, listID: list.id).document(item.id))
        }
        batch.deleteDocument(FS.shoppingLists(uid).document(list.id))
        batch.commit(completion: nil)
    }

    private static func key(_ name: String) -> String {
        name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
}
