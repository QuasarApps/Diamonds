# Project File Structure

**Regenerated from the actual tree on 2026-07-27, against `develop` @ `4892952`.**

Scale at that commit: **124 production `.kt` files / 25,598 production LOC** across five Gradle
modules, plus **26 JVM unit-test files (300 `@Test` methods)** and **23 instrumentation-test files**.
(The old "24 files / 3,250 LOC / 7 entities / 7 repositories" figures were frozen at Phase 1 and are
no longer accurate — they have been replaced throughout.)

## Complete Directory Tree

```
Diamonds/
│
├── README.md                           # Project overview, key features, Known Issues
├── ARCHITECTURE.md                     # Module/data-flow/repository/ViewModel patterns
├── DEVELOPMENT.md                      # Development setup guide
├── ROADMAP.md                          # Remediation Roadmap (Tracks A–E) + 21-phase plan
├── TECH_LEAD_REVIEW.md                 # Independent review, file:line-cited findings
├── AUDIT_REPORT.md                     # Audit + Known Issues
├── QUICK_REFERENCE.md                  # Copy-paste patterns
├── IMPLEMENTATION_SUMMARY.md           # What's built vs. what only looks built
├── FILE_STRUCTURE.md                   # This file
├── CLAUDE.md                           # Guidance for AI agents working in the repo
│
├── build.gradle.kts                    # Root build config + the `allUnitTests` aggregate task
├── settings.gradle.kts                 # Module includes (:app :core :data :ui :common)
├── gradle.properties                   # JVM args, AndroidX flags, MAPS_API_KEY (optional)
│
├── .github/
│   └── workflows/
│       └── ci.yml                      # assembleDebug + allUnitTests on PRs/pushes to develop
│
├── gradle/
│   ├── libs.versions.toml              # Centralized version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── gradlew / gradlew.bat               # Gradle wrappers
│
│
├── core/                               # DOMAIN LAYER — pure Kotlin, zero Android imports
│   ├── build.gradle.kts                # `java-library` + kotlin("jvm")
│   └── src/main/java/com/example/diamonds/domain/
│       ├── model/
│       │   ├── DomainModels.kt         # 514 LOC — 20 domain data classes + 18 enums
│       │   └── Result.kt               # 53 LOC — sealed Success / Error / Loading
│       └── repository/
│           └── Repositories.kt         # 373 LOC — 14 repository interfaces
│                                       #   (no src/test — :core has no tests yet)
│
│
├── common/                             # SHARED UTILITIES
│   ├── build.gradle.kts
│   └── src/main/java/com/example/diamonds/common/
│       ├── util/
│       │   ├── Constants.kt            # App constants, ConnectivityState enum
│       │   └── DateTimeFormatUtil.kt   # Locale-aware date/time formatting
│       └── ext/
│           └── FlowExt.kt              # Flow extensions
│                                       #   (no src/test, despite :common being in allUnitTests)
│
│
├── data/                               # DATA LAYER — 30 files / 6,801 LOC
│   ├── build.gradle.kts
│   ├── src/main/java/com/example/diamonds/data/
│   │   │
│   │   ├── local/
│   │   │   ├── AppDatabase.kt          # Room DB **version 10**, 15 entities, 15 DAOs,
│   │   │   │                           #   9 migrations MIGRATION_1_2 … MIGRATION_9_10
│   │   │   ├── entity/Entities.kt      # 15 @Entity classes
│   │   │   ├── dao/Daos.kt             # 15 DAO interfaces
│   │   │   └── preferences/
│   │   │       └── PreferencesDataStore.kt  # Session/FCM/language/notif prefs —
│   │   │                                    #   ⚠️ PLAINTEXT DataStore (token + PII)
│   │   │
│   │   ├── remote/
│   │   │   ├── auth/
│   │   │   │   ├── IAuthService.kt          # Auth contract
│   │   │   │   ├── MockAuthService.kt       # debug default (USE_MOCK_AUTH=true)
│   │   │   │   └── FirebaseAuthService.kt   # release path — signup writes no profile doc
│   │   │   └── backend/
│   │   │       ├── IBackendService.kt       # Backend contract + 25 DTO data classes
│   │   │       ├── BackendServiceStub.kt    # 1,131 LOC of seeded demo data
│   │   │       ├── FirebaseBackendService.kt# 538 LOC — 14 methods still stubbed
│   │   │       └── FirestoreBookingListener.kt
│   │   │
│   │   ├── connectivity/
│   │   │   └── ConnectivityObserver.kt
│   │   │
│   │   ├── repository/                 # 13 repository implementations
│   │   │   ├── AuthRepository.kt
│   │   │   ├── ClientRepository.kt
│   │   │   ├── ProviderRepository.kt
│   │   │   ├── ServiceRepository.kt
│   │   │   ├── BookingRepository.kt
│   │   │   ├── ReviewRepository.kt
│   │   │   ├── PaymentRepository.kt
│   │   │   ├── NotificationRepository.kt
│   │   │   ├── LocationRepository.kt
│   │   │   ├── MessageRepository.kt
│   │   │   ├── SubscriptionRepository.kt
│   │   │   ├── SupportRepository.kt
│   │   │   └── SavedLocationRepository.kt
│   │   │       (ISyncRepository is implemented by sync/SyncManager.kt, not by a
│   │   │        file in this package — 14 interfaces, 13 files here + SyncManager)
│   │   │
│   │   ├── mapper/
│   │   │   └── Mappers.kt              # 681 LOC — Entity ↔ Domain ↔ DTO
│   │   │
│   │   ├── sync/
│   │   │   ├── SyncManager.kt          # Queue, backoff, conflict resolution (ISyncRepository)
│   │   │   └── ConnectivitySyncTrigger.kt
│   │   │
│   │   └── worker/
│   │       ├── SyncWorker.kt
│   │       └── RecurringBookingWorker.kt
│   │
│   └── src/test/java/com/example/diamonds/data/   # 12 files / 2,642 LOC / 172 @Test
│       ├── mapper/MappersTest.kt
│       ├── remote/backend/BackendServiceStubTest.kt
│       ├── sync/SyncManagerTest.kt
│       └── repository/
│           ├── AuthRepositoryTest.kt
│           ├── BookingRepositoryTest.kt
│           ├── BookingRepositoryFullTest.kt
│           ├── LocationRepositoryTest.kt
│           ├── MessageRepositoryTest.kt
│           ├── PaymentRepositoryTest.kt
│           ├── ReviewRepositoryTest.kt
│           ├── SubscriptionRepositoryTest.kt
│           └── SupportRepositoryTest.kt
│
│
├── ui/                                 # UI LAYER — 83 files / 17,090 LOC
│   ├── build.gradle.kts
│   ├── src/main/java/com/example/diamonds/ui/     # organized by FEATURE package
│   │   │                                          #   (there is no `screens/` directory)
│   │   ├── base/BaseViewModel.kt
│   │   ├── theme/Theme.kt
│   │   │
│   │   ├── navigation/                 # Screen.kt (49 routes), DiamondsNavHost.kt, BottomTab.kt
│   │   ├── shell/                      # AppShell.kt, AppShellViewModel.kt, TabScreens.kt
│   │   ├── splash/                     # SplashScreen.kt, SplashViewModel.kt
│   │   ├── auth/                       # LoginScreen, SignupScreen, AuthViewModel
│   │   │
│   │   ├── booking/                    # ProviderSearch, ServiceList, BookingForm,
│   │   │                               #   BookingConfirmation, BookingsList,
│   │   │                               #   ProviderRatings, Review + BookingViewModel,
│   │   │                               #   ReviewViewModel
│   │   ├── customer/                   # CustomerProfileScreen
│   │   ├── cleaner/                    # Dashboard/requests/bookings/schedule/earnings,
│   │   │                               #   profile, service management + edit
│   │   ├── company/                    # Dashboard, bookings, team, earnings
│   │   ├── payment/                    # PaymentScreen, PaymentSuccess, PaymentHistory
│   │   ├── review/                     # Reverse reviews: LeaveClientReview, ClientRatings
│   │   ├── chat/                       # ConversationListScreen, ChatScreen, ChatViewModel
│   │   ├── map/                        # BookingMapScreen, ProviderTrackingScreen,
│   │   │                               #   BookingLocationMapCard, MapViewModel
│   │   ├── notification/               # NotificationScreen, NotificationPreferencesScreen
│   │   ├── subscription/               # RecurringBookingSetup, SubscriptionManagement
│   │   ├── support/                    # HelpCenter, ContextualHelp, FileClaim,
│   │   │                               #   CancelBooking, EditBooking (+ ViewModels)
│   │   ├── profile/                    # SavedLocationsScreen, SavedLocationsViewModel
│   │   ├── settings/                   # LanguageSelectorScreen, LanguageViewModel
│   │   ├── sync/                       # SyncStatusScreen, SyncStatusViewModel
│   │   ├── components/                 # ConfirmationDialog, ErrorScreens, FilterSheet,
│   │   │                               #   PullToRefresh, LocationPermission,
│   │   │                               #   CleaningTypeSelector, LocationTypeSelector
│   │   └── placeholder/                # PlaceholderScreen
│   │
│   ├── src/main/res/
│   │   ├── values/strings.xml          # 531 <string> + 16 <plurals>
│   │   ├── values-fr/strings.xml       # key-complete
│   │   ├── values-es/strings.xml       # key-complete
│   │   ├── values-pt/strings.xml       # key-complete
│   │   └── values-ar/strings.xml       # key-complete
│   │
│   └── src/test/java/com/example/diamonds/ui/     # 14 files / 2,749 LOC / 128 @Test
│       ├── MainDispatcherRule.kt
│       ├── base/BaseViewModelTest.kt
│       ├── auth/AuthViewModelTest.kt
│       ├── booking/{BookingViewModelTest, ReviewViewModelTest}.kt
│       ├── chat/ChatViewModelTest.kt
│       ├── cleaner/{CleanerViewModelTest, CleanerEarningsViewModelTest}.kt
│       ├── company/{CompanyViewModelTest, CompanyEarningsViewModelTest}.kt
│       ├── payment/PaymentViewModelTest.kt
│       ├── subscription/RecurringBookingViewModelTest.kt
│       ├── support/CancelBookingViewModelTest.kt
│       └── sync/SyncStatusViewModelTest.kt
│                                       #   (:ui has no androidTest — all instrumentation
│                                       #    tests live in :app)
│
│
├── app/                                # APPLICATION ENTRY POINT — 5 files / 555 LOC
│   ├── build.gradle.kts                # applicationId, USE_MOCK_AUTH / USE_MOCK_BACKEND flags
│   ├── google-services.json            # ⚠️ placeholder (project_number "000000000000")
│   ├── proguard-rules.pro              # ⚠️ untouched empty template (isMinifyEnabled = false)
│   │
│   ├── src/main/java/com/example/diamonds/
│   │   ├── DiamondsApplication.kt      # @HiltAndroidApp, channels, WorkManager scheduling
│   │   ├── MainActivity.kt             # Hosts the :ui nav graph
│   │   ├── di/
│   │   │   ├── Modules.kt              # DataModule + RepositoryModule (Hilt bindings)
│   │   │   └── AuthServiceModule.kt    # Mock vs. Firebase auth by BuildConfig
│   │   └── fcm/DiamondsFcmService.kt
│   │
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/res/
│   │   ├── drawable/ · mipmap-*/
│   │   ├── values/{strings,colors,themes}.xml   # 169 strings
│   │   ├── values-{fr,es,pt,ar}/strings.xml     # 169 each, key-complete
│   │   ├── values-night/themes.xml
│   │   └── xml/{backup_rules,data_extraction_rules,locale_config}.xml
│   │
│   └── src/androidTest/java/com/example/diamonds/   # 23 files / 2,834 LOC
│       ├── HiltTestRunner.kt
│       ├── di/{FakeAuthServiceModule, FakeDataModule,
│       │       FakeRepositories, FakeRepositoryModule}.kt
│       └── ui/                          # 17 Compose test files + TestHelpers.kt
│                                        #   ⚠️ never compiled or run by CI
│                                        #   (:app has no src/test, despite being in allUnitTests)
```

## Module Breakdown

### `:core` Module
**Size**: 3 files / 940 LOC
**Dependencies**: No project dependencies — a `java-library` with only kotlin-stdlib,
kotlinx-serialization-json and kotlinx-coroutines-core; contains zero Android imports
**Purpose**: Define business domain contracts

**Key Files**:
- `domain/model/DomainModels.kt` — 20 domain data classes + 18 enums (Client, Provider, Service,
  Booking, Review, Payment, Notification, Message, Conversation, RecurringBooking, SupportTicket,
  Claim, SavedLocation, UserSession, …)
- `domain/model/Result.kt` — sealed `Success` / `Error` / `Loading`
- `domain/repository/Repositories.kt` — 14 interfaces: `IAuthRepository`, `IClientRepository`,
  `IProviderRepository`, `IServiceRepository`, `IBookingRepository`, `IReviewRepository`,
  `IPaymentRepository`, `ISyncRepository`, `INotificationRepository`, `ILocationRepository`,
  `IMessageRepository`, `ISubscriptionRepository`, `ISupportRepository`, `ISavedLocationRepository`

**Gaps**: no `src/test` directory, and `:core` is *not* included in the root `allUnitTests` task.

### `:common` Module
**Size**: 3 files / 212 LOC
**Dependencies**: `:core` (plus AndroidX core/appcompat, Compose and DataStore)
**Purpose**: Shared utilities across modules

**Key Files**:
- `common/util/Constants.kt` — app constants, `ConnectivityState`
- `common/util/DateTimeFormatUtil.kt` — locale-aware formatting
- `common/ext/FlowExt.kt` — Flow helpers

**Gaps**: no `src/test` directory (but `:common:testDebugUnitTest` is wired into `allUnitTests`).

### `:data` Module
**Size**: 30 files / 6,801 LOC
**Dependencies**: `:core`, `:common`, Room, Coroutines, Serialization, Firebase, WorkManager
**Purpose**: Persistence, remote access, sync, connectivity

**Key Files**:
- `local/AppDatabase.kt` — Room **version 10**; 15 entities, 15 DAOs, 9 migrations
- `local/entity/Entities.kt` / `local/dao/Daos.kt`
- `remote/backend/IBackendService.kt` — backend contract plus 25 DTO data classes
- `remote/backend/BackendServiceStub.kt` — the debug backend; all seeded demo data lives here
- `repository/*.kt` — 13 implementations (see tree)
- `sync/SyncManager.kt` — implements `ISyncRepository`
- `mapper/Mappers.kt` — all model conversions

**Largest files**: `BackendServiceStub.kt` (1,131), `Mappers.kt` (681), `FirebaseBackendService.kt`
(538), `IBackendService.kt` (436), `Daos.kt` (418).

**Gaps**: `exportSchema = true` with no `room.schemaLocation` and no `schemas/` directory, so no
migration is schema-verified; `SyncWorker`, `RecurringBookingWorker`, `PreferencesDataStore` and
`ConnectivityObserver` have no tests.

### `:ui` Module
**Size**: 83 files / 17,090 LOC across 22 feature packages
**Dependencies**: `:core`, `:data`, `:common`, Compose, Hilt, Maps
**Purpose**: All Compose screens, ViewModels and navigation

**Layout**: organized **by feature package**, not by layer — there is no `screens/` directory.
45 files are `*Screen.kt`, 25 are `*ViewModel.kt`, and `navigation/Screen.kt` declares 49 routes.

| Package        | Files | LOC   | Package         | Files | LOC   |
|----------------|-------|-------|-----------------|-------|-------|
| `cleaner`      | 10    | 2,831 | `map`           | 4     | 781   |
| `support`      | 10    | 1,344 | `chat`          | 3     | 768   |
| `booking`      | 9     | 2,479 | `subscription`  | 3     | 710   |
| `components`   | 7     | 713   | `review`        | 3     | 692   |
| `company`      | 6     | 1,226 | `auth`          | 3     | 665   |
| `payment`      | 4     | 818   | `notification`  | 3     | 558   |
| `shell`        | 3     | 1,633 | `navigation`    | 3     | 312   |
| `sync`         | 2     | 568   | `splash`        | 2     | 174   |
| `profile`      | 2     | 329   | `settings`      | 2     | 183   |
| `customer`     | 1     | 204   | `base`          | 1     | 59    |
| `placeholder`  | 1     | 22    | `theme`         | 1     | 21    |

**Resources**: `ui/src/main/res/values/strings.xml` holds 531 `<string>` + 16 `<plurals>`, mirrored
key-complete in `values-fr`, `values-es`, `values-pt` and `values-ar`. 46 of the 83 Kotlin files
reference `stringResource` (608 occurrences); roughly 67 residual `Text("…")` literals remain,
almost all non-translatable glyphs (emoji, `"$"`, `"›"`).

**Gaps**: 0 uses of `collectAsStateWithLifecycle` against 87 `collectAsState()` call sites in 44
files; 37 `as? Result.Success` sites across 10 ViewModel files silently discard `Result.Error`.

### `:app` Module
**Size**: 5 files / 555 LOC
**Dependencies**: `:ui`, `:data`, `:core`, `:common`
**Purpose**: Entry point, Hilt wiring, FCM, instrumentation tests

**Key Files**:
- `DiamondsApplication.kt` — `@HiltAndroidApp`; creates notification channels, schedules
  `SyncWorker` and `RecurringBookingWorker`, starts a `ConnectivitySyncTrigger`
- `MainActivity.kt` — hosts `DiamondsNavHost`
- `di/Modules.kt` (258 LOC) — binds every `:core` interface to its `:data` implementation
- `di/AuthServiceModule.kt` — `MockAuthService` vs. `FirebaseAuthService` by `BuildConfig`
- `fcm/DiamondsFcmService.kt`

**Gaps**: no `src/test` directory (yet `:app:testDebugUnitTest` is part of `allUnitTests`); the
23-file `androidTest` suite is never compiled or run by CI.

## Build Configuration Files

### Root `build.gradle.kts`
- Plugin declarations, all `apply false` (AGP, Kotlin Android, Hilt 2.50, Google Services)
- `allUnitTests` aggregate task → `:app`, `:common`, `:data`, `:ui` `testDebugUnitTest`
  (**`:core` is not included**, and `:app`/`:common` have no tests to run)

### Module `build.gradle.kts`
- `:core` — `java-library` + `org.jetbrains.kotlin.jvm`, JVM 11 target, no Android
- `:common` — `:core` + AndroidX/Compose/DataStore
- `:data` — `:core`, `:common`; Room (kapt), Coroutines, kotlinx-serialization, Firebase BoM
  (auth/firestore/messaging), WorkManager, Hilt; `room-testing` on the test classpath
- `:ui` — `:core`, `:data`, `:common`; Compose, Hilt, Lifecycle, Maps Compose
- `:app` — all four modules + WorkManager + Firebase BoM + Hilt instrumentation-test deps

### `app/build.gradle.kts`
- `applicationId = "com.example.diamonds"`, `compileSdk`/`targetSdk` 34
- `debug`: `USE_MOCK_AUTH = true`, `USE_MOCK_BACKEND = true`
- `release`: both `false`, `isMinifyEnabled = false`, **no `signingConfig`**
- `MAPS_API_KEY` read from a Gradle property, defaulting to empty

### `gradle/libs.versions.toml`
- Centralized version catalog used by every module (`libs.*`)
- Current: AGP 8.5.0, Kotlin 1.9.0, Compose 1.6.0 / Material3 1.2.0, Hilt 2.50, Room 2.6.1

### `.github/workflows/ci.yml`
- Runs `./gradlew assembleDebug allUnitTests` on JDK 17, for pushes to `develop` and PRs targeting it
- Uploads unit-test HTML reports as an artifact
- Does **not** run `assembleDebugAndroidTest`, lint, or any coverage gate

## How to Find Things

### Domain Models
→ `core/src/main/java/com/example/diamonds/domain/model/DomainModels.kt`

### Repository Interfaces
→ `core/src/main/java/com/example/diamonds/domain/repository/Repositories.kt`

### Database (entities, DAOs, migrations)
→ `data/src/main/java/com/example/diamonds/data/local/`

### Repository Implementations
→ `data/src/main/java/com/example/diamonds/data/repository/` (plus `sync/SyncManager.kt`)

### Seeded Demo Data
→ `data/src/main/java/com/example/diamonds/data/remote/backend/BackendServiceStub.kt`

### ViewModels and Compose Screens
→ `ui/src/main/java/com/example/diamonds/ui/<feature>/` — both live side by side in the feature
package (e.g. `ui/booking/BookingViewModel.kt` and `ui/booking/BookingFormScreen.kt`)

### Navigation
→ `ui/src/main/java/com/example/diamonds/ui/navigation/` (routes) and `ui/shell/AppShell.kt`
(inner NavHost with the bottom tabs)

### Strings / Translations
→ `ui/src/main/res/values*/strings.xml` (the bulk) and `app/src/main/res/values*/strings.xml`

### Dependency Injection
→ `app/src/main/java/com/example/diamonds/di/`

### Unit Tests
→ `data/src/test/` and `ui/src/test/`

### Instrumentation Tests
→ `app/src/androidTest/java/com/example/diamonds/`

## Adding New Files

### New Repository
1. Add the interface to `core/domain/repository/Repositories.kt`
2. Add the entity to `data/local/entity/Entities.kt` and the DAO to `data/local/dao/Daos.kt`
3. Register both on `AppDatabase` and add a `Migration` (bump the DB version)
4. Add DTO(s) + methods to `data/remote/backend/IBackendService.kt`, then implement them in
   **both** `BackendServiceStub` and `FirebaseBackendService`
5. Create `data/repository/XyzRepository.kt`
6. Add mappers to `data/mapper/Mappers.kt`
7. Bind it in `app/di/Modules.kt` — **and** add a fake to
   `app/src/androidTest/di/FakeRepositoryModule.kt`, which replaces `RepositoryModule` wholesale

### New Screen
1. Create the `UiState` + ViewModel in `ui/<feature>/XyzViewModel.kt`
2. Create the Composable in `ui/<feature>/XyzScreen.kt`
3. Add a route to `ui/navigation/Screen.kt` and wire it into `ui/shell/AppShell.kt`
4. Put every user-facing literal in `ui/src/main/res/values/strings.xml` and add the key to all
   four translated locales

### New Utility
1. Shared across modules → `common/src/main/java/com/example/diamonds/common/`
2. Data-layer only → `data/src/main/java/com/example/diamonds/data/util/`

## File Statistics

| Component                | Files   | Lines      | Notes                                          |
|--------------------------|---------|------------|------------------------------------------------|
| `:core` domain           | 3       | 940        | 20 data classes, 18 enums, 14 repo interfaces   |
| `:common` utilities      | 3       | 212        |                                                |
| `:data`                  | 30      | 6,801      | Room v10 · 15 entities · 15 DAOs · 9 migrations |
| `:ui`                    | 83      | 17,090     | 45 screens · 25 ViewModels · 49 routes          |
| `:app`                   | 5       | 555        | Hilt wiring, FCM, entry point                   |
| **Production total**     | **124** | **25,598** |                                                |
| JVM unit tests           | 26      | 5,391      | 300 `@Test` (`:data` 12/172, `:ui` 14/128)      |
| Instrumentation tests    | 23      | 2,834      | `app/src/androidTest` — **not run by CI**       |
| Root documentation       | 10      | —          | see the tree above                             |

---

**Note**: the module graph is strictly acyclic — `:common` → `:core`; `:data` → `:core`, `:common`;
`:ui` → `:core`, `:data`, `:common`; `:app` → all four. Lower layers never depend on higher ones,
which is what keeps parallel work on separate modules conflict-free.
