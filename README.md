# Harmony Kitchen 🍳

Welcome to the official repository for Harmony Kitchen, a smart recipe and meal planning application. This project is being developed by **RST Innovations** as a final-year Portfolio of Evidence for Rosebank College.

**Team Members:**
* Tokollo Nonyane (ST10296818)
* Sagwadi Mashimbye (ST10168528)
* Rinae Magadagela (ST10361117)

A special thank you to the **JB Marks Education Trust** and **Rosebank College** for their invaluable support throughout our studies.

---

## 🚀 Live Demo

Check out our live recipe generator app hosted on Hugging Face Spaces!

[![Hugging Face Spaces](https://img.shields.io/badge/%F0%9F%A4%97%20Hugging%20Face-Spaces-blue)](https://huggingface.co/spaces/shirosora1234/RST-Innovations-Recipe-App)

---

## 📸 Screenshots

| Splash & Login | Country & Cuisine | Diet & Profile |
| :---: | :---: | :---: |
| <img width="250" alt="Splash Screen" src="https://github.com/user-attachments/assets/2719c1e9-b5ce-4609-88b9-11f64d67923b" /> | <img width="250" alt="Country Selection" src="https://github.com/user-attachments/assets/4153cf65-de9e-413f-a3dd-3c4d49f5c9c5" /> | <img width="250" alt="Diet Selection" src="https://github.com/user-attachments/assets/2f830263-4620-47c9-897a-5207b3930c8b" /> |
| <img width="250" alt="Login Screen" src="https://github.com/user-attachments/assets/82ad3d6c-a574-4d58-b75f-100cef42afd5" /> | <img width="250" alt="Cuisine Selection" src="https://github.com/user-attachments/assets/e8aa35e8-8073-448f-b84e-dc5de08cc678" /> | <img width="250" alt="Profile Completion" src="https://github.com/user-attachments/assets/0fccb092-e646-4631-bff1-5b7e4c375151" /> |
| <img width="250" alt="Forgot Password" src="https://github.com/user-attachments/assets/3700e6fa-4179-40a5-8824-40516b02423d" /> | <img width="250" alt="Create Account" src="https://github.com/user-attachments/assets/17e6797d-aaa4-43df-a469-8f5209017b72" /> | <img width="250" alt="User Profile" src="https://github.com/user-attachments/assets/6c45eca0-f34f-4542-87d0-66673992b4d1" /> |

| Main App & Navigation | Features |
| :---: | :---: |
| <img width="250" alt="Home Screen" src="https://github.com/user-attachments/assets/4cab09ae-3182-4472-929e-a9e3241993b8" /> | <img width="250" alt="Scan Fragment" src="https://github.com/user-attachments/assets/7034e3cf-f457-4d20-9ccd-50b9a3bc94fd" /> |
| <img width="250" alt="Discover Screen" src="https://github.com/user-attachments/assets/7201f7a1-9045-4f9b-8cad-2bf3051eac90" /> | <img width="250" alt="Shopping List" src="https://github.com/user-attachments/assets/3a53532b-3c09-4e2e-91a8-56db89f2fb54" /> |
| <img width="250" alt="My Recipes" src="https://github.com/user-attachments/assets/8e97f7db-cd2c-4350-ac54-78249f53f5e9" /> | <img width="250" alt="Favorites" src="https://github.com/user-attachments/assets/6ff9450f-6f24-43f4-b3b9-616d2c8e8412" /> |
| <img width="250" alt="Settings" src="https://github.com/user-attachments/assets/d362d7bc-f223-4d5c-9e84-0eeec8c64568" /> | <img width="250" alt="Recipe Details" src="https://github.com/user-attachments/assets/6524a9ae-881f-4b51-bc8e-b6ecbfb387e7" /> |

---

---

## ✨ Features

Harmony Kitchen is packed with features designed to make meal planning and cooking a seamless and personalized experience.

### Core Features
* **User Authentication:** A complete registration and login system using Firebase Authentication. Users can sign up with an email and password, and their profile data is securely stored in Firestore.
* **Dynamic Home Screen:** The home screen provides a personalized experience with:
    * **Featured Recipes:** A top banner showcasing beautiful, high-quality recipes.
    * **Time of Day Recommendations:** A smart section that suggests relevant meals (Breakfast, Lunch, etc.) with friendly, randomized greetings based on the time of day.
    * **Personalized Recommendations:** A main grid of recipes powered by a recommendation engine that considers the user's diet, favorite cuisines, and activity.
* **Recipe Discovery:** A dedicated "Discover" screen with an "endless scroll" feature and a search bar to filter through all public recipes.
* **Add & Manage Recipes:** Users can create, edit, and delete their own recipes through a detailed form. They have the option to keep recipes private or publish them for the community to see.
* **Smart Ingredient Scanner:** Users can find recipes based on ingredients they have at home using three input methods:
    * **Typing:** Manually enter ingredients.
    * **Voice Input:** Use their voice to list ingredients.
    * **Camera Scan:** Use the camera and ML Kit to recognize ingredients from text (e.g., a grocery receipt).
* **Advanced Shopping List:** A multi-list system that includes a manual "My List" and automatically generated lists from recipes. Users can switch between lists, and the system prevents duplicate items.
* **Offline Favorites:** Users can favorite any recipe. The full recipe data is saved to the local Room database, making all favorited recipes available 100% offline.

### Technical Features
* **API Integration:** The app fetches public recipes from the Spoonacular API and uses a "cache-first" strategy by saving them to a Firestore database to minimize API calls and improve performance.
* **Offline-First Architecture:** The app is built using an MVVM (Model-View-ViewModel) architecture with a Repository pattern. It prioritizes loading data from the local RoomDB for a fast, offline-capable experience before syncing with Firebase.
* **User Tracking & Recommendation Engine:** The app silently tracks user activity (viewed recipes, search queries, view duration) and uses this data, along with user preferences (diet, cuisines), to power a scoring system that provides personalized recipe recommendations.

---

Check out our live recipe generator app hosted on Hugging Face Spaces!
[![Hugging Face Spaces](https://img.shields.io/badge/%F0%9F%A4%97%20Hugging%20Face-Spaces-blue)](https://huggingface.co/spaces/shirosora1234/RST-Innovations-Recipe-App)

## 🛠️ How to Install and Test

To get the project running, follow these simple steps:

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/your-username/Recipe-APP-OPSC6312-.git](https://github.com/your-username/Recipe-APP-OPSC6312-.git)
    ```
2.  **Firebase Setup:**
    * Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    * Add an Android app to your Firebase project with the package name `com.rst.recipeappopsc6312`.
    * Download the generated `google-services.json` file and place it in the `app/` directory of the project.
    * In the Firebase Console, enable **Authentication** (with Email/Password provider) and **Firestore Database**.
3.  **API Key Setup:**
    * Get an API key from [Spoonacular](https://spoonacular.com/food-api).
    * In the root directory of the project, create a file named `local.properties`.
    * Add your API key(s) to this file. This file is ignored by Git to keep your keys secure.
    ```properties
    spoonacular.api.key.1="YOUR_API_KEY_HERE"
    # Add other keys if you have them
    ```
4.  **Build and Run:**
    * Open the project in Android Studio.
    * Let Gradle sync the dependencies.
    * Build and run the app on an emulator or a physical device.

---

## 🤖 Automated Testing & CI/CD

This project uses **Unit Testing** to ensure the core logic is reliable and **GitHub Actions** for Continuous Integration.

* **Unit Tests:** Key logic, especially within the `Repository` and `ViewModels`, is covered by unit tests to verify its correctness.
* **GitHub Actions:** A workflow is configured in `.github/workflows/build.yml`. On every push or pull request to the `main` branch, this workflow automatically:
    1.  Checks out the code.
    2.  Sets up the correct Java environment.
    3.  Runs all the unit tests using Gradle.
    4.  Builds a debug version of the APK to ensure the app compiles successfully.

This ensures that the codebase remains healthy and that new changes don't break existing functionality.
