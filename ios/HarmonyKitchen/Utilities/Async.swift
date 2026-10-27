import Foundation
import os

/// Waits until `operation` finishes or `seconds` pass, whichever comes first.
/// The operation is not cancelled on timeout; it keeps running in the background.
func waitAtMost(seconds: Double, for operation: @escaping @Sendable () async -> Void) async {
    await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
        let resumed = OSAllocatedUnfairLock(initialState: false)
        let resumeOnce: @Sendable () -> Void = {
            let first = resumed.withLock { alreadyResumed -> Bool in
                defer { alreadyResumed = true }
                return !alreadyResumed
            }
            if first { continuation.resume() }
        }
        Task {
            await operation()
            resumeOnce()
        }
        Task {
            try? await Task.sleep(for: .seconds(seconds))
            resumeOnce()
        }
    }
}
