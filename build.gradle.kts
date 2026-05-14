// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    alias(libs.plugins.google.services) apply false
}

// ── Test convenience tasks ────────────────────────────────────────────────────

/**
 * Runs the debug unit-test suite for every module.
 *
 * Usage:
 *   ./gradlew allUnitTests
 *
 * HTML reports land in each module's build/reports/tests/testDebugUnitTest/
 */
tasks.register("allUnitTests") {
    group = "verification"
    description = "Runs all JVM unit tests (debug variant) across every module."

    dependsOn(
        ":app:testDebugUnitTest",
        ":common:testDebugUnitTest",
        ":data:testDebugUnitTest",
        ":ui:testDebugUnitTest"
    )

    doLast {
        println("")
        println("╔══════════════════════════════════════════════════════════╗")
        println("║            All unit tests finished                       ║")
        println("║  Reports: <module>/build/reports/tests/testDebugUnitTest ║")
        println("╚══════════════════════════════════════════════════════════╝")
    }
}

/**
 * Runs all instrumentation (connected/Compose UI) tests on a connected device or emulator.
 *
 * All instrumentation tests live in :app/src/androidTest — the library modules
 * (:data, :ui, :common) contain no androidTest sources so only :app is wired here.
 *
 * Usage (device/emulator must be connected first):
 *   ./gradlew allInstrumentationTests               ← may be UP-TO-DATE if nothing changed
 *   ./gradlew allInstrumentationTests --rerun-tasks  ← always executes on the device
 *
 * HTML report: app/build/reports/androidTests/connected/debug/index.html
 */
tasks.register("allInstrumentationTests") {
    group = "verification"
    description = "Runs all Compose UI / instrumentation tests on a connected device or emulator."

    // Only :app has androidTest sources; running connectedDebugAndroidTest on
    // library modules (:data/:ui/:common) crashes the instrumentation process
    // because they contain no test classes.
    dependsOn(":app:connectedDebugAndroidTest")

    doLast {
        println("")
        println("╔═══════════════════════════════════════════════════════════════════════╗")
        println("║  All instrumentation tests finished                                   ║")
        println("║  Report: app/build/reports/androidTests/connected/debug/index.html    ║")
        println("╚═══════════════════════════════════════════════════════════════════════╝")
    }
}


