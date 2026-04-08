plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    // Apply only when google-services.json exists; comment out if not using Firebase
    // alias(libs.plugins.google.services)
}

// Read MAPS_API_KEY from gradle.properties or local.properties
val mapsApiKey: String = project.findProperty("MAPS_API_KEY")?.toString() ?: ""

android {
    namespace = "com.example.diamonds"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.diamonds"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        debug {
            // Flip to false (and add google-services.json) to use FirebaseAuthService
            buildConfigField("boolean", "USE_MOCK_AUTH", "true")
            // Flip to false to use FirebaseBackendService instead of BackendServiceStub
            buildConfigField("boolean", "USE_MOCK_BACKEND", "true")
        }
        release {
            buildConfigField("boolean", "USE_MOCK_AUTH", "false")
            buildConfigField("boolean", "USE_MOCK_BACKEND", "false")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":core"))
    implementation(project(":common"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation (needed transitively for NavHostController resolution)
    implementation(libs.androidx.navigation.compose)

    // Room (needed for Dagger-generated code that references RoomDatabase)
    implementation(libs.androidx.room.runtime)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Google Play Services Location
    implementation(libs.play.services.location)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}