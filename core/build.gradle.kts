plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
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

dependencies {
    // Kotlin stdlib only - no Android dependencies in core
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines for Flow support
    implementation(libs.kotlinx.coroutines.core)
}
