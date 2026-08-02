# Diamonds App Architecture Documentation

## Overview

Diamonds is built using a modular, offline-first Android architecture with the following key principles:

1. **Modular Structure**: Separate Gradle modules for independent compilation and testing
2. **Offline-First**: Read operations work offline; write operations require connectivity
3. **Server as Source of Truth**: Backend validates all business logic
4. **Reactive State Management**: MVVM + StateFlow for predictable data flows
5. **Dependency Injection**: Hilt for loose coupling and testability

### Scale (as of 2026-07-27, `develop` @ 4892952)

| Metric | Value |
|--------|-------|
| Production Kotlin files (`*/src/main`) | 124 (25,598 LOC) |
| ViewModels (`:ui`) | 25 |
| Room database version | 10 (`MIGRATION_1_2` … `MIGRATION_9_10`) |
| JVM unit tests | 26 files / 300 `@Test` methods (`:data`, `:ui`) |
| Instrumentation tests | 23 files (`app/src/androidTest`) |

## Module Structure

```
Diamonds/
├── :core                    # Pure Kotlin domain layer
│   └── domain/
│       ├── model/          # Domain entities (Client, Provider, Booking, etc.)
│       ├── repository/     # Repository interfaces (IBookingRepository, etc.)
│
├── :common                  # Shared utilities and extensions
│   └── common/
│       ├── util/          # Constants, utilities
│       └── ext/           # Kotlin extensions (Flow, etc.)
│
├── :data                    # Data layer (Room, sync, networking)
│   └── data/
│       ├── local/         # Room database, DAOs, entities
│       ├── remote/        # Backend service interfaces and DTOs
│       ├── connectivity/  # Network state observation
│       ├── repository/    # Repository implementations
│       └── sync/          # Sync queue and SyncManager
│
├── :ui                      # UI layer (Compose, ViewModels)
│   └── ui/                # Organised by FEATURE package, not by layer —
│       │                  # each package holds its screens + ViewModel together
│       ├── auth/          # Login, signup, role selection
│       ├── booking/       # Booking form/list/confirmation, provider search, reviews
│       ├── chat/          # Conversations and chat screen
│       ├── cleaner/       # Cleaner dashboard and job flows
│       ├── company/       # Company dashboard
│       ├── customer/      # Customer dashboard
│       ├── map/           # Map + location picking
│       ├── navigation/    # DiamondsNavHost, Screen routes, BottomTab
│       ├── notification/  # Notification centre
│       ├── payment/       # Payment + payment-method screens
│       ├── profile/       # Saved locations (SavedLocationsScreen + ViewModel)
│       ├── review/        # Reverse reviews
│       ├── settings/      # Language selector (LanguageSelectorScreen + ViewModel)
│       ├── shell/         # AppShell scaffolding
│       ├── splash/        # Splash
│       ├── subscription/  # Subscriptions and recurring bookings
│       ├── support/       # Help, support, claims
│       ├── sync/          # Sync status screen
│       ├── components/    # Reusable UI components
│       ├── placeholder/   # PlaceholderScreen for unbuilt routes
│       ├── theme/         # Material 3 theme setup
│       └── base/          # BaseViewModel
│
└── :app                     # Application entry point
    ├── MainActivity.kt
    ├── DiamondsApplication.kt
    └── di/                 # Hilt modules
```

## Data Flow

### Read Operations (Offline-Capable)
```
User Action
    ↓
ViewModel calls Repository.getXxx()
    ↓
Repository consults Room + connectivity
    ↓
If ONLINE  → fetch from backend → update cache → return
             (on network failure → fall back to the cached rows)
If OFFLINE → return the cached rows, or an error if nothing is cached
```

The *order* varies per repository and is deliberate, not accidental: some reads are
network-first-when-online with a cache fallback (`BookingRepository.getClientBookings`), others
are cache-first (`MessageRepository.getOrCreateConversation`). Read the specific repository before
assuming one shape. Either way the read path degrades to the Room cache when the network is
unavailable — this is implemented, not aspirational.

### Write Operations (Online-Required)
```
User Action
    ↓
ViewModel calls Repository.createXxx()
    ↓
Repository checks isOnline()
    ↓
If OFFLINE → return OfflineException (user sees error, button disabled)
If ONLINE → send to backend immediately
    ↓
Backend responds with success → update cache → return success
Backend responds with error → return error
```

## Key Components

### 1. Domain Layer (`:core`)
- **Purpose**: Pure business logic and contracts, no framework dependencies
- **Contains**:
  - Domain models (Client, Provider, Booking, Review, Payment)
  - Repository interfaces (contracts, not implementations)
  - Result<T> wrapper for typed error handling
  - Enums (SyncStatus, BookingStatus, etc.)

### 2. Common Module (`:common`)
- **Purpose**: Shared utilities used by multiple modules
- **Contains**:
  - Constants (sync intervals, timeouts, etc.)
  - ConnectivityState enum
  - Flow extensions
  - Helper functions

### 3. Data Layer (`:data`)
- **Purpose**: Abstracts data access (database, network, sync)
- **Components**:
  - **AppDatabase**: Room database with DAOs
  - **ConnectivityObserver**: Monitors network state
  - **PreferencesDataStore**: User session storage — **plaintext**, see the warning below
  - **IBackendService**: Abstract backend interface (stub provided)
  - **Repositories**: Implement read/write logic with offline checks
  - **SyncManager**: Handles queued operations with exponential backoff
  - **Mappers**: Convert between domain models, DTOs, and entities

> ⚠️ **Session storage is not encrypted.** `PreferencesDataStore` (`data/local/preferences/PreferencesDataStore.kt`)
> uses a plain `preferencesDataStore("app_preferences")`. The auth token, user id, email, display
> name and role are written as **plaintext** preference keys with no `EncryptedSharedPreferences`,
> Keystore-wrapped key or Tink layer anywhere. Encrypting session storage is an open **Track C**
> security-gate item and must land before a release build ships.

### 4. UI Layer (`:ui`)
- **Purpose**: Compose screens and ViewModels
- **Components**:
  - **BaseViewModel**: Provides isOnline, error, uiState StateFlow
  - **Screen Composables**: grouped by feature package (`ui/booking/`, `ui/chat/`, …) alongside the
    ViewModel that drives them — there is no `ui/screens/` directory
  - **Components**: Reusable UI elements (`ui/components/`)
  - **Navigation**: `DiamondsNavHost` / `AppShell` in `ui/navigation/` and `ui/shell/`
  - **Theme**: Material 3 styling

### 5. App Module (`:app`)
- **Purpose**: Application entry point
- **Contains**:
  - MainActivity — hosts the `:ui` nav graph (`DiamondsNavHost`)
  - DiamondsApplication (@HiltAndroidApp) — schedules the WorkManager jobs
  - Hilt DI modules (`app/di/Modules.kt`)
  - FCM service
  - All 23 instrumentation-test files (`app/src/androidTest`)

## Offline-First Architecture

### Read Operations
- **Always attempt local cache first**
- Show cached data immediately (even if stale)
- Mark as stale if > 1 hour old with "Last updated X min ago"
- Background fetch from server when online, update cache
- No blocking UI for network calls

### Write Operations
- **Require ONLINE state**
- Disable action buttons when offline
- Show tooltip "Requires internet connection"
- Send immediately to backend when online
- Never optimistically update cache for writes
- **Offline writes fail** with an offline error — `OfflineException` in most repos (a generic `Exception` in `SubscriptionRepository` and `MessageRepository.getOrCreateConversation`); they are *not* queued for later (the queue below is built but not wired into the write repos)

### Sync Queue (built, not currently wired)
The queue infrastructure exists end-to-end but **`queueOperation` has zero production callers**, so
the queue is never populated (see `TECH_LEAD_REVIEW.md` §4):
- A `SyncQueueEntity` Room table + `SyncManager` track operations by id, type, payload, status, retryCount, timestamps
- `SyncManager` implements retry-with-backoff (1 → 2 → 4 … minutes, capped at 60, max 5 attempts) and conflict resolution
- `SyncWorker.schedulePeriodic` *is* wired in `DiamondsApplication`, so a worker does run every 15
  minutes — over an always-empty table. `SyncWorker.enqueueImmediate` (the "sync on connectivity
  restored" hook) has no callers at all.
- The Sync Status screen can show / retry / cancel operations — functional once writes are wired to enqueue them

## Repository Pattern

All repositories:
1. Check connectivity for write ops
2. Hit local cache first for read ops
3. Map between domain models ↔ DTOs ↔ entities
4. Return Result<T> (Success/Error/Loading)

Example pattern:
```kotlin
// Write operation (requires online)
override suspend fun createBooking(booking: Booking): Result<Booking> {
    if (!connectivityObserver.isOnline()) {
        return Result.Error(OfflineException(...))
    }
    return try {
        val result = backendService.createBooking(...)
        // Only update cache after server confirms
        bookingDao.upsert(...)
        Result.Success(...)
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// Read operation (offline-capable)
override suspend fun getBooking(bookingId: String): Result<Booking> {
    val cached = bookingDao.getById(bookingId)
    if (cached != null) {
        return Result.Success(cached.toDomain())
    }
    // Not cached, fetch if online
    if (!connectivityObserver.isOnline()) {
        return Result.Error(OfflineException(...))
    }
    return try {
        val result = backendService.getBooking(...)
        bookingDao.upsert(...)
        Result.Success(...)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

## ViewModel Pattern

All ViewModels extend `BaseViewModel<UiState>`:
```kotlin
class BookingListViewModel(
    private val bookingRepository: IBookingRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<BookingListUiState>(connectivityObserver, initialState) {
    
    val uiState: StateFlow<BookingListUiState>      // Main state
    val error: StateFlow<String?>                   // Error messages
    val isOnline: StateFlow<Boolean>                // Connectivity status
    val pendingOperationCount: StateFlow<Int>       // Sync queue count
    
    fun loadBookings() {
        // ViewModel calls repository, observes state, updates UI
    }
}
```

## Testing Strategy

### Unit Tests (`:data/src/test` and `:ui/src/test`)
26 test files / 300 `@Test` methods today. `:core`, `:common` and `:app` have no `src/test`
directory, so the root `allUnitTests` aggregate really only executes `:data` and `:ui`.
- **Repository Tests**: Mock IBackendService, verify offline behavior
- **ViewModel Tests**: Mock repositories, verify state transitions
- **Sync Tests**: Verify retry logic, backoff calculations

Example:
```kotlin
@Test
fun testCreateBookingOfflineReturnsError() {
    // Arrange
    every { connectivityObserver.isOnline() } returns false
    
    // Act
    val result = repository.createBooking(mockBooking)
    
    // Assert
    assert(result is Result.Error)
    assert(result.exception is OfflineException)
}
```

### Integration Tests (`app/src/androidTest`)
Instrumentation tests live **only** in `:app` (23 files) and need an emulator
(`./gradlew :app:connectedDebugAndroidTest`). CI does not compile or run them today, so regressions
here stay invisible until someone runs them locally.
- Compose UI tests for critical flows
- Verify offline button states
- Verify error message display

## Dependency Management

**Strict module dependencies** (enforced, verified acyclic):
```
:app → :ui, :data, :core, :common
:ui → :core, :data, :common
:data → :core, :common
:common → :core
:core → (nothing — pure java-library, ZERO Android imports)
```

The arrow reads "depends on". `:common` depends on `:core`, **never** the other way round: `:core`
is the bottom of the graph and imports nothing but the Kotlin stdlib. Each upper module declares the
lower modules it uses *directly* rather than leaning on transitive access.

## Backend Flexibility

Backend service abstraction via `IBackendService`:
- **Stub**: `BackendServiceStub` — what debug builds actually run (`USE_MOCK_BACKEND = true`)
- **Firebase**: `FirebaseBackendService` exists but is **not release-ready** (see below)
- **REST**: a `RestBackendService(IBackendService)` could be added the same way
- **Swap easily**: flip the `BuildConfig` flags in `app/build.gradle.kts`; the Hilt bindings in
  `app/di/Modules.kt` pick the implementation

> ⚠️ **Do not flip `USE_MOCK_BACKEND=false` yet.** One blocker remains:
> 1. 14 `FirebaseBackendService` methods still return `Result.Error(Exception("…not yet implemented"))`
>    (`FirebaseBackendService.kt:484-524`).
>
> ✅ Resolved: every DTO in `IBackendService.kt` used to have required (no-default) constructor
> params, so Kotlin generated no no-arg constructor — which Firestore's object mapper needs. All 26
> `toObject()`/`toObjects()` calls threw into a swallowed `Result.Error` while writes succeeded, a
> write-only app. Every DTO parameter now has a default, guarded by `FirestoreDtoContractTest`.
> Because defaults also mean a malformed document deserializes silently into blanks, the
> `*Dto.toDomain()` mappers validate identity fields and enums and raise `MalformedDtoException`
> naming exactly what was wrong.
>
> See `TECH_LEAD_REVIEW.md` and the Track C section of `ROADMAP.md` for the rest of the go-live gate
> (Firestore Security Rules, real `google-services.json`, signing config, applicationId).

DTOs separate from domain models:
- Domain: `Booking`, `Client`, `Provider` (pure business logic)
- DTO: `BookingDto`, `ClientDto`, `ProviderDto` (API contracts)
- Mapper: `toDomain()`, `toEntity()` for conversion

## Next Steps

Phases 1–16 of the 21-phase plan are built (several are structural — the UI and repositories exist
but sit on stub/mock backends). The current source of truth for what to do next is the
🧭 Remediation Roadmap in `ROADMAP.md`, Tracks A–E:

1. **Track A** (docs & correctness) — done bar one decision: wire the sync queue into the write
   repos, or delete it.
2. **Track B** (localisation & accessibility) — externalisation done: 531 strings + 16 plurals across
   5 locales, `stringResource` used in 46 of 83 `:ui` files at 608 call sites, all icon
   `contentDescription`s localised. The remaining gate is a lint rule/baseline to stop new hardcoded
   strings — none exists yet.
3. **Track C** (Firebase go-live gate) — partial. Done: Firestore DTO no-arg constructors + mapper
   validation, `updateProvider`, the signup profile document, server-side FCM token registration.
   Still open: the 14 unimplemented backend methods, Security Rules, real `google-services.json`,
   signing config, non-`com.example.*` applicationId, encrypted session storage, a real PSP.
4. **Track D** (hardening) — partial. `SyncManager` and `LocationRepository` are covered; `SyncWorker`,
   `RecurringBookingWorker`, `PreferencesDataStore` and `ConnectivityObserver` are not. No
   `DispatcherProvider`; Turbine is declared in `:data`/`:ui` but unused;
   `collectAsStateWithLifecycle` is used 0 times against 87 `collectAsState()` sites in 44 files.
5. **Track E** (test-infrastructure & error-handling defects, found 2026-07-27) — CI never builds or
   runs `androidTest`, so instrumentation breakage is invisible: a `LoginScreenTest` assertion is
   already broken by the Track B rename, and `FakeRepositoryModule` is missing
   `ISavedLocationRepository`. Also `SavedLocationRepository` maps `Result.Error → Result.Success`
   (silent write loss), and 37 `as? Result.Success` sites across 10 ViewModels swallow errors.

## Important Notes

- **Server is source of truth**: All business logic validation happens on backend
- **Optimistic updates**: Only for read data; never for writes
- **Automatic retries (built, not wired)**: `SyncManager` supports retry-with-backoff, but write repos don't enqueue operations, so this path is dormant today
- **User control**: the Sync Status screen can retry/cancel queued operations — once writes are wired to enqueue them
- **Offline writes are not queued today**: they fail with an offline error (`OfflineException` in most repos; a generic `Exception` in a couple) rather than persisting to the sync queue (the queue infrastructure exists but is not wired into the write repos — see `TECH_LEAD_REVIEW.md` §4)
- **Battery aware**: WorkManager respects device constraints
- **Session storage is plaintext**: the auth token and profile fields live in an unencrypted
  DataStore — encrypting them is a Track C release gate, not something already handled
- **Debug ≠ release**: debug builds run `MockAuthService` / `BackendServiceStub`. The Firebase path
  is not release-ready (see "Backend Flexibility" above)
