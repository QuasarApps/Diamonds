# Diamonds App Architecture Documentation

## Overview

Diamonds is built using a modular, offline-first Android architecture with the following key principles:

1. **Modular Structure**: Separate Gradle modules for independent compilation and testing
2. **Offline-First**: Read operations work offline; write operations require connectivity
3. **Server as Source of Truth**: Backend validates all business logic
4. **Reactive State Management**: MVVM + StateFlow for predictable data flows
5. **Dependency Injection**: Hilt for loose coupling and testability

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
│   └── ui/
│       ├── screens/       # Screen composables
│       ├── components/    # Reusable UI components
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
Repository checks local cache (Room)
    ↓
If cache available → return immediately
If cache empty → (if online) fetch from backend → update cache → return
If offline and no cache → return error
```

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
  - **PreferencesDataStore**: Encrypted user session storage
  - **IBackendService**: Abstract backend interface (stub provided)
  - **Repositories**: Implement read/write logic with offline checks
  - **SyncManager**: Handles queued operations with exponential backoff
  - **Mappers**: Convert between domain models, DTOs, and entities

### 4. UI Layer (`:ui`)
- **Purpose**: Compose screens and ViewModels
- **Components**:
  - **BaseViewModel**: Provides isOnline, error, uiState StateFlow
  - **Screen Composables**: Individual screens (Booking, Profile, etc.)
  - **Components**: Reusable UI elements
  - **Theme**: Material 3 styling

### 5. App Module (`:app`)
- **Purpose**: Application entry point
- **Contains**:
  - MainActivity
  - DiamondsApplication (@HiltAndroidApp)
  - Hilt DI modules
  - Navigation setup (future)

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
- Queue and retry failed operations with backoff (1min → 2min → 4min → ... → 60min max)
- User can cancel pending operations

### Sync Queue
- Stores failed/pending operations in Room
- Each operation tracked by: id, type, payload, status, retryCount, timestamps
- Automatic retry on network restoration
- User can manually retry or cancel operations
- Shows pending operation count in UI ("2 pending")

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

### Unit Tests (`:data/test` and `:ui/test`)
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

### Integration Tests (`:ui/androidTest`)
- Compose UI tests for critical flows
- Verify offline button states
- Verify error message display

## Dependency Management

**Strict module dependencies** (enforce):
```
:app → :ui, :data, :core, :common
:ui → :core, :data, :common
:data → :core, :common
:common → (nothing)
:core → (nothing)
```

## Backend Flexibility

Backend service abstraction via `IBackendService`:
- **Stub**: `BackendServiceStub` provided for development
- **Firebase**: Create `FirebaseBackendService(IBackendService)` later
- **REST**: Create `RestBackendService(IBackendService)` later
- **Swap easily**: Change binding in Hilt `BackendModule`

DTOs separate from domain models:
- Domain: `Booking`, `Client`, `Provider` (pure business logic)
- DTO: `BookingDto`, `ClientDto`, `ProviderDto` (API contracts)
- Mapper: `toDomain()`, `toEntity()` for conversion

## Next Steps

1. **Phase 11**: Build in-app chat system (MessageRepository, ChatScreen, real-time Firestore
   listener)
2. **Phase 12**: Implement subscription and recurring bookings (RecurringBookingWorker,
   SubscriptionRepository)
3. **Phase 13**: Add multi-language support (strings.xml extraction, runtime locale switching, RTL)
4. **Phase 14**: Add reverse reviews so cleaners can rate clients and locations
5. **Phase 15**: Expand cleaning types, location types, and cleaner specializations
6. **Phase 16**: Add error logging, Firebase Analytics, and crash reporting
7. **Phase 17**: Achieve 60%+ test coverage and performance optimization
8. **Phase 18**: Set up CI/CD and prepare for app store release

## Important Notes

- **Server is source of truth**: All business logic validation happens on backend
- **Optimistic updates**: Only for read data; never for writes
- **Automatic retries**: Failed operations retry with exponential backoff
- **User control**: Users can cancel pending operations anytime
- **No data loss**: All operations persisted in sync queue until confirmed
- **Battery aware**: WorkManager respects device constraints
