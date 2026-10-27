import ImageIO
import SwiftUI
import UIKit

/// Loads remote images, downsampled to the size they are shown at.
///
/// Decoded images are kept in a memory cache; the raw bytes go to a 250 MB disk cache so
/// recipe photos still show offline. Concurrent requests for the same image share one download.
actor ImagePipeline {
    static let shared = ImagePipeline()

    private let memory = NSCache<NSString, UIImage>()
    private var inFlight: [String: Task<UIImage?, Never>] = [:]
    private let session: URLSession

    private init() {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = URLCache(memoryCapacity: 16 * 1024 * 1024, diskCapacity: 250 * 1024 * 1024)
        configuration.timeoutIntervalForRequest = 20
        session = URLSession(configuration: configuration)
        memory.totalCostLimit = 64 * 1024 * 1024
    }

    func image(for url: URL, maxPixelSize: CGFloat) async -> UIImage? {
        let key = "\(url.absoluteString)#\(Int(maxPixelSize))"
        if let cached = memory.object(forKey: key as NSString) { return cached }
        if let running = inFlight[key] { return await running.value }

        let session = self.session
        // Detached so downloads and decodes run in parallel instead of one at a time on this actor.
        let task = Task<UIImage?, Never>.detached(priority: .userInitiated) {
            var request = URLRequest(url: url)
            // Offline, serve whatever is on disk instead of failing.
            request.cachePolicy = NetworkMonitor.currentlyOnline ? .useProtocolCachePolicy : .returnCacheDataDontLoad
            guard let (data, _) = try? await session.data(for: request) else { return nil }
            return Self.downsample(data, maxPixelSize: maxPixelSize)
        }
        inFlight[key] = task
        let image = await task.value
        inFlight[key] = nil
        if let image, let cgImage = image.cgImage {
            memory.setObject(image, forKey: key as NSString, cost: cgImage.bytesPerRow * cgImage.height)
        }
        return image
    }

    /// Decodes straight to a thumbnail so a 4000px photo never gets decoded at full size.
    nonisolated static func downsample(_ data: Data, maxPixelSize: CGFloat) -> UIImage? {
        let sourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
        guard let source = CGImageSourceCreateWithData(data as CFData, sourceOptions) else { return nil }
        let options = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
        ] as CFDictionary
        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, options) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}

/// A remote photo that fills whatever frame it is given (callers set the size).
struct RemoteImage: View {
    let urlString: String?
    /// Longest side to decode, in pixels.
    var maxPixelSize: CGFloat = 700

    @State private var image: UIImage?
    @State private var failed = false

    var body: some View {
        Color.chip
            .overlay {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .transition(.opacity)
                } else if failed {
                    Image("ic_placeholder")
                        .resizable()
                        .scaledToFit()
                        .padding(24)
                        .opacity(0.6)
                } else {
                    ProgressView()
                }
            }
            .clipped()
            .task(id: urlString) {
                await load()
            }
    }

    private func load() async {
        image = nil
        failed = false
        guard let urlString, let url = URL(string: urlString), !urlString.isEmpty else {
            failed = true
            return
        }
        let loaded = await ImagePipeline.shared.image(for: url, maxPixelSize: maxPixelSize)
        guard !Task.isCancelled else { return }
        withAnimation(.easeOut(duration: 0.2)) {
            image = loaded
            failed = loaded == nil
        }
    }
}
