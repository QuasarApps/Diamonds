# Diamonds — Project Audit Report

**Audited**: April 26, 2026  
**Re-verified**: July 27, 2026 against `develop` @ `4892952`  
**Scope**: Full codebase — 5 modules, **124 production `.kt` files / 25,598 LOC**, 25 ViewModels,
Room DB v10 (9 migrations), Phases 1–16  
**Auditor**: GitHub Copilot AI

---

## Executive Summary

Diamonds is a well-structured home cleaning marketplace Android app with a solid modular
architecture, clean offline-first design, and good separation of concerns. Phases 1–16 are
structurally complete. However, **several production-blocking issues exist** that must be resolved
before switching off `USE_MOCK_BACKEND` and releasing to users. The most critical remaining are:
missing image loading library (Coil), 14 unimplemented Firebase backend methods, plaintext auth
token storage, no Firestore Security Rules, and `isMinifyEnabled = false` in the release build.
(The former headline blocker — every Firestore read failing for want of a DTO no-arg constructor —
is fixed; see §3.)

**Current test footprint**: 26 JVM unit-test files / 300 `@Test` methods, plus 23 instrumentation
files (target still 60% coverage; see §7)  
**Phase completion**: 16 of 21 phases in `ROADMAP.md`

> **Corrections — 2026-06-25 (independent tech-lead review).** Since this audit: the recurring-booking
> submit/load hang and the silent offline message-loss were fixed (PR #2), and CI (`assembleDebug
> allUnitTests` on every PR) was added (PR #3). Several findings below were also stale/inaccurate and
> are corrected inline: runtime locale switching IS wired (§3), `BookingRepositoryTest` is no longer a
> no-op (§2/§7), real coverage is well above ~15–20%, and `getReviewsForProvider` is filtered (not a
> full scan).
>
> **Re-verification — 2026-07-27 (`develop` @ `4892952`).** Track B (string externalisation, PRs
> #7–#17) is complete, so the "~350 hardcoded UI strings" finding in §3 is **closed**. Track D is
> partial: `SyncManager` and `LocationRepository` now have tests (PRs #19–#21), so the §7 priority
> list has been re-ranked. Track C (Firebase/release readiness) remains **entirely untouched**.
> Six findings new to this revision are marked **NEW 2026-07-27** below; the most serious is the
> Firestore DTO deserialisation blocker in §3. Scale metrics above were recounted from source.

---

## 1. Architecture Assessment ✅ Good

### Strengths

- 5-module dependency graph is **acyclic and matches the docs** (re-verified 2026-07-27):
  `:common` → `:core`; `:data` → `:core`, `:common`; `:ui` → `:core`, `:data`, `:common`;
  `:app` → all. `:core` is a pure `java-library` with **zero** Android imports.
- `BaseViewModel` cleanly centralises connectivity, error, and pending-op state
- Room migrations are complete and consistently written (v1→v10, `MIGRATION_1_2` … `MIGRATION_9_10`)
  — though none of them is *verified* by a schema test; see §7
- `IBackendService` / `BackendServiceStub` / `FirebaseBackendService` swap pattern is well-designed
- Read-side offline-first is genuinely implemented (cache reads, online writes, exponential backoff)
- **Zero** `GlobalScope` and **zero** `runBlocking` in any `main` source set
- **Zero** `Log.*` / `println` calls anywhere; no secrets are committed; `PendingIntent` uses
  `FLAG_IMMUTABLE`; cleartext traffic is blocked by the `targetSdk 34` default

### Issues

| Severity | Issue                                                                                                                       | File                     |
|----------|-----------------------------------------------------------------------------------------------------------------------------|--------------------------|
| 🟡       | `DiamondsApplication.onCreate` manually constructs a second `ConnectivitySyncTrigger`; Hilt-injected instance never started | `DiamondsApplication.kt` |
| 🟡       | **NEW 2026-07-27** — `Result<T>`'s documented three-arm convention is bypassed in the UI layer: **37** `as? Result.Success` sites across **10** ViewModel files collapse `Result.Error` into `null`, so the failure never reaches the user | `ui/…/*ViewModel.kt`     |
| 🔵       | `SyncManager` directly instantiates `Json` instead of injecting it — harder to test                                         | `SyncManager.kt`         |
| 🔵       | **NEW 2026-07-27** — no `DispatcherProvider`; dispatchers are referenced directly, which limits deterministic test control  | `:data`, `:ui`           |

---

## 2. Code Quality — ⚠️ Unimplemented Stubs Shipped

| Severity | File                                | Issue                                                                                                                                                                                           |
|----------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🔴       | `ClientRepository.kt:87`         | `getCurrentClient()` returns `Result.Error(Exception("Not implemented"))`. `observeCurrentClient()` hardcodes `"current_user"` as the DB key — always returns `null` for real users.                       |
| ✅       | `ProviderRepository.kt:120`     | RESOLVED — `updateProvider()` now calls `IBackendService.updateProvider` (new) via a new `Provider.toDto()` mapper and caches the response. `CleanerProfileViewModel` profile saves work again.            |
| 🟡       | `ServiceRepository.kt:151`          | TODO: `updateService` not called on backend.                                                                                                                                                    |
| 🟡       | `PaymentRepository.kt:119,132`          | TODO: provider payments not fetched from backend.                                                                                                                                               |
| 🔴       | `FirebaseBackendService.kt:484–524` | **14 methods** return `Result.Error(Exception("not yet implemented"))`: support tickets, claims, cancel-with-reason, edit booking, refunds, help articles, saved locations. Silent failures in production. |
| ✅       | `BookingRepositoryTest.kt`          | RESOLVED — no longer a no-op; now carries real MockK/assertion tests (offline / error / cache paths).                                                                                                                                |
| 🟡       | `FirebaseBackendService.kt:79`      | `searchProviders` performs a full Firestore collection scan and **ignores `latitude`, `longitude` and `radius` entirely** — the geo arguments are accepted and dropped.                          |
| 🟡       | `getConversationsForUser`           | Issues 2 separate Firestore queries merged in memory — doubles read cost; can miss ordering.                                                                                                    |
| 🔴       | `SavedLocationRepository.kt:32,45`  | **NEW 2026-07-27** — `:32` maps `Result.Error → Result.Success(emptyList())` and `:45` maps `Result.Error → Result.Success(location)`. A failed remote write is reported to the UI as success — the same silent-data-loss class as the `MessageRepository` bug fixed in PR #2. Currently untested. |
| ✅       | `FirebaseAuthService.signup`        | RESOLVED — `AuthRepository.signup` now writes the `ClientDto`/`ProviderDto` profile document (keyed on the auth uid, carrying `phoneNumber` and the role-derived collection) before saving the session, on **both** backends. The two writes are still not atomic: a profile-write failure leaves an orphaned auth account and is reported as an error rather than swallowed. |
| 🟡       | `FirebaseBackendService.createBooking` | **NEW 2026-07-27** — hardcodes `totalPrice = 0.0` and `estimatedDuration = 60` rather than using the booking's own values.                                                                    |
| ✅       | `observeFcmToken`                   | RESOLVED — `IBackendService.registerFcmToken`/`unregisterFcmToken` added and implemented on both backends. `DiamondsFcmService.onNewToken` registers when a session exists; `AuthRepository` registers on login/signup (tokens often arrive *before* login) and unregisters on logout. **Still open:** `POST_NOTIFICATIONS` is declared but never requested, so on API 33+ notifications are silently suppressed regardless — see §9.5.8. |

---

## 3. Missing Implementations

| Feature                         | Status     | Notes                                                                                                                                         |
|---------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| **Image loading library**       | ❌ Missing  | No Coil/Glide/Picasso. `profileImageUrl`, `imageUrls` stored throughout models but images cannot be displayed. Add `io.coil-kt:coil-compose`. |
| **Runtime locale switching**    | ✅ Done     | **RESOLVED (Track B, PRs #7–#17).** Wiring was already there (`LanguageSelectorScreen` → `AppCompatDelegate.setApplicationLocales()`); the strings are now externalised. `stringResource` is used in **46 of 83** `:ui` files across **608** call sites; `values/strings.xml` holds **531 `<string>` + 16 `<plurals>`**; `values-fr`, `values-es`, `values-pt`, `values-ar` are all key-complete; every `Icon` `contentDescription` is localized (`cd_*`). The old "~350 hardcoded UI strings / `stringResource` in 1 of 83 files" finding is **retracted**. Residual: ~**67** `Text("…")` literals, almost all non-translatable glyphs (emoji, `"$"`, `"›"`). |
| **Lint guard for hardcoded strings** | ❌ Missing | **NEW 2026-07-27** — no lint rule and no lint baseline exists anywhere in the repo, so nothing prevents new hardcoded literals from regressing Track B. |
| **Firestore DTO deserialisation** | ✅ **Working** | Was the biggest single blocker: DTO constructor params were all required, so Kotlin generated no no-arg constructor and Firestore's object mapper could not build one. All **26** `toObject()`/`toObjects()` calls threw into a swallowed `Result.Error` while writes succeeded — a **write-only app**. Every DTO param now has a default, and `FirestoreDtoContractTest` fails the build if one is removed. Because defaults also mean a malformed document deserializes silently into blanks rather than failing, the 13 `*Dto.toDomain()` mappers validate identity fields and enums and raise `MalformedDtoException` naming the DTO, field, received value and valid set. |
| **ReviewDirection in Firebase** | ⚠️ Partial | `ReviewDirection` enum and Room column correct; `FirebaseBackendService.createReview` still drops both `direction` **and** `locationTags`.    |
| **Firestore Security Rules**    | ❌ Missing  | **Zero** rules in the repo — no `firestore.rules`, no `firebase.json`, no `.firebaserc`. Any authenticated user can read/write any document.  |
| **Firebase project config**     | ❌ Placeholder | `app/google-services.json` is a placeholder (`project_number` `"000000000000"`, `api_key` `"placeholder-key-for-testing"`) — a real project must be provisioned before any Firebase testing. |
| **Real payment processing**     | ❌ Missing  | No Stripe/Braintree SDK. `status = "SUCCEEDED"` is fabricated client-side (`FirebaseBackendService.kt:267`).                                  |
| **CI/CD**                       | ⚠️ Partial | `.github/workflows/ci.yml` runs `assembleDebug allUnitTests` on PRs/pushes to `develop` under JDK 17 (PR #3). **NEW 2026-07-27:** it never compiles or runs `androidTest` (no `assembleDebugAndroidTest`), runs no lint, and has no coverage gate. `allUnitTests` also aggregates `:app` and `:common`, neither of which has a `src/test` dir, and excludes `:core` entirely. |
| **Release signing**             | ❌ Missing  | No `signingConfig` anywhere, and `applicationId` is still `com.example.diamonds` — Play rejects `com.example.*`.                              |
| **Proguard rules**              | ❌ Missing  | `proguard-rules.pro` is the default empty template and `isMinifyEnabled = false`.                                                             |

---

## 4. Security Concerns

| Priority | Issue                                                                                                                          | Location                  |
|----------|--------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| 🔴 P1    | `applicationId = "com.example.diamonds"` — example namespace                                                                   | `app/build.gradle.kts:17` |
| 🔴 P1    | `isMinifyEnabled = false` in release build — no obfuscation or shrinking                                                       | `app/build.gradle.kts:38` |
| 🔴 P1    | Auth token **and PII** (userId/email/role) stored as **plaintext** in an unencrypted DataStore — `EncryptedSharedPreferences` / `security-crypto` not used | `PreferencesDataStore.kt:16` |
| 🔴 P1    | No Firestore Security Rules — **zero** rules files in the repo (no `firestore.rules`/`firebase.json`/`.firebaserc`); any authenticated user can read/write all documents | repo root                 |
| 🔴 P1    | **NEW 2026-07-27** — `PaymentScreen` collects the full card PAN and CVV in unmasked `OutlinedTextField`s, with no PSP SDK and no `FLAG_SECURE` on the window (screenshots/recording are permitted) | `PaymentScreen.kt`        |
| 🟡 P2    | `ACCESS_BACKGROUND_LOCATION` and `POST_NOTIFICATIONS` are declared in the manifest but **never requested or used at runtime** — no runtime-permission call site exists for either | `AndroidManifest.xml:9,11` |
| 🟡 P2    | `android:allowBackup="true"` with empty backup-rule files — DataStore (incl. the plaintext auth token) extractable via ADB on non-rooted debug devices | `AndroidManifest.xml:15`  |
| 🟡 P2    | Deep link `autoVerify="true"` with no Digital Asset Links file — verification fails; any app can intercept `diamonds://` links | `AndroidManifest.xml:43`  |
| 🔵 P3    | `MAPS_API_KEY` is empty string committed in `gradle.properties`                                                                | `gradle.properties:25`    |

### Recommended Firestore Security Rules (starting point)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /clients/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    match /providers/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    match /bookings/{bookingId} {
      allow read, write: if request.auth != null &&
        (resource.data.clientId == request.auth.uid ||
         resource.data.providerId == request.auth.uid);
    }
    // ... etc.
  }
}
```

---

## 5. Performance Concerns

| Issue                                                                                                  | Impact                                                  | Fix                                             |
|--------------------------------------------------------------------------------------------------------|---------------------------------------------------------|-------------------------------------------------|
| `ConnectivityObserver.isOnline()` only checks `NET_CAPABILITY_INTERNET` not `NET_CAPABILITY_VALIDATED` | Captive portals (hotel WiFi) appear online; writes fail | Add `NET_CAPABILITY_VALIDATED` check            |
| `getClientBookings` always fetches from backend when online, no cache staleness check                  | Unnecessary Firestore reads                             | Implement cache-first with TTL                  |
| `searchProviders` full Firestore collection scan                                                       | Expensive at scale                                      | Add GeoHash field + `geoquery` filter           |
| `getConversationsForUser` issues 2 Firestore queries merged in memory                                  | Double read cost + ordering issues                      | Use a single query with composite index         |
| `BackendServiceStub` `delay(400)` on every call                                                        | Compounds on screens that fan out parallel calls        | Acceptable for dev stub; document the behaviour |

---

## 6. Dependency Management

| Library                 | In Project      | Latest (Apr 2026) | Action                 |
|-------------------------|-----------------|-------------------|------------------------|
| Kotlin                  | 1.9.0           | 2.1.x             | 🟡 Upgrade             |
| AGP                     | 8.5.0           | 8.7.x             | 🔵 Upgrade             |
| `core-ktx`              | 1.10.1          | 1.15.x            | 🔵 Upgrade             |
| Compose UI              | 1.6.0           | 1.7.x             | 🟡 Upgrade             |
| Material 3              | 1.2.0           | 1.3.x             | 🔵 Upgrade             |
| Hilt                    | 2.50            | 2.51+             | 🔵 Upgrade             |
| DataStore               | 1.0.0           | 1.1.x             | 🔵 Upgrade             |
| `kotlinx-serialization` | 1.6.0           | 1.7.x             | 🔵 Upgrade             |
| Lifecycle               | 2.7.0           | 2.8.x             | 🔵 Upgrade             |
| **Coil**                | **not present** | 3.x               | 🔴 **Add immediately** |

**CVE Status**: No known critical CVEs in the current dependency set as of April 26, 2026.

> Note: `kotlinCompilerExtensionVersion = "1.5.0"` should be `"1.5.8"` to align with
> `compose = "1.6.0"`. They are currently compatible but should be kept in sync.

---

## 7. Test Coverage — well above the earlier ~15–20% estimate (Target: 60%)

### Current State (recounted 2026-07-27)

**26 JVM unit-test files / 300 `@Test` methods** under `*/src/test`, plus **23 files** under
`app/src/androidTest`.

- **`:data/test`** (12 files): `BookingRepositoryFullTest` (10 tests) and `BookingRepositoryTest`
  (6 tests) are both solid — the latter is no longer a no-op `assert(true)`. PR #2 added regression
  tests for `RecurringBookingViewModel` and `MessageRepository`, which now execute in CI. Track D
  added `SyncManagerTest` (21 tests) and `LocationRepositoryTest` (8 tests) alongside
  `SyncManager` correctness fixes (PRs #19–#21).
- **`:ui/test`** (13 ViewModel test files + `MainDispatcherRule`). Missing: `MapViewModel`,
  `NotificationViewModel`, `SplashViewModel` and several support/profile VMs.
  `ChatViewModel`/`SyncStatusViewModel` now have tests.
- **`:app/androidTest`**: 23 files of Compose instrumented tests. Boilerplate `Example*Test` files
  have been removed from both `:app/test` and `:app/androidTest`.
- **Still untested**: `SyncWorker`, `RecurringBookingWorker`, `PreferencesDataStore`,
  `ConnectivityObserver`, and 5 of the 13 repositories — `Client`, `Notification`, `Provider`,
  `SavedLocation`, `Service`. (`Auth`, `Booking`, `Location`, `Message`, `Payment`, `Review`,
  `Subscription`, `Support` and the mappers are covered.)

### Test-infrastructure gaps — **NEW 2026-07-27**

| Gap                                                                                                                                                                                          | Impact                                                        |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| CI never compiles or runs `androidTest` (no `assembleDebugAndroidTest`), runs no lint, and has no coverage gate                                                                              | The two breakages below shipped invisibly                     |
| `LoginScreenTest.kt:153` asserts `"Demo accounts  (tap to fill)"` (two spaces); the shipped `R.string.demo_accounts_hint` is `"Demo accounts (tap to fill)"` (one space)                     | That instrumentation test **fails** — introduced by Track B    |
| `FakeRepositoryModule` `@TestInstallIn`-replaces `RepositoryModule` wholesale with 13 `@Provides` but omits `ISavedLocationRepository`, which production `Modules.kt` provides                | The `androidTest` Hilt graph is incomplete                     |
| `exportSchema = true` on `AppDatabase` but no `room.schemaLocation` is configured and no `schemas/` directory exists                                                                          | All **9** migrations are unverified; the declared `room-testing` dependency is dead code |
| Turbine is declared in both `data/build.gradle.kts` and `ui/build.gradle.kts` but has **0** uses                                                                                              | Flow assertions are hand-rolled                                |
| `collectAsStateWithLifecycle` has **0** uses against **87** `collectAsState()` sites across 44 files                                                                                          | Collectors keep running while the screen is in the background   |

### Priority Test Targets (re-ranked 2026-07-27)

`SyncManager` was #1 in the April list and is now tested, so the ranking below reflects what is
actually most valuable next.

1. **Room migration tests** — configure `room.schemaLocation`, commit the exported schemas, then
   assert all 9 migrations with `MigrationTestHelper`. Highest risk: a bad migration is unrecoverable
   user data loss, and nothing verifies them today.
2. **`SyncWorker` and `RecurringBookingWorker`** — `WorkManager`'s `TestListenableWorkerBuilder`;
   these are the only untested code that runs unattended in the background.
3. **`PreferencesDataStore`** — session persistence and the `null`-first-emission contract that the
   rest of the app depends on.
4. **`ConnectivityObserver`** — every write path gates on it, and its captive-portal defect (§5)
   needs a regression test alongside the fix.
5. **The 5 untested repositories** — `SavedLocation` first (its error→success mapping in §2 is an
   active silent-data-loss bug), then `Client`, `Provider`, `Service`, `Notification`.
6. **Fix and green the `androidTest` suite**, then add `assembleDebugAndroidTest` to CI so it cannot
   silently rot again.
7. Adopt Turbine (already on the classpath) and a `DispatcherProvider` so Flow/coroutine assertions
   are deterministic rather than hand-rolled.

---

## 8. Build Configuration Issues

| Issue                                                                          | Severity | Location                  |
|--------------------------------------------------------------------------------|----------|---------------------------|
| `isMinifyEnabled = false` in release                                           | 🔴 P1    | `app/build.gradle.kts:38` |
| `kotlinCompilerExtensionVersion = "1.5.0"` should be `"1.5.8"` for Compose 1.6 | 🔵 P3    | `app/build.gradle.kts`    |
| `org.gradle.parallel=true` commented out                                       | 🔵 P3    | `gradle.properties`       |
| No `signingConfig` for release build                                           | 🟡 P2    | `app/build.gradle.kts`    |
| **Upgraded 2026-07-27** — `exportSchema = true` on `AppDatabase` but no `room.schemaLocation` is configured and no `schemas/` directory exists, so all **9** migrations are unverified and the declared `room-testing` dependency is dead | 🟡 P2 | `data/build.gradle.kts`   |
| **NEW 2026-07-27** — CI does not build or run `androidTest`, run lint, or enforce a coverage gate; `allUnitTests` aggregates `:app`/`:common` (no `src/test` dirs) and skips `:core` | 🟡 P2 | `.github/workflows/ci.yml` |

### Recommended `proguard-rules.pro` starters

```
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin Serialization (Firestore DTOs)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep @kotlinx.serialization.Serializable class * { *; }
```

---

## 9. Documentation Quality ✅ Excellent

Well-structured, consistent documentation. The `ARCHITECTURE.md` is particularly detailed.
Documentation quality is a clear strength of this project.

**One inaccuracy fixed by this audit**: `README.md` listed Phase 11 (in-app chat) as a "Next
Step" when it was already complete.

**Consolidation — 2026-07-27**: the root-level Markdown set was pruned of stale status/fix-log
files, and the committed Gradle build logs were removed. Remaining drift corrected in the same
pass: the "encrypted session storage" claim (it is **plaintext**), a backwards module-dependency
direction in `QUICK_REFERENCE.md` (`:common` depends on `:core`, never the reverse), references to
a non-existent `ui/screens/` directory (`:ui` is organised by feature package), the retracted
"~15–20% coverage" / "`BookingRepositoryTest` is a no-op" / "`getReviewsForProvider` is a full
scan" claims, inconsistent phase counts (`ROADMAP.md` is the authority: **21 phases**), and
`ROADMAP.md` showing Phase 16 as unstarted when it is shipped (DB v10 + `MIGRATION_9_10`,
`CleaningType`/`LocationType`, both selectors, `SavedLocationsScreen`, specialization badges and
filter all exist).

---

## 10. Prioritised Action Plan

### Immediate (before any Firebase testing)

1. **Give every `IBackendService` DTO defaulted constructor params** so Firestore can deserialise
   them — until this is done every Firestore read fails and the Firebase path is write-only
   (**NEW 2026-07-27**, highest priority in this document)
2. **Fix `SavedLocationRepository`** — stop mapping `Result.Error → Result.Success`; propagate the
   error and add a regression test (**NEW 2026-07-27**)
3. `implementation("io.coil-kt:coil-compose:3.1.0")` — add to `ui/build.gradle.kts`
4. Change `applicationId` in `app/build.gradle.kts`
5. Implement `getCurrentClient()` in `ClientRepository` — inject `PreferencesDataStore` for real
   user ID
6. ~~Wire `AppCompatDelegate.setApplicationLocales()` in `MainActivity`~~ — **done**; wiring lives in
   `LanguageSelectorScreen` and the strings are externalised (Track B, PRs #7–#17)
7. Fix `ReviewDirection` **and `locationTags`** in `FirebaseBackendService.createReview`
8. Fix `DiamondsApplication` DI — use `@Inject lateinit var syncTrigger: ConnectivitySyncTrigger`
   and remove manual construction
9. Fix `LoginScreenTest`'s two-space assertion and add `ISavedLocationRepository` to
   `FakeRepositoryModule` — the `androidTest` suite does not currently pass (**NEW 2026-07-27**)

### Before Firebase Go-Live

10. Provision a real Firebase project and replace the placeholder `app/google-services.json`
11. Implement the 14 unimplemented `FirebaseBackendService` methods (`:485–524`)
12. ✅ DONE — `updateProvider()` implemented in `ProviderRepository`
13. ✅ DONE — signup now creates the profile document and honours the `role` (in `AuthRepository`,
    so it applies to both the stub and Firebase backends). Follow-up: make the auth-account and
    profile-document writes atomic — that needs a server-side callable function, so it cannot be
    fixed client-side
14. Fix `FirebaseBackendService.createBooking` — stop hardcoding `totalPrice = 0.0` /
    `estimatedDuration = 60` (**NEW 2026-07-27**)
15. ✅ DONE — the FCM token is registered server-side on login/signup and on token refresh, and
    unregistered on logout. Remaining prerequisite for push actually arriving: request
    `POST_NOTIFICATIONS` at runtime (declared but never requested — see §9.5.8), and write a
    Firestore rule for the new `fcmTokens` collection
16. Encrypt DataStore session (`EncryptedSharedPreferences` or `security-crypto-ktx`) — the token
    **and** the userId/email/role are plaintext today
17. Write and deploy Firestore Security Rules — none exist in the repo at all
18. Enable `isMinifyEnabled = true` and write `proguard-rules.pro`
19. Fix `ConnectivityObserver.isOnline()` — add `NET_CAPABILITY_VALIDATED`
20. Decide the sync-queue question left open by Track A: **wire `SyncManager` into the write repos
    or delete it**. It is built, tested and backs the Sync Status screen, but no repo enqueues.

### Before Release (Phase 19)

21. Change `android:allowBackup="false"` or configure `data_extraction_rules.xml`
22. Add Digital Asset Links file for `diamonds://` deep links
23. Add `signingConfig` release block
24. Add GeoHash-based provider search — `searchProviders` currently ignores `latitude`/`longitude`/
    `radius` outright
25. Add Stripe/Braintree SDK for real payment processing, mask the PAN/CVV fields and set
    `FLAG_SECURE` on `PaymentScreen`
26. Remove `ACCESS_BACKGROUND_LOCATION` and `POST_NOTIFICATIONS`, or implement the runtime
    permission requests they need
27. Enable `org.gradle.parallel=true`
28. Upgrade Kotlin to 2.1.x and Compose to 1.7.x

### Phase 18 (Testing)

29. Work the re-ranked §7 priority list — Room migration tests, both Workers,
    `PreferencesDataStore`, `ConnectivityObserver`, then the 5 untested repositories
30. Add `assembleDebugAndroidTest`, lint and a coverage gate to CI
31. Add a lint rule/baseline for hardcoded UI strings so Track B cannot regress
32. Migrate the 87 `collectAsState()` sites to `collectAsStateWithLifecycle`
33. ~~Remove boilerplate `ExampleUnitTest`/`ExampleInstrumentedTest`~~ — **done**; both template files have been deleted.

---

*Report generated by GitHub Copilot AI — April 26, 2026*
