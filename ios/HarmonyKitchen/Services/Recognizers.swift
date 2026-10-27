import AVFoundation
import Foundation
import Observation
import Speech
import UIKit
import Vision

/// Finds ingredient names in a photo, on the device, with Apple's Vision framework.
///
/// Two passes run together: image classification (a bowl of tomatoes -> "tomato") and text
/// recognition (a shopping list or receipt). Words are kept only if they appear in the
/// ingredient lists of known recipes, so "TOTAL" or "R12.99" from a receipt are ignored.
enum IngredientRecognizer {
    private static let genericLabels: Set<String> = [
        "food", "fruit", "vegetable", "produce", "meal", "dish", "plate", "table", "tableware",
        "cookware", "kitchen", "indoor", "outdoor", "structure", "container", "people", "adult",
        "baked_goods", "dessert", "drink", "beverage", "liquid", "materials", "wood_processed",
        "textile", "paper", "document", "printed_page", "screenshot", "consumer_electronics",
        "utensil", "bowl", "cup", "jar", "bottle", "box", "bag", "can", "sign", "text",
    ]

    /// Words that appear in ingredient names but are not ingredients on their own.
    private static let ignoredWords: Set<String> = [
        "fresh", "large", "small", "medium", "ground", "chopped", "sliced", "diced", "minced", "whole",
        "dried", "frozen", "canned", "cup", "cups", "and", "for", "the", "with", "pack", "total",
        "price", "each", "free", "low", "fat", "extra", "virgin", "light", "dark", "brown", "white",
        "red", "green", "yellow", "black", "fine", "finely", "plain", "raw", "cooked", "boneless",
        "skinless", "optional", "taste", "pinch", "piece", "pieces", "tin", "tins", "can", "cans",
    ]

    static func ingredients(in image: UIImage, vocabulary: Set<String>) async -> [String] {
        guard let cgImage = image.cgImage else { return [] }
        let orientation = cgOrientation(image.imageOrientation)
        return await Task.detached(priority: .userInitiated) {
            Self.recognize(cgImage: cgImage, orientation: orientation, vocabulary: vocabulary)
        }.value
    }

    private static func cgOrientation(_ orientation: UIImage.Orientation) -> CGImagePropertyOrientation {
        switch orientation {
        case .up: .up
        case .upMirrored: .upMirrored
        case .down: .down
        case .downMirrored: .downMirrored
        case .left: .left
        case .leftMirrored: .leftMirrored
        case .right: .right
        case .rightMirrored: .rightMirrored
        @unknown default: .up
        }
    }

    private static func recognize(cgImage: CGImage, orientation: CGImagePropertyOrientation, vocabulary: Set<String>) -> [String] {
        let classify = VNClassifyImageRequest()
        let text = VNRecognizeTextRequest()
        text.recognitionLevel = .accurate
        text.usesLanguageCorrection = true

        let handler = VNImageRequestHandler(cgImage: cgImage, orientation: orientation)
        do {
            try handler.perform([classify, text])
        } catch {
            print("Vision failed: \(error)")
        }

        var found: [String] = []
        for observation in classify.results ?? [] where observation.confidence >= 0.3 {
            guard !genericLabels.contains(observation.identifier) else { continue }
            let label = observation.identifier.replacingOccurrences(of: "_", with: " ")
            let keep = vocabulary.isEmpty ? observation.confidence >= 0.5 : isKnown(label, in: vocabulary)
            if keep { found.append(label) }
        }

        for observation in text.results ?? [] {
            guard let line = observation.topCandidates(1).first?.string.lowercased() else { continue }
            for token in line.split(whereSeparator: { !$0.isLetter }) where token.count >= 3 {
                let word = String(token)
                guard !ignoredWords.contains(word) else { continue }
                if vocabulary.isEmpty || isKnown(word, in: vocabulary) { found.append(word) }
            }
        }

        var seen = Set<String>()
        return found
            .filter { seen.insert($0).inserted }
            .map(TextCleaner.capitalizedFirst)
    }

    private static func isKnown(_ phrase: String, in vocabulary: Set<String>) -> Bool {
        phrase.split(separator: " ").contains { part in
            let word = String(part)
            return vocabulary.contains(word) || vocabulary.contains(word + "s")
                || vocabulary.contains(String(word.dropLast())) || vocabulary.contains(String(word.dropLast(2)))
        }
    }
}

/// Live speech-to-text for saying ingredients (Android used the system RecognizerIntent).
@Observable
@MainActor
final class SpeechRecognizer {
    enum SpeechError: LocalizedError {
        case notAuthorized
        case unavailable

        var errorDescription: String? { L("scan_manual_voice_not_available") }
    }

    private(set) var transcript = ""
    private(set) var isRecording = false

    @ObservationIgnored private var engine: AVAudioEngine?
    @ObservationIgnored private var request: SFSpeechAudioBufferRecognitionRequest?
    @ObservationIgnored private var task: SFSpeechRecognitionTask?

    func start() async throws {
        guard await Self.requestPermissions() else { throw SpeechError.notAuthorized }
        // English, as on Android: ingredient names match the English recipe data.
        guard let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US")), recognizer.isAvailable else {
            throw SpeechError.unavailable
        }
        stopAudio()
        transcript = ""

        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement, options: .duckOthers)
        try session.setActive(true, options: .notifyOthersOnDeactivation)

        let engine = AVAudioEngine()
        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        let input = engine.inputNode
        input.installTap(onBus: 0, bufferSize: 1024, format: input.outputFormat(forBus: 0)) { buffer, _ in
            request.append(buffer)
        }
        engine.prepare()
        try engine.start()

        self.engine = engine
        self.request = request
        isRecording = true
        task = recognizer.recognitionTask(with: request) { [weak self] result, error in
            let text = result?.bestTranscription.formattedString
            let finished = error != nil || (result?.isFinal ?? false)
            Task { @MainActor in
                if let text { self?.transcript = text }
                if finished { self?.stopAudio() }
            }
        }
    }

    /// Stops listening and returns what was heard.
    @discardableResult
    func stop() -> String {
        request?.endAudio()
        stopAudio()
        return transcript
    }

    private func stopAudio() {
        engine?.stop()
        engine?.inputNode.removeTap(onBus: 0)
        engine = nil
        request = nil
        task?.cancel()
        task = nil
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private static func requestPermissions() async -> Bool {
        let speech = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { continuation.resume(returning: $0 == .authorized) }
        }
        guard speech else { return false }
        return await AVAudioApplication.requestRecordPermission()
    }

    /// Splits "chicken breast and rice, onions" into ["Chicken breast", "Rice", "Onions"].
    /// Without "and" or commas, each word is its own ingredient (as on Android).
    nonisolated static func ingredients(fromSpeech text: String) -> [String] {
        let lowered = text.lowercased()
        let parts: [String]
        if lowered.contains(",") || lowered.contains(" and ") {
            parts = lowered
                .replacingOccurrences(of: " and ", with: ",")
                .split(separator: ",")
                .map(String.init)
        } else {
            parts = lowered.split(separator: " ").map(String.init)
        }
        return parts
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines.union(.punctuationCharacters)) }
            .filter { !$0.isEmpty }
            .map(TextCleaner.capitalizedFirst)
    }
}
