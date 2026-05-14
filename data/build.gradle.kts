plugins {
    id("com.android.library")
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.example.diamonds.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    lint {
        targetSdk = 34
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

// All instrumentation tests live in :app. Disabling androidTest on this library
// module prevents connectedDebugAndroidTest from crashing due to missing test classes.
androidComponents {
    beforeVariants { it.enableAndroidTest = false }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))

    implementation(libs.androidx.core.ktx)

    // Core library desugaring (java.time support on API < 26)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Google Play Services Location
    implementation(libs.play.services.location)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Hilt WorkManager integration
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
