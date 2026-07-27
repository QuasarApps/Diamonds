# Diamonds

**Diamonds** is an Android application for peer-to-peer cleaning services, built with modern Android architecture patterns, offline-first design, and reactive state management.

## Architecture Overview

Diamonds implements a **modular, offline-first architecture** with the following principles:

- **Modular Design**: Separate Gradle modules (`:core`, `:data`, `:ui`, `:common`) for independent compilation and testing
- **Offline-First**: Read operations work offline; write operations require connectivity with server as single source of truth
- **MVVM + StateFlow**: Predictable, reactive state management
- **Dependency Injection**: Hilt for loose coupling and testability
- **Clean Architecture**: Clear separation of concerns with domain, data, and UI layers

### Module Structure

```
:core       → Pure Kotlin domain layer (no Android framework)
:common     → Shared utilities and extensions
:data       → Data layer (Room, sync, networking, repositories)
:ui         → UI layer (Jetpack Compose, ViewModels)
:app        → Application entry point (DI setup, navigation)
```

## Key Features

### Offline-First Architecture
- **Read Operations**: Work offline from the local Room cache; refresh from the server when online (cache-first or network-first-with-cache-fallback, depending on the repo)
- **Write Operations**: Require online connectivity and **fail with an offline error when offline** — they are *not* currently queued for later
- **Sync queue (built, not wired)**: a `SyncManager` (retry/backoff/conflict), `SyncWorker` and connectivity trigger exist and back the Sync Status screen, but no write repo enqueues operations, so the queue stays empty — see `TECH_LEAD_REVIEW.md` §4 / `ROADMAP.md` Track A

### Data Management
- **Room Database**: Local caching for all entities (Client, Provider, Booking, Review, Payment)
- **SyncQueue**: Room-backed queue + `SyncManager` (retry/backoff/conflict) — built but not yet fed by the write repos (see `TECH_LEAD_REVIEW.md` §4)
- **DataStore**: User session storage — ⚠️ the auth token **and** profile (userId/email/role) are stored in plaintext today; see Known Issue #4
- **Mappers**: Convert between domain models, DTOs, and entities

### Backend Flexibility
- **IBackendService**: Abstract backend interface
- **BackendServiceStub**: Development stub provided
- **Easy Swapping**: Change Firebase/REST implementation via Hilt binding
- ⚠️ **`FirebaseBackendService` is not release-ready**: 14 methods still return
  `Result.Error(Exception("…not yet implemented"))`, and every Firestore *read* currently fails because the
  DTOs have no no-arg constructor — see Known Issues #6 and #26 before flipping
  `USE_MOCK_BACKEND=false`

## Getting Started

### Prerequisites

- Android Studio Iguana or later (Ladybug recommended)
- Android SDK 34
- Kotlin 1.9.0 (upgrade to 2.1.x recommended — see [Known Issues](#known-issues--audit-findings))

### Setup

1. **Clone the repository**
   ```bash
   cd Diamonds
   ```

2. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

### Architecture Documentation

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation:
- Module structure and dependencies
- Data flow patterns (read vs. write)
- Repository pattern implementation
- ViewModel pattern
- Testing strategy
- Backend flexibility approach

## Testing

### Run Unit Tests
```bash
./gradlew allUnitTests
```

### Run Instrumented Tests
```bash
./gradlew :app:connectedDebugAndroidTest
```

### Test Coverage
- Unit tests for repositories (offline behavior, error handling)
- Unit tests for ViewModels (state transitions)
- Compose UI tests for critical flows
- Current state (2026-07-27): **26 JVM unit-test files / 300 `@Test` methods** across `:data` and
  `:ui`, plus **23 instrumentation files** under `app/src/androidTest`
- Target coverage: 60%+
- ⚠️ CI compiles and runs the **JVM unit tests only** — `androidTest` is never built, so
  instrumentation breakage is invisible on a PR (see Known Issue #30)

## Technology Stack

- **Jetpack Compose**: Modern declarative UI
- **Room**: Local database with offline caching
- **DataStore**: Preferences storage — ⚠️ the session (auth token + profile) is stored in plaintext today; see Known Issue #4
- **WorkManager**: Background sync with backoff
- **Hilt**: Dependency injection
- **Coroutines & Flow**: Reactive programming
- **Material 3**: Design system

## Development Workflow

### Adding a New Screen

1. Create ViewModel extending `BaseViewModel<UiState>`
2. Create Compose screen function
3. Add navigation route
4. Inject required repositories

### Adding a New Repository

1. Create implementation of repository interface
2. Add binding in Hilt `RepositoryModule`
3. Write unit tests with mocked backend service
4. Implement offline/online logic per pattern

### Backend Integration

1. Implement `IBackendService` (e.g., `FirebaseBackendService`)
2. Update `BackendModule` Hilt binding
3. Add DTOs and mappers to `:data` module
4. Test with mocked responses

## Next Steps

> **Status convention:** `ROADMAP.md` is the single source of truth for phase status. "Built"
> below means the feature is implemented and works on the mock backend; most of these are tagged
> `✅ STRUCTURAL ⚠️` there because they silently fail against real Firebase (Track C).

- [x] ~~Build in-app chat system (Phase 11)~~ — built (structural)
- [x] ~~Add subscription and recurring bookings (Phase 12)~~ — built (structural)
- [x] ~~Implement multi-language support (Phase 13)~~ ✅ Complete
- [x] ~~Add reverse reviews - cleaner reviews customer (Phase 14)~~ — built (structural)
- [x] ~~Help, Support & Claims System (Phase 15)~~ — built (structural)
- [x] ~~Expand cleaning types, location types, and specializations (Phase 16)~~ — built (structural)
      (DB v10 + `MIGRATION_9_10`, `CleaningType`/`LocationType`, the two selectors,
      `SavedLocationsScreen`, specialization badges + filter)
- [ ] Add error handling and analytics (Phase 17)
- [ ] Comprehensive testing & optimization (Phase 18)
- [ ] Setup CI/CD pipeline and release preparation (Phase 19)
- [ ] Beta testing and launch (Phase 20–21)

---

## Known Issues & Audit Findings

> Last audited: **April 26, 2026**. Issues are prioritised P1 (blocker) → P3 (nice-to-have).
>
> **Update — 2026-06-25 (tech-lead review):** since the audit, the recurring-booking submit/load
> hang and the silent offline message-loss were fixed (PR #2), and CI that builds + runs all unit
> tests on every PR was added (PR #3). Issues #9, #13 and #18 below were found stale/inaccurate and
> are corrected inline.
>
> **Update — 2026-07-27 (re-verified against `develop` @ `4892952`).** The codebase is now
> **124 production `.kt` files / 25,598 LOC** across 5 modules, 25 ViewModels, Room DB v10 with
> 9 migrations. Issue **#9 is closed** — Track B (string externalisation, PRs #7–#17) shipped.
> Issue **#18 is now partial** — `SyncManager` and `LocationRepository` have tests; the Workers,
> `PreferencesDataStore` and `ConnectivityObserver` still do not. Five new findings (**#26–#30**)
> were added, including a Firestore read blocker that makes flipping `USE_MOCK_BACKEND=false`
> produce a *write-only* app.

### 🔴 P1 — Production Blockers

| # | Issue                                                                                                                                                                                                                 | Location                            |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| 1 | **No image loading library** — images cannot be displayed; Coil must be added                                                                                                                                         | `build.gradle.kts`                  |
| 2 | **`applicationId` is `com.example.diamonds`** — must be changed before release                                                                                                                                        | `app/build.gradle.kts:17`           |
| 3 | **`isMinifyEnabled = false` in release** — no obfuscation or code shrinking                                                                                                                                           | `app/build.gradle.kts:38`           |
| 4 | **Auth token stored as plaintext** in DataStore; `EncryptedDataStore` not used                                                                                                                                        | `PreferencesDataStore.kt`           |
| 5 | **No Firestore Security Rules file** — any authenticated user can read/write all documents                                                                                                                            | repo root (none committed)          |
| 6 | **14 `FirebaseBackendService` methods return `Result.Error(Exception("not yet implemented"))`** — support tickets, claims, cancel-with-reason, edit booking, refunds, help articles, saved locations silently fail in production | `FirebaseBackendService.kt:484–524` |
| 7 | **`getCurrentClient()` returns `Result.Error(Exception("Not implemented"))`**                                                                                                                                                    | `ClientRepository.kt:87`         |
| 8 | **`updateProvider()` returns `Result.Error(Exception("Not implemented"))`** — provider profile editing is broken                                                                                                                 | `ProviderRepository.kt:128`     |
| 26 | **Every Firestore *read* fails — DTOs have no no-arg constructor.** Every DTO in `IBackendService.kt` is a `data class` whose constructor params are all required (e.g. `ClientDto:113`, `BookingDto:162`), so Kotlin emits no zero-arg constructor. Firestore's object mapper needs one, so all **26** `toObject()`/`toObjects()` calls throw and are swallowed into `Result.Error` by the `firestoreCall` wrapper. Writes succeed. Flipping `USE_MOCK_BACKEND=false` today yields a **write-only app**. Fix: default every DTO field (or add explicit `@PropertyName` mappers). | `IBackendService.kt`, `FirebaseBackendService.kt` |
| 27 | **`SavedLocationRepository` reports failed remote writes as success** — `:32` maps `Result.Error → Result.Success(emptyList())` and `:45` maps `Result.Error → Result.Success(location)`, so the UI shows "saved" for data that never reached the backend. Same silent-data-loss class as the `MessageRepository` bug fixed in PR #2, and it has no tests. | `SavedLocationRepository.kt:32,45`  |

### 🟡 P2 — Important Fixes

| #  | Issue                                                                                                                                                     | Location                         |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| 9  | ✅ **RESOLVED (Track B, PRs #7–#17)** — multi-language is now real end-to-end. Runtime switching was already wired (`AppCompatDelegate.setApplicationLocales()`); the strings have since been externalised: `stringResource` is used in **46 of 83** `:ui` files across **608** call sites, `values/strings.xml` holds **531 `<string>` + 16 `<plurals>`**, and `values-fr`/`-es`/`-pt`/`-ar` are all key-complete. All `Icon` `contentDescription`s are localized (`cd_*` keys). Residual: ~**67** `Text("…")` literals remain, almost all non-translatable glyphs (emoji, `"$"`, `"›"`). **Still open:** no lint rule or baseline anywhere guards against new hardcoded strings | `ui/src/main/res/values*/strings.xml` |
| 10 | **`ReviewDirection` always lost in Firebase** — `createReview` never writes the `direction` field to Firestore                                            | `FirebaseBackendService.kt:~200` |
| 11 | **`DiamondsApplication` DI inconsistency** — manually constructs a second `ConnectivitySyncTrigger`; Hilt-injected instance is never started              | `DiamondsApplication.kt`         |
| 12 | **`ConnectivityObserver.isOnline()` false positives on captive portals** — only checks `NET_CAPABILITY_INTERNET`, not `NET_CAPABILITY_VALIDATED`          | `ConnectivityObserver.kt`        |
| 13 | **Full Firestore collection scans** — `searchProviders` and `getConversationsForUser` perform unfiltered reads; scalability blocker (`getReviewsForProvider` IS filtered — earlier audit was inaccurate) | `FirebaseBackendService.kt`      |
| 14 | **`android:allowBackup="true"`** with empty backup-rule files — DataStore (incl. the plaintext auth token) extractable via ADB                            | `AndroidManifest.xml:15`         |
| 15 | **Deep link `autoVerify="true"` with no Digital Asset Links file** — verification fails; any app can intercept `diamonds://` links                        | `AndroidManifest.xml:43`         |
| 16 | **`proguard-rules.pro` is empty** — must be written before `isMinifyEnabled = true` is enabled                                                            | `proguard-rules.pro`             |
| 17 | **No real payment processing** — `status = "SUCCEEDED"` is fabricated client-side with no PSP SDK. `PaymentScreen` also collects the card PAN and CVV in plain `OutlinedTextField`s with no masking and no `FLAG_SECURE` | `FirebaseBackendService.kt:267`, `PaymentScreen.kt` |
| 28 | **`LoginScreenTest` is broken by the Track B externalisation** — `LoginScreenTest.kt:153` asserts `"Demo accounts  (tap to fill)"` (two spaces) but the shipped `R.string.demo_accounts_hint` is `"Demo accounts (tap to fill)"` (one space), so the test fails. CI never runs instrumentation, so it was not caught | `app/src/androidTest/.../LoginScreenTest.kt:153` |
| 29 | **`androidTest` Hilt graph is incomplete** — `FakeRepositoryModule` `@TestInstallIn`-replaces `RepositoryModule` wholesale with 13 `@Provides`, but omits `ISavedLocationRepository`, which the production `Modules.kt` does provide | `app/src/androidTest/.../di/FakeRepositoryModule.kt` |
| 30 | **CI never builds or runs `androidTest`** — no `assembleDebugAndroidTest` step, which is why #28 and #29 were invisible. CI also runs no lint and has no coverage gate; `allUnitTests` aggregates `:app` and `:common` (neither has a `src/test` dir) and excludes `:core` entirely | `.github/workflows/ci.yml`       |

### 🔵 P3 — Improvements

| #  | Issue                                                                                                                                                                                                       | Location                 |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| 18 | ⚠️ **PARTIAL — coverage gaps in specific classes** (target 60%). Closed by Track D: `SyncManagerTest` (21 tests) and `LocationRepositoryTest` (8 tests) landed, boilerplate `Example*Test` files were deleted, and `SyncManager` correctness fixes shipped (PRs #19–#21). **Still untested:** `SyncWorker`, `RecurringBookingWorker`, `PreferencesDataStore`, `ConnectivityObserver` and 5 repositories. Also: no `DispatcherProvider`; Turbine is declared in `:data`/`:ui` but has **0** uses; `collectAsStateWithLifecycle` has **0** uses against **87** `collectAsState()` sites in 44 files. Stale claim retracted: "~15–20% / `BookingRepositoryTest` is a no-op `assert(true)`" | `data/test/`, `ui/test/` |
| 19 | **Kotlin 1.9.0** — upgrade to 2.1.x for K2 compiler and latest stdlib                                                                                                                                       | `libs.versions.toml`     |
| 20 | **Compose 1.6.0** — upgrade to 1.7.x for stability improvements                                                                                                                                             | `libs.versions.toml`     |
| 21 | **`ACCESS_BACKGROUND_LOCATION` and `POST_NOTIFICATIONS` declared but never requested at runtime** — neither has a runtime-permission call site anywhere; foreground-only location suffices if tracking occurs on the active booking screen                                                            | `AndroidManifest.xml:9,11` |
| 22 | **`MAPS_API_KEY` is empty string** committed in `gradle.properties`                                                                                                                                         | `gradle.properties:25`   |
| 23 | **`BackendServiceStub` is 1,131 lines** — seed data difficult to maintain in a single file                                                                                                                  | `BackendServiceStub.kt`  |
| 24 | **`org.gradle.parallel=true` commented out** — enabling would speed up multi-module builds                                                                                                                  | `gradle.properties`      |
| 25 | **No `signingConfig` block** for release build                                                                                                                                                              | `app/build.gradle.kts`   |

---

TODO: Add license information

## Contributing

Guidelines coming soon.
