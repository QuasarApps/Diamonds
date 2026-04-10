# Project File Structure

## Complete Directory Tree

```
Diamonds/
│
├── README.md                           # Project overview and quick start
├── ARCHITECTURE.md                     # High-level architecture (285 lines)
├── DEVELOPMENT.md                      # Development setup guide (400+ lines)
├── ROADMAP.md                          # 20-phase implementation roadmap
├── IMPLEMENTATION_SUMMARY.md           # What's implemented and what's next
├── FILE_STRUCTURE.md                   # This file
│
├── build.gradle.kts                    # Root build config with Hilt plugin
├── settings.gradle.kts                 # Module includes
│
├── gradle/
│   └── libs.versions.toml              # Centralized dependency versions
│
├── gradlew                             # Unix gradle wrapper
├── gradlew.bat                         # Windows gradle wrapper
│
├── local.properties                    # Local SDK/NDK paths
│
│
├── core/                               # ✅ DOMAIN LAYER (Pure Kotlin)
│   ├── build.gradle.kts
│   └── src/main/java/com/example/diamonds/domain/
│       ├── model/
│       │   ├── DomainModels.kt         # Client, Provider, Booking, Review, Payment
│       │   ├── Result.kt               # Result<T> type-safe wrapper
│       │   └── Enums.kt                # Status enums (Booking, Payment, Sync, etc.)
│       └── repository/
│           └── Repositories.kt         # 7 repository interfaces (contracts)
│
│
├── common/                             # ✅ SHARED UTILITIES
│   ├── build.gradle.kts
│   └── src/main/java/com/example/diamonds/common/
│       ├── util/
│       │   └── Constants.kt            # App constants, ConnectivityState enum
│       └── ext/
│           └── FlowExt.kt              # Flow extensions and helpers
│
│
├── data/                               # ✅ DATA LAYER
│   ├── build.gradle.kts
│   └── src/main/java/com/example/diamonds/data/
│       │
│       ├── local/
│       │   ├── AppDatabase.kt          # Room database singleton
│       │   ├── entity/
│       │   │   └── Entities.kt         # 7 Room entities with @Entity
│       │   ├── dao/
│       │   │   └── Daos.kt             # 7 DAOs with queries
│       │   └── preferences/
│       │       └── PreferencesDataStore.kt  # Encrypted session storage
│       │
│       ├── remote/backend/
│       │   ├── IBackendService.kt      # Backend service interface (abstract)
│       │   ├── BackendServiceStub.kt   # Development stub implementation
│       │   └── DTOs                    # API contracts (separate from domain)
│       │
│       ├── connectivity/
│       │   └── ConnectivityObserver.kt # Network state observation
│       │
│       ├── repository/
│       │   ├── AuthRepository.kt       # ✅ Auth with token management
│       │   ├── ClientRepository.kt     # ✅ Client data (offline-read)
│       │   ├── ProviderRepository.kt   # ✅ Provider search (offline-read)
│       │   ├── ServiceRepository.kt    # ✅ Service listings (offline-read)
│       │   ├── BookingRepository.kt    # ✅ Booking CRUD (offline-read, online-write)
│       │   ├── ReviewRepository.kt     # ✅ Reviews (offline-read, online-write)
│       │   ├── PaymentRepository.kt    # ✅ Payments (offline-read, online-write)
│       │   ├── NotificationRepository.kt # ✅ Phase 8: Notifications
│       │   ├── LocationRepository.kt   # ✅ Phase 9: Location & tracking
│       │   └── SubscriptionRepository.kt # Phase 12: Recurring bookings (planned)
│       │
│       ├── mapper/
│       │   └── Mappers.kt              # Entity ↔ Domain ↔ DTO conversions
│       │
│       ├── sync/
│       │   └── SyncManager.kt          # Sync queue, exponential backoff, retry
│       │
│       └── worker/
│           └── SyncWorker.kt           # WorkManager background sync task
│
│   ├── src/test/java/com/example/diamonds/data/
│       └── repository/
│           └── BookingRepositoryTest.kt  # Repository test template
│
│
├── ui/                                 # ✅ UI LAYER
│   ├── build.gradle.kts
│   └── src/main/java/com/example/diamonds/ui/
│       │
│       ├── base/
│       │   └── BaseViewModel.kt        # Base VM with state management
│       │
│       ├── theme/
│       │   └── Theme.kt                # Material 3 theme
│       │
│       ├── navigation/
│       │   ├── Screen.kt               # Sealed class of all routes
│       │   ├── DiamondsNavHost.kt      # Root NavHost (splash → auth → shell)
│       │   └── BottomTab.kt            # Tab definitions per role
│       │
│       ├── shell/
│       │   ├── AppShell.kt             # Inner NavHost with bottom tabs, deep links, transitions
│       │   ├── AppShellViewModel.kt    # Session state for the shell
│       │   └── TabScreens.kt           # CustomerHomeTab, CleanerDashboardTab
│       │
│       ├── splash/
│       │   ├── SplashScreen.kt         # Animated splash screen
│       │   └── SplashViewModel.kt      # Auth check → route
│       │
│       ├── auth/
│       │   ├── AuthViewModel.kt        # Login/Signup state
│       │   ├── LoginScreen.kt          # Login with demo accounts
│       │   └── SignupScreen.kt         # Signup with role selection
│       │
│       ├── booking/
│       │   ├── BookingViewModel.kt     # Search, form, list, detail state
│       │   ├── BookingsListScreen.kt   # Booking list + detail screen
│       │   ├── BookingFormScreen.kt    # Create booking form
│       │   ├── BookingConfirmationScreen.kt # Post-booking confirmation
│       │   ├── ProviderSearchScreen.kt # Provider search with filter sheet
│       │   ├── ProviderRatingsScreen.kt # Provider ratings display
│       │   ├── ReviewScreen.kt         # Leave a review
│       │   ├── ReviewViewModel.kt      # Review state management
│       │   └── ServiceListScreen.kt    # Services for a provider
│       │
│       ├── cleaner/
│       │   ├── CleanerViewModel.kt     # Dashboard state
│       │   ├── CleanerProfileViewModel.kt # Profile + service management
│       │   ├── CleanerBookingRequestsScreen.kt
│       │   ├── CleanerEarningsScreen.kt
│       │   ├── CleanerEarningsViewModel.kt
│       │   ├── CleanerProfileScreen.kt
│       │   ├── CleanerScheduleScreen.kt
│       │   └── ServiceManagementScreen.kt
│       │
│       ├── company/
│       │   ├── CompanyViewModel.kt     # Company aggregate state
│       │   ├── CompanyEarningsViewModel.kt
│       │   ├── CompanyBookingsScreen.kt
│       │   ├── CompanyDashboardScreen.kt
│       │   ├── CompanyEarningsScreen.kt
│       │   └── CompanyTeamScreen.kt
│       │
│       ├── customer/
│       │   └── CustomerProfileScreen.kt
│       │
│       ├── payment/
│       │   ├── PaymentViewModel.kt     # Card validation, payment state
│       │   ├── PaymentScreen.kt        # Payment form with card visual
│       │   ├── PaymentSuccessScreen.kt # Receipt-style success
│       │   └── PaymentHistoryScreen.kt # Transaction history
│       │
│       ├── components/                 # ✅ Phase 7: Reusable UI components
│       │   ├── ConfirmationDialog.kt   # Reusable Material 3 confirmation dialog
│       │   ├── ErrorScreens.kt         # GenericError, NoInternet, NotFound, OfflineBanner
│       │   ├── QuickBookingSheet.kt    # Quick-book bottom sheet
│       │   └── FilterSheet.kt          # Advanced filter bottom sheet
│       │
│       └── placeholder/
│           └── PlaceholderScreen.kt
│
│   ├── src/test/java/com/example/diamonds/ui/
│       └── base/
│           └── BaseViewModelTest.kt    # ViewModel test template
│
│   └── src/androidTest/java/com/example/diamonds/ui/
│       └── [TODO: Compose UI tests]
│
│
├── app/                                # ✅ APPLICATION ENTRY POINT
│   ├── build.gradle.kts
│   │
│   ├── src/main/java/com/example/diamonds/
│   │   ├── DiamondsApplication.kt      # @HiltAndroidApp entry point
│   │   ├── MainActivity.kt              # Main activity with Compose
│   │   └── di/
│   │       └── Modules.kt               # Hilt DI: DataModule, RepositoryModule
│   │
│   ├── src/main/AndroidManifest.xml    # Permissions, app config
│   │
│   ├── src/main/res/
│   │   ├── drawable/
│   │   ├── mipmap-*/
│   │   ├── values/
│   │   │   └── strings.xml
│   │   ├── values-night/
│   │   └── xml/
│   │       ├── data_extraction_rules.xml
│   │       └── backup_rules.xml
│   │
│   ├── src/test/java/com/example/diamonds/
│   │   └── [TODO: App-level tests]
│   │
│   └── src/androidTest/java/com/example/diamonds/
│       └── [TODO: E2E tests]
│
├── proguard-rules.pro                  # ProGuard config for release builds
│
└── gradle/wrapper/
    ├── gradle-wrapper.jar
    └── gradle-wrapper.properties

```

## Module Breakdown

### `:core` Module
**Size**: ~200 lines of production code
**Dependencies**: None (pure Kotlin stdlib)
**Purpose**: Define business domain contracts

**Key Files**:
- `domain/model/DomainModels.kt` - All domain entities
- `domain/model/Result.kt` - Type-safe error handling
- `domain/repository/Repositories.kt` - 7 repository contracts

**Files to Add**:
- None immediately - domain is stable

### `:common` Module  
**Size**: ~50 lines
**Dependencies**: Only Kotlin stdlib + Compose/DataStore for UI utilities
**Purpose**: Shared utilities across modules

**Key Files**:
- `common/util/Constants.kt` - App constants
- `common/ext/FlowExt.kt` - Flow helpers

**Files to Add**:
- More extensions as needed (DateExt, StringExt, etc.)

### `:data` Module
**Size**: ~1500 lines
**Dependencies**: `:core`, `:common`, Room, Coroutines, Serialization
**Purpose**: All data access and business logic coordination

**Key Files**:
- `local/AppDatabase.kt` - Room setup
- `local/entity/Entities.kt` - 7 entities
- `local/dao/Daos.kt` - 7 DAOs
- `remote/backend/IBackendService.kt` - Backend contract
- `repository/*.kt` - 7 repository implementations
- `sync/SyncManager.kt` - Sync orchestration
- `mapper/Mappers.kt` - Model conversions

**Files to Add**:
- `remote/backend/FirebaseBackendService.kt` - Real Firebase impl
- `remote/backend/RestBackendService.kt` - REST API impl
- More repositories as models added
- Test files for each repository

### `:ui` Module
**Size**: ~100 lines (growing with screens)
**Dependencies**: `:core`, `:data`, `:common`, Compose, Hilt
**Purpose**: All UI and ViewModels

**Key Files**:
- `base/BaseViewModel.kt` - ViewModel base class
- `theme/Theme.kt` - Compose theme

**Files to Add**:
- `screens/Auth/LoginScreen.kt`
- `screens/Auth/SignupScreen.kt`
- `screens/Booking/BookingListScreen.kt`
- `screens/Booking/BookingDetailScreen.kt`
- `screens/Profile/ProfileScreen.kt`
- `screens/Search/SearchScreen.kt`
- `components/OfflineBanner.kt`
- `components/SyncStatusBadge.kt`
- Test files for each ViewModel

### `:app` Module
**Size**: ~150 lines
**Dependencies**: All other modules
**Purpose**: Application entry point and DI configuration

**Key Files**:
- `DiamondsApplication.kt` - Hilt setup
- `MainActivity.kt` - Entry point
- `di/Modules.kt` - All DI bindings
- `AndroidManifest.xml` - Permissions

**Files to Add**:
- `navigation/NavGraph.kt` - Navigation setup
- Test files

## Build Configuration Files

### Root `build.gradle.kts`
- Hilt plugin version
- Android build plugin version
- Kotlin plugin version

### Module `build.gradle.kts`
Each module has its own with specific dependencies:
- `:core` - None (pure Kotlin)
- `:common` - Compose + DataStore
- `:data` - Room, Coroutines, Serialization
- `:ui` - Compose, Hilt, Lifecycle
- `:app` - All above + WorkManager

### `libs.versions.toml`
- Version catalog for all dependencies
- Centralized version management
- Used across all modules via `libs.xxx`

## How to Find Things

### Domain Models
→ `core/src/main/java/com/example/diamonds/domain/model/`

### Database
→ `data/src/main/java/com/example/diamonds/data/local/`

### Repositories
→ `data/src/main/java/com/example/diamonds/data/repository/`

### ViewModels
→ `ui/src/main/java/com/example/diamonds/ui/screens/`

### Compose Screens
→ `ui/src/main/java/com/example/diamonds/ui/screens/`

### Dependency Injection
→ `app/src/main/java/com/example/diamonds/di/`

### Unit Tests
→ `[module]/src/test/java/com/example/diamonds/`

### UI Tests
→ `[module]/src/androidTest/java/com/example/diamonds/`

## Adding New Files

### New Repository
1. Update domain interface: `core/domain/repository/Repositories.kt`
2. Create entity: `data/local/entity/Entities.kt`
3. Create DAO: `data/local/dao/Daos.kt`
4. Create implementation: `data/repository/XyzRepository.kt`
5. Add mapping: `data/mapper/Mappers.kt`
6. Add Hilt binding: `app/di/Modules.kt`

### New Screen
1. Create ViewModel: `ui/src/main/java/com/example/diamonds/ui/screens/XyzViewModel.kt`
2. Create Compose screen: `ui/src/main/java/com/example/diamonds/ui/screens/XyzScreen.kt`
3. Add to navigation: `app/navigation/NavGraph.kt` (when created)

### New Utility
1. Add to appropriate module
2. If shared: `common/src/main/java/com/example/diamonds/common/`
3. If data-specific: `data/src/main/java/com/example/diamonds/data/util/`

## File Statistics

| Component | Lines | Files | Status |
|-----------|-------|-------|--------|
| Domain Models | 200 | 2 | ✅ Complete |
| Database (Room) | 400 | 3 | ✅ Complete |
| Repositories | 600 | 7 | ✅ Complete |
| Sync/Backend | 300 | 4 | ✅ Complete |
| Hilt DI | 150 | 1 | ✅ Complete |
| UI Base | 100 | 2 | ✅ Complete |
| Documentation | 1500+ | 5 | ✅ Complete |
| **TOTAL** | **3250+** | **24** | **✅ Phase 1** |

---

**Note**: This structure is designed for parallel development. Different team members can work on different modules simultaneously without conflicts.
