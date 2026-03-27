plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
    }
}

dependencies {
    // Kotlin stdlib only - no Android dependencies in core
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
}
