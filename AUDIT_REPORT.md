# Diamonds — Project Audit Report

**Audited**: April 26, 2026  
**Scope**: Full codebase (5 modules, 17,000+ LOC, Phases 1–15)  
**Auditor**: GitHub Copilot AI

---

## Executive Summary

Diamonds is a well-structured home cleaning marketplace Android app with a solid modular
architecture, clean offline-first design, and good separation of concerns. Phases 1–15 are
structurally complete. However, **several production-blocking issues exist** that must be resolved
before switching off `USE_MOCK_BACKEND` and releasing to users. The most critical are: missing
image loading library (Coil), 14 unimplemented Firebase backend methods, plaintext auth token
storage, no Firestore Security Rules, and `isMinifyEnabled = false` in the release build.

**Estimated current test coverage**: well above the earlier ~15–20% estimate — 49 test files (target still 60%; see §7)  
**Phase completion**: 75% (15 of 20 phases)

> **Corrections — 2026-06-25 (independent tech-lead review).** Since this audit: the recurring-booking
> submit/load hang and the silent offline message-loss were fixed (PR #2), and CI (`assembleDebug
> allUnitTests` on every PR) was added (PR #3). Several findings below were also stale/inaccurate and
> are corrected inline: runtime locale switching IS wired (§3), `BookingRepositoryTest` is no longer a
> no-op (§2/§7), real coverage is well above ~15–20%, and `getReviewsForProvider` is filtered (not a
> full scan).

---

## 1. Architecture Assessment ✅ Good

### Strengths

- 5-module dependency graph correctly enforced: `:core` ← `:data` ← `:ui` ← `:app`
- `BaseViewModel` cleanly centralises connectivity, error, and pending-op state
- Room migrations (v1→v10) are complete and correctly written
- `IBackendService` / `BackendServiceStub` / `FirebaseBackendService` swap pattern is well-designed
- Result<T> error handling used consistently
- Offline-first pattern correctly implemented (cache reads, online writes, exponential backoff)

### Issues

| Severity | Issue                                                                                                                       | File                     |
|----------|-----------------------------------------------------------------------------------------------------------------------------|--------------------------|
| 🟡       | `DiamondsApplication.onCreate` manually constructs a second `ConnectivitySyncTrigger`; Hilt-injected instance never started | `DiamondsApplication.kt` |
| 🔵       | `SyncManager` directly instantiates `Json` instead of injecting it — harder to test                                         | `SyncManager.kt`         |

---

## 2. Code Quality — ⚠️ Unimplemented Stubs Shipped

| Severity | File                                | Issue                                                                                                                                                                                           |
|----------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🔴       | `ClientRepository.kt:86–91`         | `getCurrentClient()` returns `Result.Error("Not implemented")`. `observeCurrentClient()` hardcodes `"current_user"` as the DB key — always returns `null` for real users.                       |
| 🔴       | `ProviderRepository.kt:127–130`     | `updateProvider()` returns `Result.Error("Not implemented in backend service")` — provider profile editing broken in production.                                                                |
| 🟡       | `ServiceRepository.kt:150`          | TODO: `updateService` not called on backend.                                                                                                                                                    |
| 🟡       | `PaymentRepository.kt:131`          | TODO: provider payments not fetched from backend.                                                                                                                                               |
| 🔴       | `FirebaseBackendService.kt:485–524` | **14 methods** return `Result.Error("not yet implemented")`: support tickets, claims, cancel-with-reason, edit booking, refunds, help articles, saved locations. Silent failures in production. |
| ✅       | `BookingRepositoryTest.kt`          | RESOLVED — no longer a no-op; now carries real MockK/assertion tests (offline / error / cache paths).                                                                                                                                |
| 🟡       | `FirebaseBackendService.kt:79`      | `searchProviders` performs a full Firestore collection scan with no geo-filtering. At scale, reads entire `providers` collection on every search.                                               |
| 🟡       | `getConversationsForUser`           | Issues 2 separate Firestore queries merged in memory — doubles read cost; can miss ordering.                                                                                                    |

---

## 3. Missing Implementations

| Feature                         | Status     | Notes                                                                                                                                         |
|---------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| **Image loading library**       | ❌ Missing  | No Coil/Glide/Picasso. `profileImageUrl`, `imageUrls` stored throughout models but images cannot be displayed. Add `io.coil-kt:coil-compose`. |
| **Runtime locale switching**    | ⚠️ Partial | Wiring IS done — `LanguageSelectorScreen` calls `AppCompatDelegate.setApplicationLocales()`. Remaining gap: ~350 hardcoded UI strings, so switching has little visible effect. |
| **ReviewDirection in Firebase** | ⚠️ Partial | `ReviewDirection` enum and Room column correct; `FirebaseBackendService.createReview` never writes `direction` field to Firestore.            |
| **Firestore Security Rules**    | ❌ Missing  | No rules file in repo. Any authenticated user can read/write any document.                                                                    |
| **Real payment processing**     | ❌ Missing  | No Stripe/Braintree SDK. Payments immediately set to `SUCCEEDED` with no real processing.                                                     |
| **CI/CD**                       | ✅ Added    | `.github/workflows/ci.yml` runs `assembleDebug allUnitTests` on PRs/pushes to `develop` (PR #3).                                              |
| **Proguard rules**              | ❌ Missing  | `proguard-rules.pro` is the default empty template.                                                                                           |

---

## 4. Security Concerns

| Priority | Issue                                                                                                                          | Location                  |
|----------|--------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| 🔴 P1    | `applicationId = "com.example.diamonds"` — example namespace                                                                   | `app/build.gradle.kts:17` |
| 🔴 P1    | `isMinifyEnabled = false` in release build — no obfuscation or shrinking                                                       | `app/build.gradle.kts:38` |
| 🔴 P1    | Auth token stored as **plaintext** in DataStore — `EncryptedSharedPreferences` or `EncryptedDataStore` not used                | `PreferencesDataStore.kt` |
| 🔴 P1    | No Firestore Security Rules — any authenticated user can read/write all documents                                              | Firebase Console          |
| 🟡 P2    | `ACCESS_BACKGROUND_LOCATION` declared but likely unnecessary if tracking only occurs while booking screen is open              | `AndroidManifest.xml:9`   |
| 🟡 P2    | `android:allowBackup="true"` — DataStore (incl. auth token) extractable via ADB on non-rooted debug devices                    | `AndroidManifest.xml:14`  |
| 🟡 P2    | Deep link `autoVerify="true"` with no Digital Asset Links file — verification fails; any app can intercept `diamonds://` links | `AndroidManifest.xml:44`  |
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

### Current State

- **`:data/test`**: `BookingRepositoryFullTest` (10 tests) and `BookingRepositoryTest` (6 tests) are
  both solid now — the latter is no longer a no-op `assert(true)`. PR #2 added regression tests for
  `RecurringBookingViewModel` and `MessageRepository`, which now execute in CI.
- **Missing unit tests**: `SyncManager`, both Workers (`SyncWorker`, `RecurringBookingWorker`),
  `ConnectivityObserver`, `PreferencesDataStore`, and the `Provider`/`Service`/`Notification`/
  `Location`/`SavedLocation`/`Client` repositories. (`AuthRepository`, `MessageRepository`,
  `SubscriptionRepository`, `SupportRepository` and the mappers now have tests.)
- **`:ui/test`**: 13 ViewModel test files. Missing: `MapViewModel`, `NotificationViewModel`,
  `SplashViewModel` (and several support/profile VMs). `ChatViewModel`/`SyncStatusViewModel` now have tests.
- **`:app/androidTest`**: 14 Compose instrumented tests (good). `ExampleInstrumentedTest.kt`
  boilerplate never removed.
- `ExampleUnitTest` in multiple modules is also unreplaced boilerplate

### Priority Test Targets (for Phase 18)

1. `SyncManager` — dispatch logic and conflict resolution
2. `ClientRepository.getCurrentClient()` — once fixed
3. `PreferencesDataStore` — session persistence
4. All unimplemented repository methods once implemented
5. `ChatViewModel` and `SyncStatusViewModel`
6. Remove all no-op and boilerplate test files

---

## 8. Build Configuration Issues

| Issue                                                                          | Severity | Location                  |
|--------------------------------------------------------------------------------|----------|---------------------------|
| `isMinifyEnabled = false` in release                                           | 🔴 P1    | `app/build.gradle.kts:38` |
| `kotlinCompilerExtensionVersion = "1.5.0"` should be `"1.5.8"` for Compose 1.6 | 🔵 P3    | `app/build.gradle.kts`    |
| `org.gradle.parallel=true` commented out                                       | 🔵 P3    | `gradle.properties`       |
| No `signingConfig` for release build                                           | 🟡 P2    | `app/build.gradle.kts`    |
| `exportSchema = true` on `AppDatabase` but no `schemaLocation` set             | 🔵 P3    | `data/build.gradle.kts`   |

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

Nine well-structured, consistent documentation files. The `ARCHITECTURE.md` is particularly
detailed. Documentation quality is a clear strength of this project.

**One inaccuracy fixed by this audit**: `README.md` listed Phase 11 (in-app chat) as a "Next
Step" when it was already complete.

---

## 10. Prioritised Action Plan

### Immediate (before any Firebase testing)

1. `implementation("io.coil-kt:coil-compose:3.1.0")` — add to `ui/build.gradle.kts`
2. Change `applicationId` in `app/build.gradle.kts`
3. Implement `getCurrentClient()` in `ClientRepository` — inject `PreferencesDataStore` for real
   user ID
4. Wire `AppCompatDelegate.setApplicationLocales()` in `MainActivity`
5. Fix `ReviewDirection` in `FirebaseBackendService.createReview`
6. Fix `DiamondsApplication` DI — use `@Inject lateinit var syncTrigger: ConnectivitySyncTrigger`
   and remove manual construction

### Before Firebase Go-Live

7. Implement the 14 unimplemented `FirebaseBackendService` methods
8. Implement `updateProvider()` in `ProviderRepository`
9. Encrypt DataStore session (`EncryptedSharedPreferences` or `security-crypto-ktx`)
10. Write and deploy Firestore Security Rules
11. Enable `isMinifyEnabled = true` and write `proguard-rules.pro`
12. Fix `ConnectivityObserver.isOnline()` — add `NET_CAPABILITY_VALIDATED`

### Before Release (Phase 19)

13. Change `android:allowBackup="false"` or configure `data_extraction_rules.xml`
14. Add Digital Asset Links file for `diamonds://` deep links
15. Add `signingConfig` release block
16. Add GeoHash-based provider search (replace full collection scan)
17. Add Stripe/Braintree SDK for real payment processing
18. Enable `org.gradle.parallel=true`
19. Upgrade Kotlin to 2.1.x and Compose to 1.7.x

### Phase 18 (Testing)

20. Expand test coverage to 60%+ (
    priority: `SyncManager`, `ClientRepository`, `PreferencesDataStore`, support/claim repos)
21. Remove boilerplate `ExampleUnitTest`/`ExampleInstrumentedTest` (the no-op `assert(true)` test is already resolved)

---

*Report generated by GitHub Copilot AI — April 26, 2026*
