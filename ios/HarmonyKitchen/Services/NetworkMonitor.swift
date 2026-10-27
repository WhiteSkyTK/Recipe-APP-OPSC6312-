import Foundation
import Network
import Observation
import os

/// Tracks connectivity for the UI (offline banner) and for the data layer (cache-only reads).
@Observable
@MainActor
final class NetworkMonitor {
    private(set) var isOnline = true

    /// Thread-safe copy of the latest status, readable from actors and background code.
    nonisolated static var currentlyOnline: Bool { latest.withLock { $0 } }
    nonisolated private static let latest = OSAllocatedUnfairLock(initialState: true)

    private let monitor = NWPathMonitor()

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            let online = path.status == .satisfied
            NetworkMonitor.latest.withLock { $0 = online }
            Task { @MainActor in
                self?.isOnline = online
            }
        }
        monitor.start(queue: DispatchQueue(label: "NetworkMonitor", qos: .utility))
    }
}
