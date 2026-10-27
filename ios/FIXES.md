# Fixes and optimizations

Problems found in the Android app (`WhiteSkyTK/Recipe-APP-OPSC6312-`, commit `35f24be`). Every item is
handled in the iOS app, and the branch `feature/ios-app-and-android-fixes` also fixes them in the Android
code, except those listed under [Not changed in the Android code](#not-changed-in-the-android-code).
File references are to `app/src/main/java/com/rst/recipeappopsc6312/` at commit `35f24be`.

## Security (act on these first)

| # | Problem | Where | What to do |
|---|---------|-------|------------|
| S1 | Release keystore passwords and the Supabase key are committed to the public repo. `gradle.properties` was added to `.gitignore` *after* it was committed, so it is still tracked. | `gradle.properties` | `git rm --cached gradle.properties`, change the keystore passwords, and treat the old ones as public. |
| S2 | The Cloudinary **API secret** is compiled into the APK through `BuildConfig.CLOUDINARY_URL`. Anyone with the APK can upload to or delete from that Cloudinary account. | `app/build.gradle.kts`, `RecipeApplication.kt:15` | Rotate the Cloudinary secret. The iOS app does not use Cloudinary at all. |
| S3 | Spoonacular and RapidAPI keys ship inside the APK (`BuildConfig`) and are logged in full by an HTTP logger set to `BODY` in release builds. | `RetrofitClient.kt:14`, `RetrofitClient.kt:26` | Rotate the keys if the APK was ever shared. iOS still ships optional keys (see README), so the proper fix is a Cloud Function that holds the keys. |

## Build problems

| # | Problem | Where | iOS |
|---|---------|-------|-----|
| B1 | The `build.gradle.kts` in your local "New folder" references Supabase libraries that are not in `libs.versions.toml`, so Gradle sync fails. The GitHub version has removed them. | local `build.gradle.kts` | n/a |
| B2 | `local.properties` has no `spoonacular.api.key6` or `tasty.api.key1`, so those `BuildConfig` fields become the literal text `"null"` and are used as API keys. | `app/build.gradle.kts`, `ShoppingRepository.kt:108-118` | Keys come from one comma-separated setting; empty values are ignored. |

## Bugs

| # | Problem | Where | iOS fix |
|---|---------|-------|---------|
| 1 | Popular recipes are queried as `isPopular`, but Kotlin's `isPopular` is stored as `popular`. Featured always falls back to random recipes, and Discover's "Popular" sort returns **nothing** (ordering by a missing field excludes every document). | `ShoppingRepository.kt:514`, `:783`, `:1042` | Queries `popular`. |
| 2 | Notifications are written with `isRead`, but the Kotlin model reads `read`, so every notification always looks unread and the red dot never clears. | `ShoppingRepository.kt:949`, `:1144`, `MyFirebaseMessagingService.kt:41` | Reads either field; marking as read writes both, which also fixes the Android display. |
| 3 | Each favourite is counted twice (in the repository and again in the ViewModel), so badges unlock at half the intended count and un-favouriting subtracts twice. | `BaseRecipeViewModel.kt:25`, `ShoppingRepository.kt:933`, `:938` | Counted once. |
| 4 | Every recipe *edit* is counted as a newly created recipe for badges. | `ShoppingRepository.kt:759` | Only new recipes count. |
| 5 | Editing a recipe that has a photo always fails: the existing `https://` URL is passed to `putFile`, which only accepts local files. | `AddRecipeFragment.kt` (populateForm), `ShoppingRepository.kt:752` | Only a newly picked photo is uploaded. |
| 6 | Editing a published recipe makes it private again, because the form rebuilds the recipe with `isPublic = false`. | `AddRecipeFragment.kt:468` | Edits keep visibility, owner and API flags. |
| 7 | Recipe view time is never logged: `onCreate` declares a local `recipeId` that shadows the property, so the property stays `null` and the recommendation engine's "viewed" signal never fires. | `RecipeDetailActivity.kt:45`, `:127`, `:153` | Logged when leaving the recipe screen. |
| 8 | Search queries are never logged, so the recommendation boost for searched terms never applies. | `ShoppingRepository.kt` (`SEARCH_QUERY` is read, never written) | Searches are logged. |
| 9 | Changing servings divides by the original servings; Tasty recipes with no servings (0) produce `Infinity`/`NaN` quantities. | `RecipeDetailActivity.kt:417` | The servings control is hidden when servings are unknown. |
| 10 | Quantities like `1/2` scale to `0` (`toDoubleOrNull`). | `RecipeDetailActivity.kt:422`, `IngredientAdapter.kt:43` | Fractions, mixed numbers and ranges are parsed. |
| 11 | The Tasty mapper turns `1½` into `10.5` (the `½` is replaced in place, next to the `1`). | `RecipeMapper.kt:145` | `1½` is read as 1.5 (unit tested). |
| 12 | Diets and cuisines are saved in the **display language**. In Sepedi, Xitsonga or Tshivenda the diet filters (which compare against "Vegan", "Gluten-Free", ...) never match. Discover's sort option is also compared as translated text. | `CuisineSelectionActivity.kt:82`, `DietarySelectionActivity.kt:83`, `ShoppingRepository.kt:1042-1045` | Stored as English IDs; old translated values are mapped back when read. |
| 13 | Discover filters with `whereArrayContainsAny("dietTags", diets)`: recipes matching *any* diet pass, and Halal, Kosher, Nut Allergy, Pescetarian and Low-Carb never appear in tags, so those users see an empty list. | `ShoppingRepository.kt:1037` | Every diet must pass, using flags where they exist and ingredient keywords otherwise. |
| 14 | Recipes users write have no diet flags, so the flag-based recommendation query hides all of them from anyone who set a diet. | `ShoppingRepository.kt:405-411` | User recipes are judged by their ingredients. |
| 15 | Spoonacular search decodes `complexSearch` results as full recipes, but that endpoint returns no ingredients or steps, so parsing fails and Spoonacular search never returns anything. | `ShoppingRepository.kt:595` | Searches, then loads full recipes with `informationBulk`. |
| 16 | 2 of the 8 "Spoonacular" keys are actually the Tasty RapidAPI key, so about 1 in 4 Spoonacular calls is rejected. | `ShoppingRepository.kt:116`, `:272`, `:594` | Separate settings; failed keys fail over to the next. |
| 17 | API refresh joins up to seven tags with commas. Spoonacular treats them as "must have all", so the call usually returns nothing. | `ShoppingRepository.kt:275` | One tag per request. |
| 18 | "My List" uses the fixed ID `MY_LIST_ID` for every account on the device, and its list document is never written to Firestore (only its items), so My List items never sync to a new phone. It also appears twice in the list picker. | `ShoppingListViewModel.kt:16`, `ShoppingRepository.kt:659` | My List is a real document under the user, shown once. |
| 19 | Selecting "All Items" crashes if the user is not loaded yet (`userId.value!!`). | `ShoppingListViewModel.kt:45` | No force-unwraps. |
| 20 | A sign-up profile photo taken with the camera is never uploaded: the `content://` URI's `.path` is treated as a file path. | `ProfileCompletionActivity.kt:235` | The image itself is passed along and uploaded. |
| 21 | The login form asks for "Email / Username", but Firebase only signs in with email, so usernames always fail. | `LoginActivity.kt:168` | Email only, validated. |
| 22 | The OTP and Reset Password screens are placeholders that show "Password reset successfully!" without doing anything. | `ResetPasswordActivity.kt:46`, `OtpActivity.kt` | Removed; Firebase's emailed reset link is the flow. |
| 23 | Google sign-in creates profiles with `selected_country` while the rest of the app reads `country`. | `ShoppingRepository.kt:1085` | Writes `country`; reads both. |
| 24 | The theme setting is saved but never applied on launch, so the app forgets dark mode after a restart. | `PreferencesActivity.kt` (no startup code reads `ThemeMode`) | Applied at the app root. |
| 25 | Edit Profile has validation that is never called, so an empty name can be saved. | `EditProfileActivity.kt:232` | Validated. |
| 26 | The login streak compares day-of-year numbers, so it resets every 1 January. | `ShoppingRepository.kt:317` | Calendar-day difference (unit tested). |
| 27 | Ingredient scanning matched names exactly ("chicken" did not match "chicken breast") and listed recipes the user had none of the ingredients for. ML Kit's coarse classifier added "Food" as an ingredient, and OCR added every word of a receipt, prices included. | `ShoppingRepository.kt` (findRecipesByIngredients), `ScanViewModel.kt` | Word-level matching with plurals; at least one ingredient must match; recognised words must appear in real recipe ingredients. |

## Performance

| # | Problem | Where | iOS |
|---|---------|-------|-----|
| P1 | Every app launch calls `seedFirebaseDatabase()`, which downloads the **entire** recipes collection just to count it, and if there are fewer than 200, makes 630 Spoonacular calls (9 keys × 70 tags) plus 52 Tasty calls from the phone. | `MainViewModel.kt:78`, `ShoppingRepository.kt:96` | No seeding on the client. APIs are called only when a search finds nothing, or once if the database is empty (max. 6 requests). |
| P2 | Each screen creates its own `ShoppingRepository` (19 places), so in-memory caches are never shared: the splash screen's preload is thrown away and switching tabs re-downloads the home screen. | e.g. `SplashActivity.kt:29`, `HomeFragment.kt:34` | One shared repository; one pool of up to 400 recipes is reused by home, search, scan and categories. |
| P3 | Categories are built by downloading every public recipe document. | `ShoppingRepository.kt:869` | Built from the shared pool. |
| P4 | Every recipe fetched from an API is downloaded and re-uploaded to Cloudinary for "upscaling" before it is shown, and `getPublicRecipes` does this twice for the same recipes. | `ShoppingRepository.kt:229`, `:288` | Removed. Spoonacular's largest image size is used directly. |
| P5 | Cuisine and diet icons are full-resolution photos up to 8533×6500 px. The cuisine grid decodes about **1.1 GB** of bitmaps, a likely out-of-memory crash on mid-range phones. | `res/drawable/ic_*.webp` | Shipped as 240×240 icons (Android: 240×240 JPEGs in `drawable-nodpi`, 0.5 MB instead of 41.9 MB). |
| P6 | Offline, Firestore's default read waits for the network before using the cache, and awaited writes never complete, so save spinners hang. | throughout | Cache-only reads and fire-and-forget writes while offline. |
| P7 | Favourites, shopping lists and recipes are copied into Room and re-synced by hand on each launch. | `ShoppingRepository.kt:1184`, `:1206` | Firestore's own offline cache (unlimited size) with live listeners; no second copy. |
| P8 | The splash screen waits a fixed 3 seconds. | `SplashViewModel.kt` | At least 1.2 s, at most 4 s, while it warms the cache. |
| P9 | Recipe photos are uploaded at full camera resolution (3-5 MB). | `ShoppingRepository.kt:752` | Resized to 1600 px and JPEG-compressed (profile photos to 800 px). |

## Not changed in the Android code

These are fixed in the iOS app only; on Android they would need larger changes:

- **P6** Offline saves still wait for the server (Firestore `.await()` on writes).
- **P7** Room still keeps a second copy of favourites and shopping lists.
- **P8** The splash screen timing is unchanged (it now preloads into the shared repository, so the
  preload is no longer wasted).
- **P9** Recipe photos are still uploaded at full resolution.
- **12** New preferences are saved in English. Preferences already saved in another language are not
  converted on Android; re-saving them in Preferences fixes them (the iOS app converts them when read).
- **27** Scan results use word matching and require at least one matching ingredient. Camera labels
  and receipt words are filtered by simple rules, not by the recipe vocabulary the iOS app uses.
- **S1** The secrets are gone from `gradle.properties`, but they remain in the git history, so the
  keystore passwords should still be changed.

The Android changes compile (`:app:compileDebugKotlin`) and the new unit tests in
`app/src/test/.../RecipeFiltersTest.kt` pass. They have not been tried on a device.
