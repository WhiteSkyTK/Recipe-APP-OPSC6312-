plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

// Add this entire block
dependencies {
    // Firebase Admin SDK for the script
    implementation("com.google.firebase:firebase-admin:9.3.0")
    // Coroutines library for the script
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
