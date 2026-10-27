import FirebaseStorage
import Foundation
import Observation
import UIKit

/// Short messages shown at the bottom of the screen (Android's Toast and Snackbar).
@Observable
@MainActor
final class ToastCenter {
    struct Toast: Identifiable {
        let id = UUID()
        let message: String
        let actionTitle: String?
        let action: (() -> Void)?
    }

    private(set) var current: Toast?
    @ObservationIgnored private var dismissTask: Task<Void, Never>?

    func show(_ message: String, actionTitle: String? = nil, action: (() -> Void)? = nil) {
        let toast = Toast(message: message, actionTitle: actionTitle, action: action)
        current = toast
        dismissTask?.cancel()
        dismissTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(action == nil ? 2.5 : 4))
            guard !Task.isCancelled else { return }
            if self?.current?.id == toast.id { self?.current = nil }
        }
    }

    func performAction() {
        current?.action?()
        current = nil
    }

    func dismiss() {
        current = nil
    }
}

/// Past ingredient searches, saved as JSON on this device only (like Android's Room table).
@Observable
@MainActor
final class ScanHistoryStore {
    private(set) var items: [ScanHistoryItem] = []

    private let fileURL: URL = {
        let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory.appendingPathComponent("scan_history.json")
    }()

    init() {
        if let data = try? Data(contentsOf: fileURL),
           let saved = try? JSONDecoder().decode([ScanHistoryItem].self, from: data) {
            items = saved
        }
    }

    func add(ingredients: [String], imageUrl: String?) {
        let title = ingredients.prefix(3).joined(separator: ", ") + (ingredients.count > 3 ? "…" : "")
        items.insert(ScanHistoryItem(title: title, imageUrl: imageUrl, ingredients: ingredients), at: 0)
        if items.count > 50 { items.removeLast(items.count - 50) }
        save()
    }

    func delete(_ item: ScanHistoryItem) {
        items.removeAll { $0.id == item.id }
        save()
    }

    func clear() {
        items = []
        save()
    }

    private func save() {
        let snapshot = items
        let url = fileURL
        Task.detached(priority: .utility) {
            if let data = try? JSONEncoder().encode(snapshot) {
                try? data.write(to: url, options: .atomic)
            }
        }
    }
}

/// Uploads photos to Firebase Storage after shrinking them.
/// (Android uploaded full-size camera photos, often 3-5 MB each.)
enum ImageUploader {
    static func upload(_ image: UIImage, path: String, maxDimension: CGFloat = 1600) async throws -> String {
        guard let data = resized(image, maxDimension: maxDimension).jpegData(compressionQuality: 0.8) else {
            throw CocoaError(.fileWriteUnknown)
        }
        let ref = Storage.storage().reference(withPath: path)
        let metadata = StorageMetadata()
        metadata.contentType = "image/jpeg"
        _ = try await ref.putDataAsync(data, metadata: metadata)
        let url = try await ref.downloadURL()
        // The download URL stays the same when a file is replaced; a version parameter makes
        // image caches fetch the new picture.
        let separator = url.absoluteString.contains("?") ? "&" : "?"
        return url.absoluteString + "\(separator)v=\(Int(Date().timeIntervalSince1970))"
    }

    static func resized(_ image: UIImage, maxDimension: CGFloat) -> UIImage {
        let size = image.size
        let scale = min(1, maxDimension / max(size.width, size.height))
        guard scale < 1 else { return image }
        let target = CGSize(width: (size.width * scale).rounded(), height: (size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
