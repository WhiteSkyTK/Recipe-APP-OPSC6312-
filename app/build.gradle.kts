import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.firebase.crashlytics")
}

// Load secrets from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.rst.recipeappopsc6312"
    compileSdk = 36

    // Define the signing configuration for your release builds
    signingConfigs {
        // Only configure the release signing if the properties exist
        if (localProperties.getProperty("RELEASE_STORE_FILE") != null) {
            create("release") {
                storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.rst.recipeappopsc6312"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Read secrets from the loaded local.properties
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"${localProperties.getProperty("spoonacular.api.key")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY_1", "\"${localProperties.getProperty("spoonacular.api.key1")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY_2", "\"${localProperties.getProperty("spoonacular.api.key2")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY_3", "\"${localProperties.getProperty("spoonacular.api.key3")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY_4", "\"${localProperties.getProperty("spoonacular.api.key4")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY_5", "\"${localProperties.getProperty("spoonacular.api.key5")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY_6", "\"${localProperties.getProperty("spoonacular.api.key6")}\"")
        buildConfigField("String", "TASTY_API_KEY", "\"${localProperties.getProperty("tasty.api.key")}\"")
        buildConfigField("String", "TASTY_API_KEY1", "\"${localProperties.getProperty("tasty.api.key1")}\"")
        buildConfigField("String", "CLOUDINARY_URL", "\"${localProperties.getProperty("cloudinary.url")}\"")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Only try to apply the signing config if the property exists.
            if (localProperties.getProperty("RELEASE_STORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.ui.text)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.lottie)
    implementation(libs.circleimageview)

    // Retrofit for networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Glide for loading images from a URL
    implementation(libs.glide)
    implementation(libs.ktor.client.android)

    // Ktor dependencies
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.kotlinx.datetime)

    // Import the Firebase BoM (Bill of Materials)
    implementation(platform(libs.firebase.bom))

    // Add the dependencies for Firebase products you want to use
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.auth)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Optional - Test helpers
    testImplementation(libs.androidx.room.testing)

    // Gson for Type Converters (if not already added)
    implementation(libs.gson)
    implementation(libs.androidx.fragment.ktx)

    // ... other dependencies
    implementation(libs.text.recognition)
    implementation("com.github.amlcurran.showcaseview:library:5.4.3")
    implementation("com.hbb20:ccp:2.7.3")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
    implementation ("androidx.camera:camera-core:1.5.0")
    implementation ("androidx.camera:camera-camera2:1.5.0")
    implementation ("androidx.camera:camera-lifecycle:1.5.0")
    implementation ("androidx.camera:camera-view:1.5.0")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.cloudinary:cloudinary-android:3.1.2")
}