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
   ./gradlew build
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
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
- Unit tests for repositories (offline behavior, error handling)
- Unit tests for ViewModels (state transitions)
- Compose UI tests for critical flows
- Target coverage: 60%+

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

- [x] ~~Build in-app chat system (Phase 11)~~ ✅ Complete
- [x] ~~Add subscription and recurring bookings (Phase 12)~~ ✅ Complete
- [x] ~~Implement multi-language support (Phase 13)~~ ✅ Complete
- [x] ~~Add reverse reviews - cleaner reviews customer (Phase 14)~~ ✅ Complete
- [x] ~~Help, Support & Claims System (Phase 15)~~ ✅ Complete
- [ ] Expand cleaning types, location types, and specializations (Phase 16)
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

### 🔴 P1 — Production Blockers

| # | Issue                                                                                                                                                                                                                 | Location                            |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| 1 | **No image loading library** — images cannot be displayed; Coil must be added                                                                                                                                         | `build.gradle.kts`                  |
| 2 | **`applicationId` is `com.example.diamonds`** — must be changed before release                                                                                                                                        | `app/build.gradle.kts:17`           |
| 3 | **`isMinifyEnabled = false` in release** — no obfuscation or code shrinking                                                                                                                                           | `app/build.gradle.kts:38`           |
| 4 | **Auth token stored as plaintext** in DataStore; `EncryptedDataStore` not used                                                                                                                                        | `PreferencesDataStore.kt`           |
| 5 | **No Firestore Security Rules file** — any authenticated user can read/write all documents                                                                                                                            | Firebase Console                    |
| 6 | **14 `FirebaseBackendService` methods return `Result.Error("not yet implemented")`** — support tickets, claims, cancel-with-reason, edit booking, refunds, help articles, saved locations silently fail in production | `FirebaseBackendService.kt:485–524` |
| 7 | **`getCurrentClient()` returns `Result.Error("Not implemented")`**                                                                                                                                                    | `ClientRepository.kt:86–91`         |
| 8 | **`updateProvider()` returns `Result.Error("Not implemented")`** — provider profile editing is broken                                                                                                                 | `ProviderRepository.kt:127–130`     |

### 🟡 P2 — Important Fixes

| #  | Issue                                                                                                                                                     | Location                         |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| 9  | **Multi-language feature is inert** — runtime switching IS wired (`AppCompatDelegate.setApplicationLocales()` in `LanguageSelectorScreen`); the real gap is ~350 hardcoded `Text("…")` literals (`stringResource` used in 1 of 83 UI files), so switching locale has almost no visible effect                         | `LanguageSelectorScreen.kt`      |
| 10 | **`ReviewDirection` always lost in Firebase** — `createReview` never writes the `direction` field to Firestore                                            | `FirebaseBackendService.kt:~200` |
| 11 | **`DiamondsApplication` DI inconsistency** — manually constructs a second `ConnectivitySyncTrigger`; Hilt-injected instance is never started              | `DiamondsApplication.kt`         |
| 12 | **`ConnectivityObserver.isOnline()` false positives on captive portals** — only checks `NET_CAPABILITY_INTERNET`, not `NET_CAPABILITY_VALIDATED`          | `ConnectivityObserver.kt`        |
| 13 | **Full Firestore collection scans** — `searchProviders` and `getConversationsForUser` perform unfiltered reads; scalability blocker (`getReviewsForProvider` IS filtered — earlier audit was inaccurate) | `FirebaseBackendService.kt`      |
| 14 | **`android:allowBackup="true"`** — DataStore (incl. auth token) extractable via ADB                                                                       | `AndroidManifest.xml:14`         |
| 15 | **Deep link `autoVerify="true"` with no Digital Asset Links file** — verification fails; any app can intercept `diamonds://` links                        | `AndroidManifest.xml:44`         |
| 16 | **`proguard-rules.pro` is empty** — must be written before `isMinifyEnabled = true` is enabled                                                            | `proguard-rules.pro`             |
| 17 | **No real payment processing** — card payments immediately set to `SUCCEEDED` with no Stripe/Braintree SDK                                                | `PaymentRepository.kt`           |

### 🔵 P3 — Improvements

| #  | Issue                                                                                                                                                                                                       | Location                 |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| 18 | **Coverage gaps in specific classes** (target 60%) — `SyncManager`, both Workers, `PreferencesDataStore`, `ConnectivityObserver` and several repos still lack tests. Stale claim: "~15–20% / `BookingRepositoryTest` is a no-op `assert(true)`" — that test now has real assertions, and PR #2's regression tests now run in CI | `data/test/`, `ui/test/` |
| 19 | **Kotlin 1.9.0** — upgrade to 2.1.x for K2 compiler and latest stdlib                                                                                                                                       | `libs.versions.toml`     |
| 20 | **Compose 1.6.0** — upgrade to 1.7.x for stability improvements                                                                                                                                             | `libs.versions.toml`     |
| 21 | **`ACCESS_BACKGROUND_LOCATION` declared but likely unnecessary** — foreground-only suffices if tracking only occurs during active booking screen                                                            | `AndroidManifest.xml:9`  |
| 22 | **`MAPS_API_KEY` is empty string** committed in `gradle.properties`                                                                                                                                         | `gradle.properties:25`   |
| 23 | **`BackendServiceStub` is 1,132 lines** — seed data difficult to maintain in a single file                                                                                                                  | `BackendServiceStub.kt`  |
| 24 | **`org.gradle.parallel=true` commented out** — enabling would speed up multi-module builds                                                                                                                  | `gradle.properties`      |
| 25 | **No `signingConfig` block** for release build                                                                                                                                                              | `app/build.gradle.kts`   |

---

TODO: Add license information

## Contributing

Guidelines coming soon.
