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
- **Read Operations**: Always check local cache first; fetch from server when online
- **Write Operations**: Require online connectivity; sync queue with exponential backoff retry
- **Automatic Sync**: Background sync via WorkManager when connectivity restored
- **User Control**: Users can cancel pending operations anytime

### Data Management
- **Room Database**: Local caching for all entities (Client, Provider, Booking, Review, Payment)
- **SyncQueue**: Tracks pending/failed operations with retry logic
- **DataStore**: Encrypted user session storage
- **Mappers**: Convert between domain models, DTOs, and entities

### Backend Flexibility
- **IBackendService**: Abstract backend interface
- **BackendServiceStub**: Development stub provided
- **Easy Swapping**: Change Firebase/REST implementation via Hilt binding

## Getting Started

### Prerequisites
- Android Studio Iguana or later
- Android SDK 34
- Kotlin 1.9.0

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
- **DataStore**: Encrypted preferences
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

- [ ] Implement Firebase authentication
- [ ] Add Firebase Cloud Messaging for notifications
- [ ] Integrate Google Maps for location services
- [ ] Integrate Stripe payments
- [ ] Build authentication screens (Login, Signup)
- [ ] Build main booking flow (Search, Book, Track)
- [ ] Add provider profile screens
- [ ] Implement review/rating system
- [ ] Setup CI/CD pipeline

## License

TODO: Add license information

## Contributing

Guidelines coming soon.
