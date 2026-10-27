# Harmony Kitchen for iOS

A native SwiftUI version of the Harmony Kitchen Android app (OPSC6312, RST Innovations). It uses
the **same Firebase project and the same Firestore data** as the Android app, so accounts, recipes,
favourites, shopping lists, badges and notifications are shared between the two.

See [FIXES.md](FIXES.md) for the bugs and performance problems found in the Android app, and which
of them are fixed where.

## What you need

- A Mac (or macOS virtual machine) with **Xcode 16** or later. iOS apps cannot be built on
  Windows, and installing Swift on Windows does not help: Xcode already includes Swift.
- [Homebrew](https://brew.sh) and XcodeGen: `brew install xcodegen`
- An iPhone on **iOS 17** or later, or the iOS Simulator that comes with Xcode

## Run it

1. Clone the repository and switch to this branch.
2. Copy `GoogleService-Info.plist` into `ios/HarmonyKitchen/Resources/`. It is the file from
   Firebase console → Project settings → Your apps → the iOS app (bundle ID
   `com.rst.recipeappopsc6312`). It is git-ignored, so it never gets committed.
3. Generate and open the Xcode project:
   ```bash
   cd ios
   ```
   ```bash
   xcodegen generate
   ```
   ```bash
   open HarmonyKitchen.xcodeproj
   ```
   Xcode downloads the Firebase and Google Sign-In packages the first time (a few minutes).
   Firebase is already added to the project and `FirebaseApp.configure()` is already called, so
   skip those steps in the Firebase console's setup guide.
4. **Simulator:** choose an iPhone in the device menu at the top of Xcode and press ▶ (⌘R).
5. **Your iPhone:**
   - Xcode → Settings → Accounts → add your Apple ID (a free one works).
   - Select the *HarmonyKitchen* target → *Signing & Capabilities* → *Team*: your name.
   - Connect the iPhone with a cable (in a VM, pass the USB device through to macOS) and tap
     *Trust* on the phone.
   - On the phone: Settings → Privacy & Security → Developer Mode → on, then restart it.
   - Choose the iPhone in Xcode's device menu and press ▶.
   - With a free Apple ID the app stops opening after 7 days; press ▶ again to reinstall.

Optional: to let search fall back to Spoonacular and Tasty, copy `Config/Secrets.example.xcconfig`
to `Config/Secrets.xcconfig` and add the keys (also git-ignored).

If `GoogleService-Info.plist` is missing, the app opens on a "Firebase setup needed" screen instead
of crashing.

## Tests and checks

- In Xcode: ⌘U. They cover unit conversion and fractions, servings scaling, diet filtering,
  ingredient matching, recommendations, login streaks, Firestore field mapping, API mapping and
  form validation.
- `node scripts/check-project.mjs` runs quick checks that work on Windows too: every localization
  key exists, every image exists, and brackets balance.
- `.github/workflows/ios.yml` builds the app and runs the tests on a GitHub-hosted Mac whenever
  something in `ios/` changes, so you can see whether it compiles without owning a Mac
  (repository → *Actions* tab → *iOS*).

## Firebase

- `firebase/firestore.indexes.json`: indexes for Discover's sort orders. Deploy with
  `firebase deploy --only firestore:indexes`. Without them the app sorts on the device instead.
- `firebase/firestore.rules`, `firebase/storage.rules`: suggested security rules. Compare them with
  your current rules first, because they apply to the Android app too.

## Project layout

```
project.yml                 XcodeGen spec (targets, packages, Info.plist)
Config/                     Build settings; Secrets.xcconfig holds optional keys (git-ignored)
HarmonyKitchen/
  App/                      App entry, Firebase start-up, sign-in routing, push notifications
  Models/                   Recipe (Firestore-compatible), user data, cuisines/diets/countries
  Logic/                    Pure Swift: units, diets, recommendations, API mapping, validation
  Services/                 Firestore, recipe repository, user/shopping stores, images, Vision, speech
  Features/                 SwiftUI screens, one folder per area
  Resources/                Assets, fonts, Localizable.xcstrings (EN, Sepedi, Xitsonga, Tshivenda)
HarmonyKitchenTests/        Unit tests
firebase/                   Firestore indexes and suggested security rules
scripts/                    Android strings converter and static checks
```

## Android to iOS

| Android | iOS |
|---------|-----|
| Activities + Fragments + XML layouts | SwiftUI views, `NavigationStack`, `TabView` |
| ViewModel + LiveData | `@Observable` stores and view state |
| Room (favourites, shopping lists, recipes) | Firestore's offline cache with live listeners |
| SharedPreferences | `@AppStorage` (UserDefaults) |
| Retrofit + Gson | `URLSession` + `Codable` |
| Glide | `ImagePipeline` (downsampling, memory + disk cache) |
| ML Kit object detection + text recognition | Apple Vision (on device, no extra SDK) |
| `RecognizerIntent` voice input | `Speech` framework |
| CameraX | System camera + `PhotosPicker` |
| restcountries.com download | Country list built into iOS, with flag emoji |
| ShowcaseView tour | First-launch tour sheet |
| Lottie animations | SF Symbols and system progress views |
| `res/values-*/strings.xml` | `Localizable.xcstrings`, generated by `scripts/convert-android-strings.mjs` |
| In-app language picker | iOS Settings → Harmony Kitchen → Language (the iOS-standard place) |
| Cloudinary "upscaling" | Removed (see FIXES.md) |

To refresh the translations after editing the Android strings (from `ios/`):

```bash
node scripts/convert-android-strings.mjs ../app/src/main/res .
```

iOS-only strings live in `scripts/ios-strings.json` (English only for now).

## Before the App Store

- **Sign in with Apple.** Apple requires it when an app offers Google sign-in. It needs a paid
  Apple Developer account and the "Sign in with Apple" provider enabled in Firebase.
- **Push notifications.** In-app notifications work as is. For real push messages (the
  `new_recipes` topic), add the *Push Notifications* capability in Xcode and upload an APNs key in
  Firebase → Project settings → Cloud Messaging. Both need a paid developer account.
- **API keys.** Any key built into an app can be extracted from it. Move the Spoonacular and Tasty
  calls into a Cloud Function before release.
- **Crashlytics.** Crash reports are collected; to symbolicate them, add Firebase's
  `upload-symbols` run script as described in the Crashlytics setup guide.
