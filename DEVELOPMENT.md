# Development Setup Guide

## Prerequisites

- **JDK 17** is required to run the build (AGP 8.5.0). The modules still compile to **JVM 11**
  bytecode (`sourceCompatibility`/`targetCompatibility` = 11, `jvmTarget` = "11").
- Kotlin 1.9.0, Room 2.6.1 — all versions are pinned in `gradle/libs.versions.toml`.
- **No secrets are needed for a debug build.** It runs on mock/stub backends
  (`USE_MOCK_AUTH` / `USE_MOCK_BACKEND` = `true` in debug), the committed `app/google-services.json`
  is a placeholder, and `MAPS_API_KEY` is read from a Gradle property and defaults to empty.

```bash
./gradlew assembleDebug              # build the debug app (compiles every module)
./gradlew allUnitTests               # root aggregate: JVM unit tests for :app/:common/:data/:ui
./gradlew :data:testDebugUnitTest    # a single module's unit tests
./gradlew installDebug               # install on a device/emulator
```

## Project Structure Overview

The Diamonds app is organized into 5 main Gradle modules:

### 1. `:core` Module
**Pure Kotlin domain layer** - No Android framework dependencies

**Location**: `/core/src/main/java/com/example/diamonds/`

**Contains**:
- `domain/model/` - Domain entities (Client, Provider, Booking, etc.)
- `domain/repository/` - Repository interfaces (contracts)
- `domain/model/Result.kt` - Type-safe result wrapper

**Key Points**:
- Zero Android dependencies
- Can be used in pure Kotlin/multiplatform projects
- Update here when business rules change

### 2. `:common` Module
**Shared utilities and extensions** used across data and UI

**Location**: `/common/src/main/java/com/example/diamonds/`

**Contains**:
- `common/util/` - Constants, ConnectivityState enum
- `common/ext/` - Flow extensions, utility functions

**Key Points**:
- Depends on `:core` only — never on `:data` or `:ui`
- Can be imported by `:data`, `:ui` and `:app`
- Reusable helpers and extensions

### 3. `:data` Module
**Data layer** - Room database, sync engine, repositories, networking

**Location**: `/data/src/main/java/com/example/diamonds/`

**Contains**:
- `data/local/` - Room entities, DAOs, AppDatabase (currently schema version **10**, 9 migrations)
- `data/local/preferences/` - **Plaintext** DataStore session storage (see the warning below)
- `data/local/entity/` - Entity definitions matching database schema
- `data/remote/backend/` - Backend service interface and DTOs
- `data/connectivity/` - ConnectivityObserver for network state
- `data/repository/` - Repository implementations (offline-first logic)
- `data/sync/` - SyncManager and sync queue handling
- `data/mapper/` - Mappers: Entity ↔ Domain ↔ DTO
- `data/worker/` - WorkManager tasks (SyncWorker)

**Key Points**:
- All I/O operations happen here
- Repositories check online status before writes
- Read ops fall back to the Room cache when the network is unavailable (the exact cache-first vs
  network-first order varies per repository — read the one you're touching)
- `SyncManager` implements queue + retry logic, but nothing enqueues into it today (see
  "Offline Sync Deep Dive" below)

> ⚠️ **Session storage is plaintext.** `PreferencesDataStore` uses a plain
> `preferencesDataStore("app_preferences")`; the auth token, user id, email, display name and role
> are stored **unencrypted** on disk. There is no `EncryptedSharedPreferences`, Keystore-wrapped key
> or Tink layer anywhere in the repo. Encrypting it is an open **Track C** security-gate item and
> blocks any release build — do not describe this storage as encrypted.

### 4. `:ui` Module
**UI layer** - Jetpack Compose screens and ViewModels

**Location**: `/ui/src/main/java/com/example/diamonds/`

**Contains** — organised by **feature package**, not by layer. There is no `ui/screens/` directory;
each feature package holds its screens and its ViewModel together:
- `ui/auth/`, `ui/booking/`, `ui/chat/`, `ui/cleaner/`, `ui/company/`, `ui/customer/`, `ui/map/`,
  `ui/notification/`, `ui/payment/`, `ui/profile/`, `ui/review/`, `ui/settings/`,
  `ui/subscription/`, `ui/support/`, `ui/sync/` - feature screens + ViewModels
- `ui/navigation/` - `DiamondsNavHost`, `Screen` routes, `BottomTab`
- `ui/shell/` - `AppShell` scaffolding; `ui/splash/` - splash
- `ui/base/` - BaseViewModel with state management
- `ui/components/` - Reusable UI components
- `ui/placeholder/` - `PlaceholderScreen` for routes that aren't built yet
- `ui/theme/` - Material 3 theme configuration
- `ui/src/main/res/values*/strings.xml` - 531 strings + 16 plurals, key-complete across
  `values`, `values-fr`, `values-es`, `values-pt`, `values-ar`

**Key Points**:
- All UI is Jetpack Compose
- ViewModels extend BaseViewModel<UiState> (25 ViewModels today)
- Observes connectivity state via isOnline: StateFlow<Boolean>
- Action buttons disabled when offline
- User-facing text goes through `stringResource(R.string.*)` — don't add new hardcoded literals

### 5. `:app` Module
**Application entry point** - DI setup, navigation, main activity

**Location**: `/app/src/main/java/com/example/diamonds/`

**Contains**:
- `DiamondsApplication.kt` - @HiltAndroidApp entry point
- `MainActivity.kt` - Main activity with Compose
- `di/Modules.kt` - Hilt dependency injection modules
- `AndroidManifest.xml` - Permissions and app configuration

**Key Points**:
- Hilt setup and all DI bindings
- `MainActivity` hosts the `:ui` nav graph (`DiamondsNavHost`); the graph itself lives in
  `ui/navigation/`
- FCM service and WorkManager scheduling live here
- No business logic here
- Only wiring everything together
- `app/src/androidTest/` holds all 23 instrumentation-test files

## Module Dependencies

**Strict dependency hierarchy** (enforced, verified acyclic). Each line reads
"module → what it depends on":

```
:core    → nothing (pure java-library, zero Android imports)
:common  → :core
:data    → :core, :common
:ui      → :core, :data, :common
:app     → :ui, :data, :core, :common
```

`:core` sits at the bottom and must never import `:common`, `:data`, `:ui` or anything Android.
Upper modules declare the lower modules they use *directly* rather than relying on transitive
access, and prefer depending on `:core` interfaces over `:data` implementations.

## Data Flow Examples

### Example 1: Reading a Booking (Offline-Capable)

```
User opens booking screen
    ↓
BookingListViewModel calls bookingRepository.getClientBookings(clientId)
    ↓
BookingRepository.getClientBookings():   // network-first when online
    1. Check connectivityObserver.isOnline()
    2. If online  → fetch from backendService, upsert into Room, return Result.Success
    3. If that network call throws → fall back to the cached rows
                                     (Result.Error only when the cache is also empty)
    4. If offline → return the cached rows, or Result.Error(OfflineException) if none
    ↓
ViewModel collects result and updates uiState
    ↓
Compose UI renders the state
```

Not every read is shaped this way. `BookingRepository.getBooking(id)` is **cache-first** (returns the
cached row immediately and only hits the backend on a miss), and `MessageRepository.getOrCreateConversation`
checks the cache first too. Read the repository you're changing rather than assuming a single shape;
the invariant that always holds is that reads degrade to the Room cache, and writes never do.

### Example 2: Creating a Booking (Online-Required)

```
User taps "Book Now"
    ↓
BookingViewModel.createBooking(booking) called
    ↓
BookingRepository.createBooking():
    1. Check if online via connectivityObserver.isOnline()
    2. If offline → return Result.Error(OfflineException)
    3. If online → send to backendService immediately
    4. Backend validates and returns result
    5. If success → update local cache
    6. Return Result.Success or Result.Error
    ↓
ViewModel observes result
    ↓
If offline: ViewModel sets error state, button disabled
If online: ViewModel shows loading, disables button, awaits response
If success: ViewModel shows success, navigates away
If error: ViewModel shows error message, button re-enabled
```

## Adding New Features

### Step 1: Define Domain Models
**File**: `:core/src/main/java/com/example/diamonds/domain/model/`

```kotlin
data class YourEntity(
    val id: String,
    val field1: String,
    val field2: Int,
    val syncStatus: SyncStatus = SyncStatus.READ_ONLY,
    val createdAt: String,
    val updatedAt: String
)
```

### Step 2: Define Repository Interface
**File**: `:core/src/main/java/com/example/diamonds/domain/repository/Repositories.kt`

```kotlin
interface IYourRepository {
    suspend fun getYour Entity(id: String): Result<YourEntity>
    suspend fun createYourEntity(entity: YourEntity): Result<YourEntity>
    // Other methods...
}
```

### Step 3: Create Room Entity
**File**: `:data/src/main/java/com/example/diamonds/data/local/entity/`

```kotlin
@Entity(tableName = "your_entities")
data class YourEntityEntity(
    @PrimaryKey val id: String,
    val field1: String,
    val field2: Int,
    val syncStatus: String,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)
```

### Step 4: Create DAO
**File**: `:data/src/main/java/com/example/diamonds/data/local/dao/Daos.kt`

```kotlin
@Dao
interface YourEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: YourEntityEntity)

    @Query("SELECT * FROM your_entities WHERE id = :id")
    suspend fun getById(id: String): YourEntityEntity?

    // Other queries...
}
```

### Step 5: Add DTOs
**File**: `:data/src/main/java/com/example/diamonds/data/remote/backend/IBackendService.kt`

```kotlin
data class YourEntityDto(
    val id: String,
    val field1: String,
    val field2: Int,
    val createdAt: String,
    val updatedAt: String
)
```

### Step 6: Create Mappers
**File**: `:data/src/main/java/com/example/diamonds/data/mapper/Mappers.kt`

```kotlin
fun YourEntityEntity.toDomain(): YourEntity = YourEntity(...)
fun YourEntity.toEntity(): YourEntityEntity = YourEntityEntity(...)
fun YourEntityDto.toDomain(): YourEntity = YourEntity(...)
```

### Step 7: Implement Repository
**File**: `:data/src/main/java/com/example/diamonds/data/repository/YourRepository.kt`

```kotlin
class YourRepository(
    private val db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IYourRepository {
    // Implement interface following offline-first pattern
}
```

### Step 8: Add Hilt Binding
**File**: `:app/src/main/java/com/example/diamonds/di/Modules.kt`

```kotlin
@Singleton
@Provides
fun provideYourRepository(
    db: AppDatabase,
    backendService: IBackendService,
    connectivityObserver: ConnectivityObserver
): IYourRepository {
    return YourRepository(db, backendService, connectivityObserver)
}
```

### Step 9: Create ViewModel
**File**: `:ui/src/main/java/com/example/diamonds/ui/<feature>/YourViewModel.kt`
(pick the existing feature package — `booking/`, `chat/`, `profile/`, … — or add a new one; there is
no `ui/screens/` directory)

```kotlin
data class YourUiState(
    val items: List<YourEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class YourViewModel @Inject constructor(
    private val yourRepository: IYourRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<YourUiState>(connectivityObserver, YourUiState()) {
    
    fun loadItems() {
        viewModelScope.launch {
            val result = yourRepository.getYourEntity("id")
            updateState { it.copy(isLoading = false) }
        }
    }
}
```

### Step 10: Create Compose Screen
**File**: `:ui/src/main/java/com/example/diamonds/ui/<feature>/YourScreen.kt`

```kotlin
@Composable
fun YourScreen(viewModel: YourViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    
    Column {
        if (!isOnline) {
            OfflineBanner()
        }
        Text(stringResource(R.string.your_screen_title))   // never a bare literal
    }
}
```

Two conventions to follow here:
- **Strings**: add the key to `ui/src/main/res/values/strings.xml` *and* to `values-fr`, `values-es`,
  `values-pt`, `values-ar` — all five locales are currently key-complete and should stay that way.
  Icons need a localised `contentDescription` (the `cd_*` keys).
- **State collection**: `collectAsStateWithLifecycle` is the target pattern shown above, but the
  existing code has not migrated — there are 87 `collectAsState()` call sites across 44 files and
  zero uses of `collectAsStateWithLifecycle`. Adding the lifecycle-aware variant in new code is fine;
  the bulk migration is an open Track D item.

### Step 11: Add Unit Tests
**File**: `:data/src/test/java/com/example/diamonds/data/repository/YourRepositoryTest.kt`

```kotlin
class YourRepositoryTest {
    @Test
    fun testCreateYourEntityOfflineReturnsError() {
        // Test that write ops return OfflineException when offline
    }
    
    @Test
    fun testGetYourEntityReturnsCachedData() {
        // Test that read ops return cached data immediately
    }
}
```

## Offline Sync Deep Dive

### What actually happens today

There is **no offline write queue in operation**. `SyncManager.queueOperation` has zero production
callers — only the interface declaration, the implementation itself, `SyncManagerTest`, and the
androidTest fake reference it. Nothing in any repository enqueues, so the `sync_queue` table is
always empty at runtime. Describe the behaviour below, not the retry story that used to live here.

1. **Write attempted while offline**
   - The repository checks `connectivityObserver.isOnline()` *before* doing anything
   - It returns immediately with `Result.Error(OfflineException(...))` — most repos; a few still
     return a generic `Exception("No internet connection")` (`SubscriptionRepository`,
     `MessageRepository.getOrCreateConversation`)
   - Nothing is persisted, nothing is retried, the cache is not optimistically updated
   - The ViewModel surfaces the error and the action button stays disabled

2. **Write attempted while online but the backend fails**
   - The backend error is propagated as `Result.Error` to the caller. It is **not** queued.

3. **Read attempted while offline**
   - The repository serves the Room cache; if the cache is empty it returns
     `Result.Error(OfflineException(...))`. This part is genuinely implemented.

4. **The periodic worker still runs**
   - `DiamondsApplication` calls `SyncWorker.schedulePeriodic(this)`, so WorkManager wakes a
     `SyncWorker` every 15 minutes (`Constants.DEFAULT_SYNC_INTERVAL_MINUTES`) under a
     `NetworkType.CONNECTED` constraint and calls `syncManager.processSyncQueue()` — over an empty
     table. `SyncWorker.enqueueImmediate` (the "sync on connectivity restored" hook) has **no
     callers**.

### The queue infrastructure that exists but is unwired

All of this is implemented and unit-tested (`SyncManagerTest`, 21 tests) — it simply has no input:

- `SyncQueueEntity` Room table tracking id, type, payload, status, `retryCount`, timestamps
- `SyncManager.queueOperation` / `processSyncQueue` / `cancelSyncOperation` / `retryOperation`
- Exponential backoff in `SyncManager.calculateBackoff`: `INITIAL_BACKOFF_MINUTES * 2^retryCount`,
  i.e. 1 → 2 → 4 → 8 … minutes, capped at `MAX_BACKOFF_MINUTES` (60), giving up after
  `MAX_RETRY_ATTEMPTS` (5)
- The Sync Status screen (`ui/sync/`), which can list, retry and cancel operations

```kotlin
syncManager.cancelSyncOperation(operationId)   // works — but the queue is never populated
```

### Open decision

Track A's one remaining item is **wire the sync queue into the write repos, or delete it**. Until
that is decided, do not write code (or docs) that assumes a failed write will be retried later, and
do not add a "queued for sync" affordance to the UI. See `TECH_LEAD_REVIEW.md` §4 and
`ROADMAP.md` Track A.

## Testing Strategy

### Unit Testing Repositories

```kotlin
@Test
fun testOfflineBehavior() {
    // Mock connectivity
    every { connectivityObserver.isOnline() } returns false
    
    // Attempt write op
    val result = repository.createBooking(booking)
    
    // Should return OfflineException
    assert(result is Result.Error)
    assert(result.exception is OfflineException)
}

@Test
fun testReadCacheFirst() {
    // Mock cached data
    coEvery { dao.getById("id") } returns cachedEntity
    
    // Should return cached immediately
    val result = repository.getBooking("id")
    
    // Should not call backend
    verify(exactly = 0) { backendService.getBooking("id") }
}
```

### Mocking Backend Service

```kotlin
val mockBackendService = mockk<IBackendService> {
    coEvery { createBooking(any()) } returns Result.Success(bookingDto)
    coEvery { getBooking("id") } returns Result.Error(Exception("Not found"))
}
```

## Common Development Tasks

### Run all unit tests
```bash
./gradlew allUnitTests
```
Root aggregate task. It depends on `:app`, `:common`, `:data` and `:ui` `testDebugUnitTest` — but
only `:data` and `:ui` have a `src/test` directory, and `:core` is not in the list at all.

### Run one module's tests
```bash
./gradlew :data:testDebugUnitTest
```

### Run specific test
```bash
./gradlew :data:testDebugUnitTest --tests "*BookingRepositoryTest"
```

### Build the debug app
```bash
./gradlew assembleDebug
```

### Build specific module
```bash
./gradlew :data:build
```

### Clean rebuild
```bash
./gradlew clean assembleDebug
```

### Check code quality
```bash
./gradlew lint
```
Note: lint is **not** run by CI, and there is no lint baseline or custom rule (including none for
hardcoded strings) anywhere in the repo.

### Run instrumentation tests (needs an emulator/device)
```bash
./gradlew :app:connectedDebugAndroidTest
```
CI does not compile or run `androidTest`, so breakages here are only caught locally.

### View module dependencies
```bash
./gradlew :app:dependencies
```

## CI

`.github/workflows/ci.yml` runs `./gradlew assembleDebug allUnitTests` on JDK 17, on every pull
request to `develop` and on pushes to `develop`. Treat a red run as blocking. CI does **not** run
lint, instrumentation tests, or any coverage gate.

## Troubleshooting

### Import not found
- Check module is added to settings.gradle.kts
- Check dependency is in build.gradle.kts
- Clean and rebuild

### Hilt compilation error
- Ensure class has @HiltViewModel or is provided in module
- Check all dependencies are provided in DI modules
- Run `./gradlew clean build`

### Room migration issue
- Increment database version in @Database annotation (currently **10**)
- Add a `MIGRATION_n_n+1` alongside the existing 9 (`MIGRATION_1_2` … `MIGRATION_9_10`) — don't reach
  for `fallbackToDestructiveMigration`
- ⚠️ `AppDatabase` sets `exportSchema = true`, but no `room.schemaLocation` annotation-processor
  argument is configured and there is no `schemas/` directory in the repo. Nothing is exported, so
  none of the 9 migrations is verified by a `MigrationTestHelper` test and the declared
  `room-testing` dependency is currently dead weight. Configuring the schema location is a
  prerequisite for testing any migration you add.

### ConnectivityObserver not updating
- Verify internet permission in AndroidManifest.xml
- Check ConnectivityManager is initialized
- Test on real device (emulator may have issues)

## Resources

- [ARCHITECTURE.md](ARCHITECTURE.md) - High-level architecture
- [TECH_LEAD_REVIEW.md](TECH_LEAD_REVIEW.md) - Current source of truth for known gaps
- [ROADMAP.md](ROADMAP.md) - 21-phase plan + the Track A–E remediation roadmap
- [Android Architecture Components](https://developer.android.com/guide/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
