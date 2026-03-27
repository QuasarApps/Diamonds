# Development Setup Guide

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
- No domain or data layer dependencies
- Can be imported by any module
- Reusable helpers and extensions

### 3. `:data` Module
**Data layer** - Room database, sync engine, repositories, networking

**Location**: `/data/src/main/java/com/example/diamonds/`

**Contains**:
- `data/local/` - Room entities, DAOs, AppDatabase
- `data/local/preferences/` - DataStore for encrypted session storage
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
- Read ops try cache first, then fetch if online
- SyncManager handles queued operations with retry logic

### 4. `:ui` Module
**UI layer** - Jetpack Compose screens and ViewModels

**Location**: `/ui/src/main/java/com/example/diamonds/`

**Contains**:
- `ui/base/` - BaseViewModel with state management
- `ui/screens/` - Screen composables (Booking, Profile, Search, etc.)
- `ui/components/` - Reusable UI components
- `ui/theme/` - Material 3 theme configuration

**Key Points**:
- All UI is Jetpack Compose
- ViewModels extend BaseViewModel<UiState>
- Observes connectivity state via isOnline: StateFlow<Boolean>
- Action buttons disabled when offline

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
- Navigation configuration (future)
- No business logic here
- Only wiring everything together

## Module Dependencies

**Strict dependency hierarchy** (enforced):

```
:core       ← Nothing depends on core
:common     ← Only :core imports common
:data       ← Only :core and :common
:ui         ← Only :core, :data, :common
:app        ← Everything else
```

## Data Flow Examples

### Example 1: Reading a Booking (Offline-Capable)

```
User opens booking screen
    ↓
BookingListViewModel calls bookingRepository.getClientBookings(clientId)
    ↓
BookingRepository.getClientBookings():
    1. Check local Room database for cached bookings
    2. If cache exists → return immediately Result.Success
    3. If cache empty → check if online
    4. If offline → return Result.Error(OfflineException)
    5. If online → fetch from backendService
    6. Update local database
    7. Return Result.Success
    ↓
ViewModel collects result and updates uiState
    ↓
Compose UI renders the state
```

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
**File**: `:ui/src/main/java/com/example/diamonds/ui/screens/YourScreen.kt`

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
**File**: `:ui/src/main/java/com/example/diamonds/ui/screens/YourScreen.kt`

```kotlin
@Composable
fun YourScreen(viewModel: YourViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    
    Column {
        if (!isOnline) {
            OfflineBanner()
        }
        // Your UI here
    }
}
```

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

### How Sync Queue Works

1. **Write Operation Fails (Network Error)**
   - Operation stored in `SyncQueue` with status=PENDING
   - User sees error "Retrying..."

2. **Background Sync Triggered**
   - WorkManager runs `SyncWorker` every 15 minutes
   - OR triggered immediately on connectivity change
   - SyncWorker calls `syncManager.processSyncQueue()`

3. **Retry with Backoff**
   - Retry 1: 1 minute wait
   - Retry 2: 2 minutes wait
   - Retry 3: 4 minutes wait
   - Max: 60 minutes between retries

4. **Success or Final Failure**
   - If success: mark SYNCED, remove from queue, update cache
   - If still failing: mark FAILED, stay in queue for manual retry
   - User sees "1 failed operation (tap to retry)"

### User Can Cancel Anytime
```kotlin
syncManager.cancelSyncOperation(operationId)
// Operation removed immediately, no further retries
```

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

### Run specific test
```bash
./gradlew :data:test --tests "BookingRepositoryTest"
```

### Build specific module
```bash
./gradlew :data:build
```

### Clean rebuild
```bash
./gradlew clean build
```

### Check code quality
```bash
./gradlew lint
```

### View module dependencies
```bash
./gradlew :app:dependencies
```

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
- Increment database version in @Database annotation
- Add migration or use fallbackToDestructiveMigration

### ConnectivityObserver not updating
- Verify internet permission in AndroidManifest.xml
- Check ConnectivityManager is initialized
- Test on real device (emulator may have issues)

## Resources

- [ARCHITECTURE.md](../ARCHITECTURE.md) - High-level architecture
- [Android Architecture Components](https://developer.android.com/guide/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
