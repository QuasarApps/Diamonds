# Implementation Roadmap

## 🧭 Remediation Roadmap — supersedes "Next Steps" *(Tech-Lead Review, 2026-06-25)*

> The original Phase 1–21 plan below tracks **what was built**. An independent review
> ([`TECH_LEAD_REVIEW.md`](TECH_LEAD_REVIEW.md)) found that several phases marked "✅ COMPLETE" are
> **structurally complete but not functionally working** (re-tagged `✅ STRUCTURAL ⚠️` inline). The
> architecture is strong; the gap is "compiles & demos on the mock backend" vs "works against real
> Firebase." This section is the corrected, evidence-based work order. Each item cites the review
> section that substantiates it. **Do these before resuming the linear Phase 16→21 march.**
>
> ### Track A — Make the docs true (cheap, high trust; ~few days)
> - [ ] **Decide the offline-write story:** wire `SyncManager.queueOperation()` into write repos, or
>   delete the dead queue/backoff/conflict code. Then fix README/AUDIT, which advertise it. *(§4)*
> - [ ] Fix `RecurringBookingViewModel.getCurrentSession()` infinite-suspend → use `.first()`. *(§3.3)*
> - [ ] Fix `MessageRepository.sendMessage` offline message loss (queue or surface pending state). *(§3.4)*
> - [ ] Correct stale/inaccurate doc claims (§6): locale switching IS wired; `BookingRepositoryTest`
>   is no longer a no-op; coverage is well above "15–20%"; `getReviewsForProvider` is not a full scan;
>   README's "encrypted session storage" contradicts plaintext-token reality.
> - [ ] Add CI: build + `./gradlew allUnitTests` on every push (the committed `*_build.txt` logs are
>   stale; there is currently no green/red signal). *(§7)*
>
> ### Track B — Make it actually multilingual & accessible
> - [ ] Externalize the ~350 hardcoded `Text("…")` literals to `stringResource(R.string.*)` (translations
>   already exist) and add a `HardcodedText` lint baseline. *(§3.2)*
> - [ ] Replace emoji-as-icons with `androidx.compose.material.icons.*` + `contentDescription`; localize
>   `contentDescription`s; adopt `collectAsStateWithLifecycle`. *(§4)*
>
> ### Track C — Before flipping `USE_MOCK_BACKEND=false` (Firebase go-live gate)
> - [ ] Implement the 14 `FirebaseBackendService` stubs + repo stubs (`getCurrentClient`,
>   `updateProvider`, `getPaymentsForProvider`, `updatePaymentStatus`, `updateService`). *(§3.1, §4)*
> - [ ] Persist `direction`/`locationTags` in `createReview`; populate price/duration/type in Firebase
>   `createBooking`; create the profile doc on signup; register the FCM token server-side. *(§3.5, §4)*
> - [ ] **Security:** encrypt the session store; `allowBackup=false` / exclude DataStore; write & deploy
>   Firestore Security Rules; integrate a real PSP (status from server webhook only). *(§3.6)*
> - [ ] Real `google-services.json`, real `applicationId`, `signingConfig`, ProGuard keep rules +
>   `isMinifyEnabled=true`, supply `MAPS_API_KEY`, add Coil. *(§4, §5)*
>
> ### Track D — Hardening (folds into Phase 17/18)
> - [ ] Unit-test `SyncManager` / Workers / `PreferencesDataStore` / `ConnectivityObserver` and the
>   newly-implemented repo methods; use Turbine to assert state transitions; export Room schemas +
>   migration tests; remove boilerplate `Example*Test`. *(§4, §5)*
> - [ ] Introduce a `:core` connectivity interface + `DispatcherProvider`; fix the captive-portal
>   `NET_CAPABILITY_VALIDATED` check; cancel app/service coroutine scopes; drop unjustified
>   `ACCESS_BACKGROUND_LOCATION`; apply the `kotlin-serialization` plugin to `:core`. *(§4, §5)*
>
> **Gate:** treat Tracks A–C as exit criteria for "production candidate." The linear roadmap's
> Phase 19 (Release Prep) cannot truthfully start until Track C is done.

---

## Phase 1: Core Infrastructure ✅ COMPLETE
Core architecture and scaffolding in place.

- [x] Create modular Gradle structure (`:core`, `:data`, `:ui`, `:common`, `:app`)
- [x] Set up dependency injection with Hilt
- [x] Define domain models (Client, Provider, Booking, Review, Payment)
- [x] Create repository interfaces
- [x] Implement Room database (entities, DAOs, migrations)
- [x] Build data layer (repositories, mappers)
- [x] Create connectivity observer
- [x] Implement sync queue and SyncManager
- [x] Add DataStore for session management
- [x] Create BaseViewModel with state management
- [x] Set up Jetpack Compose theme
- [x] Create MainActivity entry point
- [x] Write basic unit tests

**Key Files**:
- `:core/domain/` - Domain models and interfaces
- `:data/local/` - Room database implementation
- `:data/repository/` - All repository implementations
- `:ui/base/BaseViewModel.kt` - State management base
- `:app/di/Modules.kt` - Hilt configuration

---

## Phase 2: Authentication & User Management ✅ COMPLETE
User authentication and profile management screens.

### Tasks
- [x] Create auth screen ViewModel (AuthViewModel)
- [x] Build Login screen (Compose) with demo account table
- [x] Build Signup screen (Compose) with role/cleaner type selection
- [x] Build role selection (Client vs Provider vs Company)
- [x] Implement token refresh logic in AuthRepository
- [x] Add JWT token decoding support
- [x] Store auth token securely in DataStore
- [x] Create ProfileScreen for customers and cleaners
- [x] Build profile edit flows with service management
- [x] Add profile picture/avatar support

### Tests
- [x] Auth flow unit tests
- [x] Login/Signup UI tests
- [x] Token management tests

**Status**: ✅ COMPLETE

---

## Phase 3: Booking Flow (Client Side) ✅ COMPLETE
Core booking functionality for clients.

### Tasks
- [x] Create ProviderSearchScreen with filters and badges
- [x] Build ServiceListScreen with categories
- [x] Create BookingDetailsScreen with status tracking
- [x] Build BookingConfirmationScreen with cost summary
- [x] Implement date/time picker for booking
- [x] Add address input with location support
- [x] Create BookingHistoryScreen showing past bookings
- [x] Build BookingDetailsScreen with real-time status updates
- [x] Add booking status transitions (PENDING → ACCEPTED → IN_PROGRESS → COMPLETED)
- [x] Implement pull-to-refresh on booking lists

### Tests
- [x] Search repository tests
- [x] Booking creation tests
- [x] Offline booking list tests

**Status**: ✅ COMPLETE

---

## Phase 4: Provider Management ✅ COMPLETE
Provider-side features including cleaner dashboards, company management, and service handling.

### Key Deliverables
- [x] Three distinct role-based dashboards: Customer, Cleaner (Independent/Employed), Company
- [x] CleanerViewModel with multi-tab state management (Dashboard, Requests, Schedule)
- [x] CleanerBookingRequestsScreen for accept/decline of PENDING bookings
- [x] CleanerScheduleScreen for today's and upcoming jobs with Start/Complete actions
- [x] Live CleanerDashboardTab with pending count, today's jobs, weekly earnings
- [x] CompanyViewModel for aggregate company statistics across all employed cleaners
- [x] CompanyDashboardScreen with KPI cards (team size, revenue, active jobs)
- [x] CompanyTeamScreen listing all team members with per-cleaner stats
- [x] CompanyBookingsScreen with tabbed view (Pending/Active/Completed) with accept/decline actions
- [x] CompanyEarningsScreen with weekly charts, monthly KPIs, and per-cleaner breakdown
- [x] CleanerType enum (INDEPENDENT, EMPLOYED, COMPANY) persisted through session
- [x] CleanerProfileScreen with avatar, editable bio, employment badge, services summary
- [x] ServiceManagementScreen for adding/editing/toggling services with categories
- [x] ProviderRatingsScreen showing avg rating, star distribution, and scrollable reviews
- [x] CleanerEarningsScreen with 7-day Canvas chart, monthly KPIs, booking history
- [x] SignupScreen with INDEPENDENT/EMPLOYED/COMPANY radio selector and info cards
- [x] ProviderSearchScreen with Independent/Employed/Company badges on cards
- [x] LoginScreen with demo account table (tap to auto-fill 4 demo accounts)
- [x] AppShell with session-aware routing by cleanerType
- [x] Seeded demo accounts (p1-p5) with realistic cleaner bookings and earnings data
- [x] Room migration v1→v2 for cleanerType, employerId, employerName columns

### Tests
- [x] Provider profile tests
- [x] Service management tests
- [x] Booking request handling tests

**Status**: ✅ COMPLETE

---

## Phase 5: Reviews & Ratings ✅ STRUCTURAL ⚠️
Post-job review system.
> ⚠️ **Review 2026-06-25:** `FirebaseBackendService.createReview` never persists the `direction`
> (and `locationTags`) field, so against real Firestore every review defaults to
> `CLIENT_REVIEWS_PROVIDER` and the client↔provider review split breaks. See
> [TECH_LEAD_REVIEW.md §3.5](TECH_LEAD_REVIEW.md).

### Tasks
- [x] Seed rich review data (12 seeded reviews across p1–p4, mutable store so new reviews persist in session)
- [x] Add getReviewsForBooking to IBackendService + BackendServiceStub
- [x] ReviewViewModel — load booking context, check existing review, submit, load all reviews for a provider
- [x] ReviewScreen — interactive ★☆ tap picker, label (Poor→Excellent), optional comment field, read-only view if already reviewed
- [x] StarPicker — shared reusable composable (interactive + read-only modes)
- [x] ProviderRatingsScreen — large avg rating, star breakdown LinearProgressIndicator bars (5→1), scrollable review card list with client initials avatar
- [x] ReviewBooking + ProviderRatings routes added to Screen
- [x] BookingDetailScreen — "⭐ Leave a Review" button shown only for COMPLETED bookings
- [x] ProviderSearchScreen — star row is now tappable (navigates to ProviderRatingsScreen)
- [ ] Add photo upload for reviews
- [ ] Implement review moderation (future)


---

## Phase 6: Payments ✅ STRUCTURAL ⚠️
Payment processing with mock/stub implementation, ready for Stripe swap-in.
> ⚠️ **Review 2026-06-25:** Both Stub and Firebase paths fabricate a `SUCCEEDED` payment with a
> synthetic `transactionId` and **no payment processor** — a booking is marked paid with no money
> moving. The Stub's "5% failure rate" comment is dead code. No real PSP = a launch blocker, not a
> swap-in. See [TECH_LEAD_REVIEW.md §3.6](TECH_LEAD_REVIEW.md).

### Tasks
- [x] Seed payment data in BackendServiceStub (6 historical payments, mutable store)
- [x] Add getPaymentsForClient to IBackendService + implement in BackendServiceStub
- [x] Fix PaymentRepository.getPaymentsForClient (was TODO/stub — now fully implemented)
- [x] PaymentViewModel — card input formatting (16-digit with spaces, MM/YY expiry, CVV), validation, processPayment, loadHistory
- [x] PaymentScreen — stylised card visual (updates live as user types), card number / holder / expiry / CVV fields, Pay button with spinner, decline banner, "Skip Payment (Demo)" link, accepted cards row
- [x] PaymentSuccessScreen — receipt-style card (service, provider, amount, txn ID, status), View Bookings + Back to Home buttons
- [x] PaymentHistoryScreen — LazyColumn of all payments with status badge (✅/❌/↩️/⏳)
- [x] CustomerProfileScreen — avatar + name + email, "My Bookings" and "Payment History" quick-link cards, account info, Sign Out button
- [x] BookingConfirmationScreen — "💳 Pay Now" added as primary CTA navigating to PaymentScreen
- [x] Payment + PaymentSuccess + PaymentHistory routes added to Screen; wired in AppShell
- [x] CustomerProfile route wired in AppShell (replaces placeholder)
- [ ] Integrate real Stripe SDK (Phase 8 / backend integration)
- [ ] Payment method management (save cards)
- [ ] Refund handling UI

---

## Phase 7: Navigation & App Flow ✅ COMPLETE
Comprehensive navigation, deep linking, transitions, and UX polish.

### Tasks
- [x] Add navArgument declarations to all parameterised composable routes
- [x] Create bottom navigation for main screens (role-based: Customer, Cleaner, Company)
- [x] Implement deep linking for bookings (diamonds://customer/booking/{id}, diamonds://customer/confirm/{id})
- [x] Register diamonds:// URI scheme in AndroidManifest
- [x] Handle deep links via singleTop launch mode and onNewIntent
- [x] Add back stack management (BackHandler on BookingConfirmation, PaymentSuccess)
- [x] Improved logout back-stack clearing (popUpTo graph.id)
- [x] Create splash screen with branded animations
- [x] Build app-level navigation logic (root NavHost → inner NavHost per role)
- [x] Add navigation transition animations (slide left/right for push/pop, fade for tab switches)
- [x] Add error handling screens (GenericErrorScreen, NoInternetScreen, NotFoundScreen)
- [x] Add offline connectivity banner (OfflineBanner shown when device is offline)
- [x] Implement bottom sheets for actions (QuickBookingSheet, FilterSheet for provider search)
- [x] Add confirmation dialogs (reusable ConfirmationDialog for sign-out, cancel booking)
- [x] Replace inline AlertDialog usage with reusable ConfirmationDialog component

### New Files
- `ui/components/ConfirmationDialog.kt` — Reusable Material 3 confirmation dialog
- `ui/components/ErrorScreens.kt` — GenericErrorScreen, NoInternetScreen, NotFoundScreen, OfflineBanner
- `ui/components/QuickBookingSheet.kt` — Quick-book bottom sheet with service categories
- `ui/components/FilterSheet.kt` — Advanced filter bottom sheet (category, rating, price)

### Modified Files
- `ui/shell/AppShell.kt` — navArguments, deep links, transitions, offline banner, sign-out dialog
- `ui/navigation/DiamondsNavHost.kt` — Fade transitions, improved back-stack on logout
- `ui/shell/TabScreens.kt` — QuickBookingSheet on CustomerHomeTab
- `ui/booking/ProviderSearchScreen.kt` — FilterSheet integration
- `ui/booking/BookingsListScreen.kt` — Reusable ConfirmationDialog, NotFoundScreen
- `app/AndroidManifest.xml` — Deep link intent filter, singleTop launch mode
- `app/MainActivity.kt` — onNewIntent for deep link handling

**Status**: ✅ COMPLETE

---

## Phase 8: Firebase Integration ✅ STRUCTURAL ⚠️
Backend and real-time features via Firebase.
> ⚠️ **Review 2026-06-25:** The Firebase path is materially incomplete and `release` builds enable
> it (`USE_MOCK_BACKEND=false`): **14 `FirebaseBackendService` methods return
> `Result.Error("not yet implemented")`**, `google-services.json` is a placeholder, `signup` never
> writes a Client/Provider profile doc, and the FCM token is saved locally but never registered
> server-side (targeted push can't work). See [TECH_LEAD_REVIEW.md §3.1, §4](TECH_LEAD_REVIEW.md).

### Tasks

- [x] Create FirebaseBackendService implementation
- [x] Implement Firebase Auth integration
- [x] Set up Firestore collections (schema defined in FirebaseBackendService)
- [x] Add Firebase Cloud Messaging (FCM)
- [x] Implement push notifications
- [x] Add real-time booking updates (FirestoreBookingListener)
- [x] Create notification handler (DiamondsFcmService)
- [x] Build notification management screen (NotificationScreen + NotificationPreferencesScreen)
- [x] Add notification preferences (DataStore-backed toggles)

### New Files

- `data/remote/backend/FirebaseBackendService.kt` — Firestore IBackendService implementation
- `data/remote/backend/FirestoreBookingListener.kt` — Real-time Firestore snapshot listeners
- `data/repository/NotificationRepository.kt` — Local notification persistence + preferences
- `app/fcm/DiamondsFcmService.kt` — FCM service (push notifications)
- `ui/notification/NotificationViewModel.kt` — Notification state management
- `ui/notification/NotificationScreen.kt` — Notification list with swipe-to-dismiss
- `ui/notification/NotificationPreferencesScreen.kt` — Notification toggle settings

### Modified Files

- `data/remote/auth/FirebaseAuthService.kt` — Full Firebase Auth implementation
- `data/local/entity/Entities.kt` — Added NotificationEntity
- `data/local/dao/Daos.kt` — Added NotificationDao
- `data/local/AppDatabase.kt` — Version 3, MIGRATION_2_3, notificationDao()
- `data/mapper/Mappers.kt` — Notification entity ↔ domain mappers
- `data/local/preferences/PreferencesDataStore.kt` — FCM token + notification prefs
- `core/domain/model/DomainModels.kt` — Notification, NotificationType, NotificationPreferences
- `core/domain/repository/Repositories.kt` — INotificationRepository
- `ui/navigation/Screen.kt` — Notifications + NotificationPreferences routes
- `ui/shell/AppShell.kt` — Bell icon with badge, notification composable destinations
- `app/di/Modules.kt` — Firebase backend switching, NotificationRepository binding
- `app/DiamondsApplication.kt` — Notification channel creation
- `app/AndroidManifest.xml` — FCM service, POST_NOTIFICATIONS permission

### How to Activate Firebase

1. Create a Firebase project at https://console.firebase.google.com
2. Enable Email/Password auth, Firestore, and Cloud Messaging
3. Download `google-services.json` → place in `app/`
4. Uncomment `alias(libs.plugins.google.services)` in `app/build.gradle.kts`
5. Set `USE_MOCK_AUTH` and `USE_MOCK_BACKEND` to `false` in `app/build.gradle.kts`
6. Sync Gradle and build

### Tests
- [ ] Firebase Auth tests
- [ ] Firestore integration tests
- [ ] FCM notification tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

---

## Phase 9: Maps & Location ✅ COMPLETE
Maps integration and location services.

### Tasks

- [x] Integrate Google Maps SDK (maps-compose 4.3.3, play-services-maps, play-services-location)
- [x] Implement location permissions (RequireLocationPermission composable with rationale dialog)
- [x] Create BookingMapScreen for address selection (tap map, search, "My Location")
- [x] Add provider location display (ProviderLocationEntity cached in Room, seeded demo data)
- [x] Implement service area visualization (Circle overlay on Google Map)
- [x] Create real-time tracking during job (ProviderTrackingScreen with live updates, ETA, distance)
- [x] Add ETA calculation (Haversine distance / avg speed estimate)
- [x] Build distance display (real-time Haversine distance in tracking screen)
- [x] BookingLocationMapCard in BookingDetailScreen showing mini-map
- [x] "📍 Pick on Map" button in BookingFormScreen with savedStateHandle data return
- [x] "🗺️ Track Cleaner" button on BookingDetailScreen for ACCEPTED/IN_PROGRESS bookings
- [x] Room migration v3→v4 for provider_locations table
- [x] ILocationRepository + LocationRepository with FusedLocationProviderClient
- [x] Hilt DI wiring (FusedLocationProviderClient, ILocationRepository)
- [x] Google Maps API key via gradle.properties + manifestPlaceholders
- [ ] Implement geofencing (future)

### New Files

- `ui/components/LocationPermission.kt` — RequireLocationPermission composable
- `ui/map/MapViewModel.kt` — Map & tracking state management
- `ui/map/BookingMapScreen.kt` — Full-screen map address picker
- `ui/map/BookingLocationMapCard.kt` — Mini-map card for booking detail
- `ui/map/ProviderTrackingScreen.kt` — Real-time cleaner tracking screen
- `data/repository/LocationRepository.kt` — Location repository with Haversine, FusedLocation,
  caching

### Modified Files

- `gradle/libs.versions.toml` — Added maps-compose, play-services-maps, play-services-location
- `ui/build.gradle.kts` — Maps Compose dependencies
- `data/build.gradle.kts` — Play Services Location dependency
- `app/build.gradle.kts` — Play Services Location, MAPS_API_KEY manifestPlaceholders
- `gradle.properties` — MAPS_API_KEY property
- `app/AndroidManifest.xml` — Maps API key meta-data, BACKGROUND_LOCATION permission
- `core/domain/model/DomainModels.kt` — GeoLocation, ServiceArea, ProviderLocation, TrackingState
- `core/domain/repository/Repositories.kt` — ILocationRepository interface
- `data/local/entity/Entities.kt` — ProviderLocationEntity
- `data/local/dao/Daos.kt` — ProviderLocationDao
- `data/local/AppDatabase.kt` — Version 4, MIGRATION_3_4, providerLocationDao()
- `data/mapper/Mappers.kt` — ProviderLocation mappers
- `data/remote/backend/IBackendService.kt` — Location/tracking endpoints, DTOs
- `data/remote/backend/BackendServiceStub.kt` — Seeded provider locations & service areas
- `data/remote/backend/FirebaseBackendService.kt` — Firestore location collection stubs
- `app/di/Modules.kt` — FusedLocationProviderClient, ILocationRepository bindings
- `ui/navigation/Screen.kt` — BookingMap, ProviderTracking routes
- `ui/shell/AppShell.kt` — Map composable destinations, savedStateHandle, title mappings
- `ui/booking/BookingFormScreen.kt` — "Pick on Map" button, map coordinates integration
- `ui/booking/BookingViewModel.kt` — lat/lng in BookingFormUiState, onLocationSelected()
- `ui/booking/BookingsListScreen.kt` — BookingLocationMapCard, Track Cleaner button

### Tests
- [ ] Location permission tests
- [ ] Map display tests
- [ ] Distance calculation tests

**Status**: ✅ COMPLETE

**Estimated Duration**: 2-3 weeks

---

## Phase 10: Sync & Offline Features ✅ STRUCTURAL ⚠️
Complete offline-first implementation and sync.
> ⚠️ **Review 2026-06-25:** The sync engine is **dead code** — `SyncManager.queueOperation()` has
> zero production call sites. Every write path hard-fails when offline instead of enqueuing, so the
> backoff/conflict-resolution machinery processes a table that is never populated. Read-side
> offline-first is real; the offline-**write** story advertised here does not run. Decision needed:
> wire the queue into write repos, or delete it and correct the docs. See [TECH_LEAD_REVIEW.md §4](TECH_LEAD_REVIEW.md).

### Tasks

- [x] Implement SyncWorker background job (@HiltWorker with periodic + immediate scheduling)
- [x] Add connectivity change triggers (ConnectivitySyncTrigger auto-syncs on OFFLINE→ONLINE)
- [x] Build sync status UI (SyncStatusScreen with tabbed Pending/Failed/Conflicts view)
- [x] Create pending operations list (real-time observable sync queue)
- [x] Add manual retry for failed syncs (per-operation + retry-all)
- [x] Implement operation cancellation UI (cancel button on each operation card)
- [x] Add sync conflict resolution (CONFLICT status, "Keep Local" / "Use Server" UI)
- [x] Create sync error reporting (error message display, retry count tracking)
- [x] Backend dispatch implemented (SyncManager dispatches by EntityType × OperationType)

### New Files

- `data/sync/ConnectivitySyncTrigger.kt` — Auto-sync on connectivity restoration with debounce
- `ui/sync/SyncStatusViewModel.kt` — ViewModel with pending/failed/conflict state management
- `ui/sync/SyncStatusScreen.kt` — Full sync status screen with tabs, retry, cancel, resolve

### Modified Files

- `data/sync/SyncManager.kt` — Full backend dispatch, conflict handling, retryAllFailed,
  resolveConflict, syncNow
- `data/worker/SyncWorker.kt` — @HiltWorker migration, periodic + immediate scheduling, network
  constraints
- `data/local/entity/Entities.kt` — serverPayload column on SyncQueueEntity
- `data/local/dao/Daos.kt` — observeFailedOperations, observeConflictOperations, markConflict,
  resetAllFailed
- `data/local/AppDatabase.kt` — Version 5, MIGRATION_4_5
- `data/remote/backend/IBackendService.kt` — @Serializable annotations on DTOs/requests
- `data/build.gradle.kts` — Hilt, hilt-work, serialization plugins
- `core/domain/model/DomainModels.kt` — CONFLICT added to SyncStatus enum
- `core/domain/repository/Repositories.kt` — retryAllFailed, resolveConflict, observeFailedOps,
  syncNow
- `ui/shell/AppShellViewModel.kt` — pendingSyncCount from ISyncRepository
- `ui/shell/AppShell.kt` — Sync badge in top bar, SyncStatus route
- `ui/navigation/Screen.kt` — SyncStatus route
- `app/DiamondsApplication.kt` — HiltWorkerFactory, periodic sync scheduling,
  ConnectivitySyncTrigger
- `app/di/Modules.kt` — ConnectivitySyncTrigger provider
- `app/build.gradle.kts` — hilt-work dependencies
- `gradle/libs.versions.toml` — hilt-work version and libraries
- `common/util/Constants.kt` — MAX_RETRY_ATTEMPTS increased to 5

### Tests
- [ ] Sync queue tests
- [ ] Connectivity change tests
- [ ] Offline data preservation tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

---

## Phase 11: In-App Chat System ✅ STRUCTURAL ⚠️
> ⚠️ **Review 2026-06-25:** `MessageRepository.sendMessage` reports `Result.Success` for messages
> composed offline / on transient backend failure, but never sends or retries them — silent chat
> **data loss**. See [TECH_LEAD_REVIEW.md §3.4](TECH_LEAD_REVIEW.md).

Real-time messaging between clients and cleaners.

### Tasks

- [x] Define Message and Conversation domain models in DomainModels.kt
- [x] Add IMessageRepository interface to Repositories.kt
- [x] Create MessageEntity and ConversationEntity in Room with DAO
- [x] Add Room migration v5 to v6 for messages and conversations tables
- [x] Implement MessageRepository with local caching and sync support
- [x] Add Firestore conversations collection to FirebaseBackendService
- [x] Add chat endpoints and DTOs to IBackendService and BackendServiceStub
- [x] Build ChatViewModel (load conversation, send message, mark as read)
- [x] Build ConversationListScreen - list of all active conversations
- [x] Build ChatScreen - message bubbles, input bar, send button, timestamps
- [x] Add unread message badge on top bar (💬 icon)
- [x] Wire chat routes in Screen.kt and AppShell.kt
- [x] Seed demo chat conversations for demo accounts
- [x] "💬 Chat with Cleaner" button on BookingDetailScreen

### New Files

- `ui/chat/ChatViewModel.kt` - Chat and conversation state management
- `ui/chat/ConversationListScreen.kt` - List of all conversations per user
- `ui/chat/ChatScreen.kt` - Full messaging screen with bubbles and input bar
- `data/repository/MessageRepository.kt` - Message persistence and sync

### Modified Files

- `core/domain/model/DomainModels.kt` - Message, Conversation models
- `core/domain/repository/Repositories.kt` - IMessageRepository interface
- `data/local/entity/Entities.kt` - MessageEntity, ConversationEntity
- `data/local/dao/Daos.kt` - MessageDao, ConversationDao
- `data/local/AppDatabase.kt` - Version 6, MIGRATION_5_6
- `data/remote/backend/IBackendService.kt` - Chat endpoints and DTOs
- `data/remote/backend/BackendServiceStub.kt` - Seeded demo conversations
- `data/remote/backend/FirebaseBackendService.kt` - Firestore chat collections
- `data/mapper/Mappers.kt` - Conversation/Message mappers
- `ui/navigation/Screen.kt` - Chat and ConversationList routes
- `ui/shell/AppShell.kt` - Chat composable destinations, 💬 badge in top bar
- `ui/booking/BookingsListScreen.kt` - "Chat with Cleaner" button in BookingDetailScreen
- `app/di/Modules.kt` - IMessageRepository binding

### Tests

- [ ] Message repository tests
- [ ] Chat ViewModel tests
- [ ] Real-time listener tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

### Modified Files

- `core/domain/model/DomainModels.kt` - Message, Conversation models
- `core/domain/repository/Repositories.kt` - IMessageRepository interface
- `data/local/entity/Entities.kt` - MessageEntity, ConversationEntity
- `data/local/dao/Daos.kt` - MessageDao, ConversationDao
- `data/local/AppDatabase.kt` - Version 6, MIGRATION_5_6
- `data/remote/backend/IBackendService.kt` - Chat endpoints and DTOs
- `data/remote/backend/BackendServiceStub.kt` - Seeded demo conversations
- `data/remote/backend/FirebaseBackendService.kt` - Firestore chat collections
- `app/fcm/DiamondsFcmService.kt` - New message push notification handling
- `ui/navigation/Screen.kt` - Chat and ConversationList routes
- `ui/shell/AppShell.kt` - Chat composable destinations, unread badge

### Tests

- [ ] Message repository tests
- [ ] Chat ViewModel tests
- [ ] Real-time listener tests

**Estimated Duration**: 2-3 weeks

---

## Phase 12: Subscription and Recurring Bookings ✅ STRUCTURAL ⚠️
> ⚠️ **Review 2026-06-25:** `RecurringBookingViewModel.getCurrentSession()` collects a
> never-completing DataStore flow with `return@collect`, which only returns the lambda — `collect()`
> never returns, so the suspend fn **hangs forever** and recurring-booking submit/load is broken.
> Fix: use `.first()`. See [TECH_LEAD_REVIEW.md §3.3](TECH_LEAD_REVIEW.md).

Allow clients to set up recurring cleaning schedules.

### Tasks

- [x] Define RecurringBooking domain model with RecurringFrequency and RecurringBookingStatus enums
- [x] Add ISubscriptionRepository interface
- [x] Create RecurringBookingEntity in Room with RecurringBookingDao
- [x] Add Room migration v6 to v7 for recurring_bookings table
- [x] Implement SubscriptionRepository with local and remote sync
- [x] Add subscription and recurring booking endpoints to IBackendService and BackendServiceStub
- [x] Add Firestore recurringBookings collection to FirebaseBackendService
- [x] Build RecurringBookingViewModel (create, pause, cancel, resume, modify schedule)
- [x] Build RecurringBookingSetupScreen - frequency picker (daily/weekly/fortnightly/monthly), day
  selector, time, provider
- [x] Build SubscriptionManagementScreen - list active plans, pause/cancel controls, next booking
  date
- [x] Add "Recurring Bookings" quick link in CustomerProfileScreen
- [x] Auto-generate upcoming bookings from recurring schedules via RecurringBookingWorker (daily
  WorkManager job)
- [x] Handle RECURRING_BOOKING EntityType in SyncManager and SyncStatusScreen
- [x] Seed demo recurring bookings for demo accounts (3 seeded: weekly, monthly, fortnightly)
- [x] Wire RecurringBookingSetup and SubscriptionManagement routes in Screen.kt and AppShell.kt
- [x] DI wiring for ISubscriptionRepository in Modules.kt
- [ ] Handle payment for recurring bookings (charge on each occurrence) — future
- [ ] Add notification reminders before each recurring booking — future

### New Files

- `ui/subscription/RecurringBookingViewModel.kt` - Recurring booking state management
- `ui/subscription/RecurringBookingSetupScreen.kt` - Schedule setup screen
- `ui/subscription/SubscriptionManagementScreen.kt` - Active plans management screen
- `data/repository/SubscriptionRepository.kt` - Subscription persistence and sync
- `data/worker/RecurringBookingWorker.kt` - WorkManager job for auto-generating bookings

### Modified Files

- `core/domain/model/DomainModels.kt` - RecurringBooking, SubscriptionPlan models
- `core/domain/repository/Repositories.kt` - ISubscriptionRepository interface
- `data/local/entity/Entities.kt` - RecurringBookingEntity, SubscriptionEntity
- `data/local/dao/Daos.kt` - RecurringBookingDao, SubscriptionDao
- `data/local/AppDatabase.kt` - Version 7, MIGRATION_6_7
- `data/remote/backend/IBackendService.kt` - Subscription endpoints and DTOs
- `data/remote/backend/BackendServiceStub.kt` - Seeded demo recurring bookings
- `data/remote/backend/FirebaseBackendService.kt` - Firestore subscription collections
- `ui/navigation/Screen.kt` - RecurringBookingSetup, SubscriptionManagement routes
- `ui/shell/AppShell.kt` - Subscription composable destinations
- `ui/booking/BookingsListScreen.kt` - Recurring booking badge/label

### Tests

- [ ] Recurring booking generation tests
- [ ] Subscription repository tests
- [ ] WorkManager scheduling tests

**Estimated Duration**: 2-3 weeks

---

## Phase 13: Multi-Language Support ✅ STRUCTURAL ⚠️
> ⚠️ **Review 2026-06-25:** Non-functional in practice. 845 strings are professionally translated
> (ES/FR/AR/PT, real Arabic) but `stringResource` is used in **1 of 83 UI files** — ~350 `Text("…")`
> literals are hardcoded English, so switching locale changes almost nothing on screen.
> *Correction to prior audit:* runtime locale switching **is** now wired via
> `AppCompatDelegate.setApplicationLocales()` (README #9 is stale). See [TECH_LEAD_REVIEW.md §3.2](TECH_LEAD_REVIEW.md).

Full internationalisation and localisation of the app.

### Tasks

- [x] Audit all hardcoded strings across all screens and components
- [x] Extract all UI strings into res/values/strings.xml
- [x] Create locale resource directories (values-fr, values-es, values-pt, values-ar as initial
  targets)
- [x] Add LanguagePreference to PreferencesDataStore
- [x] Build in-app language selector accessible from Profile and Settings screen
- [x] Apply locale dynamically at runtime without requiring app restart
- [x] Handle RTL layout support for Arabic and other RTL languages
- [x] Localise date, time, and currency formats per locale
- [x] Translate strings for all initial supported languages
- [x] Update SignupScreen and ProfileScreen to show and store preferred language
- [x] Add locale to user profile synced with backend
- [ ] Add photo upload for reviews (future)
- [ ] Implement review moderation (future)

### New Files

- `ui/settings/LanguageSelectorScreen.kt` — In-app language picker screen with flag + radio buttons
- `ui/settings/LanguageViewModel.kt` — Language preference state management via PreferencesDataStore
- `ui/src/main/res/values/strings.xml` — All UI strings extracted (190+ entries)
- `ui/src/main/res/values-fr/strings.xml` — French translations
- `ui/src/main/res/values-es/strings.xml` — Spanish translations
- `ui/src/main/res/values-pt/strings.xml` — Portuguese translations
- `ui/src/main/res/values-ar/strings.xml` — Arabic translations
- `app/src/main/res/values-fr/strings.xml` — French translations (app module)
- `app/src/main/res/values-es/strings.xml` — Spanish translations (app module)
- `app/src/main/res/values-pt/strings.xml` — Portuguese translations (app module)
- `app/src/main/res/values-ar/strings.xml` — Arabic translations (app module)
- `app/src/main/res/xml/locale_config.xml` — Android 13+ per-app language configuration

### Modified Files

- `app/src/main/res/values/strings.xml` — All hardcoded UI strings extracted here (190+ entries)
- `data/local/preferences/PreferencesDataStore.kt` — LANGUAGE_KEY, saveLanguage(), observeLanguage()
- `ui/navigation/Screen.kt` — LanguageSelector route
- `ui/shell/AppShell.kt` — Language selector destination, title mapping, wired onLanguage callbacks
- `ui/customer/CustomerProfileScreen.kt` — onLanguage callback, 🌐 Language quick-link card
- `ui/cleaner/CleanerProfileScreen.kt` — onLanguage callback, 🌐 Language card in profile
- `app/MainActivity.kt` — Switched from ComponentActivity to AppCompatActivity for per-app locale
- `app/AndroidManifest.xml` — android:localeConfig for per-app language (Android 13+)

### Tests

- [ ] String resource completeness tests (no missing keys per locale)
- [ ] RTL layout tests
- [ ] Date and time formatting tests per locale

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

> ⚠️ **Updated 2026-06-25 (supersedes the April 2026 "known gap" below)**: runtime locale switching
> **is** now wired — `LanguageSelectorScreen` calls `AppCompatDelegate.setApplicationLocales()` — so
> the older note ("nothing reads the stored locale") is **stale**. The *real* remaining gap is that
> the UI hardcodes strings, so switching locale has almost no visible effect. Single source of truth:
> the Phase 13 callout above and [TECH_LEAD_REVIEW.md §3.2](TECH_LEAD_REVIEW.md).
>
> <details><summary>Original April 26, 2026 audit note (now resolved/stale — kept for history)</summary>
>
> > `LanguageViewModel` saves the locale code to DataStore but nothing reads it to reconfigure the
> > app locale. `AppCompatDelegate.setApplicationLocales()` must be called for runtime switching to
> > actually work.
> </details>

**Estimated Duration**: 2 weeks

---

## Phase 14: Reverse Reviews

Allow cleaners to review clients and their locations after a job.

### Tasks

- [ ] Extend Review domain model to support direction: CLIENT_REVIEWS_PROVIDER and
  PROVIDER_REVIEWS_CLIENT
- [ ] Add reviewDirection field to ReviewEntity and update Room migration v7 to v8
- [ ] Update IBackendService, BackendServiceStub, and FirebaseBackendService for bidirectional
  reviews
- [ ] Update ReviewRepository to handle both review directions
- [ ] Build ClientReviewViewModel - load booking context, check if cleaner has already reviewed
- [ ] Build LeaveClientReviewScreen - star picker, written feedback, optional location tags (e.g.
  Easy parking, Clear instructions, Pet-friendly)
- [ ] Build ClientRatingsScreen - customer average rating and review history visible to cleaners
- [ ] Show client rating badge on BookingRequestCard in CleanerBookingRequestsScreen
- [ ] Add Review Client button in CleanerScheduleScreen for COMPLETED bookings
- [ ] Prevent duplicate reverse reviews (same guard as forward reviews)
- [ ] Seed demo reverse review data for demo accounts

### New Files

- `ui/review/ClientReviewViewModel.kt` - Reverse review state management
- `ui/review/LeaveClientReviewScreen.kt` - Cleaner reviews client screen
- `ui/review/ClientRatingsScreen.kt` - Client rating profile visible to cleaners

### Modified Files

- `core/domain/model/DomainModels.kt` - ReviewDirection enum, updated Review model
- `data/local/entity/Entities.kt` - reviewDirection column on ReviewEntity
- `data/local/AppDatabase.kt` - Version 8, MIGRATION_7_8
- `data/mapper/Mappers.kt` - ReviewDirection mappers
- `data/remote/backend/IBackendService.kt` - Bidirectional review endpoints
- `data/remote/backend/BackendServiceStub.kt` - Seeded reverse reviews
- `data/remote/backend/FirebaseBackendService.kt` - Firestore reverse review support
- `ui/navigation/Screen.kt` - LeaveClientReview, ClientRatings routes
- `ui/shell/AppShell.kt` - Reverse review composable destinations
- `ui/cleaner/CleanerScheduleScreen.kt` - Review Client button on completed bookings
- `ui/cleaner/CleanerBookingRequestsScreen.kt` - Client rating badge on request cards

### Tests

- [ ] Bidirectional review logic tests
- [ ] Duplicate review prevention tests
- [ ] Client rating display tests

**Estimated Duration**: 1-2 weeks

**Status**: ✅ COMPLETE

> ⚠️ **Known gap (audit April 26, 2026)**: `FirebaseBackendService.createReview` does not write
> the `direction` field to Firestore. The `ReviewDirection` enum and Room column exist correctly,
> but the value is silently dropped when using the Firebase backend. Fix: include `direction` in
> the `ReviewDto` Firestore write in `FirebaseBackendService.createReview`.

---

## Phase 15: Help, Support & Claims System ✅ STRUCTURAL ⚠️
> ⚠️ **Review 2026-06-25:** Fully implemented in the Stub (so it works in debug), but **every**
> backing Firebase method is one of the 14 `"not yet implemented"` stubs — support tickets, claims,
> cancel-with-reason, edit-booking, refunds, help articles and saved locations all silent-fail in a
> `release` build. See [TECH_LEAD_REVIEW.md §3.1](TECH_LEAD_REVIEW.md).

Comprehensive contextual help, cancellations, booking edits, refunds, and post-service claims for
both customers and providers — integrated directly into booking and review flows.

### Domain Models (`:core/domain/model/DomainModels.kt`)

- `SupportTicketType` enum: `PRE_BOOKING`, `POST_BOOKING_PRE_SERVICE`, `DURING_SERVICE`,
  `POST_SERVICE`, `SAFETY_EMERGENCY`, `ORDER_ISSUE`, `SATISFACTION`
- `SupportTicketStatus` enum: `OPEN`, `IN_PROGRESS`, `AWAITING_RESPONSE`, `RESOLVED`, `CLOSED`
- `ClaimType` enum (customer): `INCOMPLETE_SERVICE`, `UNSATISFACTORY_SERVICE`, `PROPERTY_DAMAGE`,
  `INAPPROPRIATE_BEHAVIOR_PROVIDER`
- `ClaimType` enum (provider): `DANGEROUS_PROPERTY`, `EXCEEDINGLY_DIRTY`,
  `INAPPROPRIATE_BEHAVIOR_CLIENT`
- `ClaimStatus` enum: `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `RESOLVED`
- `CancellationReason` enum: `CHANGED_MIND`, `FOUND_ANOTHER`, `SCHEDULING_CONFLICT`, `PRICE_ISSUE`,
  `OTHER`
- `SupportTicket` data class: `id`, `userId`, `userRole`, `bookingId?`, `type`, `status`, `subject`,
  `description`, `conversationId?`, `createdAt`, `updatedAt`
- `Claim` data class: `id`, `bookingId`, `filedByUserId`, `filedByRole`, `claimType`, `status`,
  `description`, `evidenceImageUrls: List<String>`, `resolutionNotes?`, `refundAmount?`, `createdAt`,
  `updatedAt`
- Add `SUPPORT_UPDATE` and `CLAIM_UPDATE` to existing `NotificationType` enum

### Contextual Help Matrix

| Booking Status | Customer Actions                                    | Provider Actions                                  |
|----------------|-----------------------------------------------------|---------------------------------------------------|
| PENDING        | Edit address/service type, Cancel+Refund, Chat, FAQ | —                                                 |
| ACCEPTED       | Cancel+Refund, Chat provider, FAQ                   | —                                                 |
| IN_PROGRESS    | 🚨 Safety SOS, Chat provider, Report issue          | —                                                 |
| COMPLETED      | File claim, Leave review, Contact support           | File claim (dangerous/dirty/inappropriate client) |
| CANCELLED      | Refund status, Contact support                      | —                                                 |
| NO_SHOW        | File claim, Request refund, Contact support         | —                                                 |

### Tasks

#### Cancellation & Refund

- [x] Add `requestCancellation(bookingId, reason): Result<Booking>` — validates status is PENDING or
  ACCEPTED (not yet IN_PROGRESS); transitions booking to CANCELLED
- [x] Add `requestRefund(bookingId): Result<Payment>` — updates PaymentStatus to REFUNDED on
  associated payment
- [x] Build CancelBookingScreen — reason picker (radio list of CancellationReason), refund
  confirmation summary, "Confirm Cancellation" button
- [x] Show inline "Cancel Booking" button on BookingDetailScreen when status is PENDING or ACCEPTED

#### Booking Edits (Pre-Confirmation)

- [x] Add `editBooking(bookingId, newAddress?, newServiceId?): Result<Booking>` — validates status
  is
  PENDING only (provider has not yet accepted)
- [x] Build EditBookingScreen — pre-populated address and service type fields, "Save Changes" button
- [x] Show inline "Edit Booking" button on BookingDetailScreen when status is PENDING

#### Post-Service Claims (Customer)

- [x] Build FileClaimScreen — claim type selector chips (Incomplete, Unsatisfactory, Property
  Damage,
  Inappropriate Behavior), description text field, image evidence picker
  (PickMultipleVisualMedia), submit button
- [x] Build ClaimDetailScreen — view claim status, resolution notes, refund amount if approved
- [x] Show "File a Claim" button on BookingDetailScreen when status is COMPLETED
- [x] Add "Having an issue?" link on ReviewScreen that navigates to FileClaimScreen

#### Post-Service Claims (Provider)

- [x] Build provider-side FileClaimScreen — claim type chips (Dangerous Property, Exceedingly Dirty,
  Inappropriate Client), description, evidence images
- [x] Show "Report Issue with Client" button on CleanerScheduleScreen for COMPLETED bookings
- [x] Add "Having an issue?" link on LeaveClientReviewScreen navigating to FileClaimScreen

#### Support Tickets & Help Center

- [x] Build HelpCenterScreen — FAQ accordion list, "My Tickets" section with ticket history, "
  Contact
  Support" button (creates a support conversation via existing chat/message system)
- [x] Build ContextualHelpScreen — dynamically shows available actions based on booking status and
  user role (see matrix above); each action is a card/button navigating to the appropriate screen
- [x] Add prominent "Help & Support" button/FAB on BookingDetailScreen → navigates to
  ContextualHelpScreen(bookingId)
- [x] Add Help Center entry in CustomerProfileScreen and CleanerProfileScreen

#### Safety & Emergency (During Service)

- [x] Add a prominent red "🚨 Emergency / Safety Help" button on ContextualHelpScreen when booking is
  IN_PROGRESS
- [x] Creates an urgent SAFETY_EMERGENCY support ticket and optionally launches a phone dialer
  intent
  to local emergency services
- [ ] Send push notification to support team (via existing FCM infrastructure) — future enhancement

#### Data Layer

- [x] Create `SupportTicketEntity` Room entity (table `support_tickets`)
- [x] Create `ClaimEntity` Room entity (table `claims`, `evidenceImageUrls` stored as JSON string)
- [x] Create `SupportTicketDao` — insert, getByUser, getById, updateStatus, observeByUser
- [x] Create `ClaimDao` — insert, getByBooking, getByUser, getById, updateStatus, observeByUser
- [x] Add Room `MIGRATION_8_9` creating both tables; bump database version to 9
- [x] Add DTOs and endpoints to `IBackendService`: `SupportTicketDto`, `ClaimDto`,
  `CreateSupportTicketRequest`, `FileClaimRequest`, `EditBookingRequest`,
  `CancelBookingWithReasonRequest`
- [x] Implement stubs in `BackendServiceStub` with seeded demo claims and tickets
- [x] Implement Firestore collections in `FirebaseBackendService` (`support_tickets`, `claims`)
- [x] Create `ISupportRepository` interface in Repositories.kt
- [x] Create `SupportRepositoryImpl` in `data/repository/SupportRepository.kt`
- [x] Bind `ISupportRepository` in Hilt DI module

#### Navigation

- [x] Add routes to Screen.kt: `HelpCenter`, `ContextualHelp(bookingId)`, `FileClaim(bookingId)`,
  `ClaimDetail(claimId)`, `CancelBooking(bookingId)`, `EditBooking(bookingId)`,
  `SupportTicketDetail(ticketId)`
- [x] Wire all new composable destinations in AppShell.kt

### New Files

- `ui/support/HelpCenterScreen.kt` — FAQ, ticket history, contact support
- `ui/support/HelpCenterViewModel.kt` — Help center state management
- `ui/support/ContextualHelpScreen.kt` — Dynamic help actions based on booking status + role
- `ui/support/ContextualHelpViewModel.kt` — Loads booking, determines available actions
- `ui/support/FileClaimScreen.kt` — Claim submission form with evidence upload
- `ui/support/FileClaimViewModel.kt` — Claim filing state management
- `ui/support/ClaimDetailScreen.kt` — Claim status and resolution view
- `ui/support/ClaimDetailViewModel.kt` — Claim detail state management
- `ui/support/CancelBookingScreen.kt` — Cancellation reason picker and refund confirmation
- `ui/support/CancelBookingViewModel.kt` — Cancellation + refund logic
- `ui/support/EditBookingScreen.kt` — Edit address and service type for PENDING bookings
- `ui/support/EditBookingViewModel.kt` — Booking edit state management
- `data/repository/SupportRepository.kt` — ISupportRepository implementation

### Modified Files

- `core/domain/model/DomainModels.kt` — SupportTicket, Claim, all new enums, NotificationType
  additions
- `core/domain/repository/Repositories.kt` — ISupportRepository interface
- `data/local/entity/Entities.kt` — SupportTicketEntity, ClaimEntity
- `data/local/dao/Daos.kt` — SupportTicketDao, ClaimDao
- `data/local/AppDatabase.kt` — Version 9, MIGRATION_8_9, new DAOs registered
- `data/remote/backend/IBackendService.kt` — Support/claim DTOs and endpoints
- `data/remote/backend/BackendServiceStub.kt` — Seeded demo claims and support tickets
- `data/remote/backend/FirebaseBackendService.kt` — Firestore support_tickets and claims collections
- `data/mapper/Mappers.kt` — SupportTicket and Claim entity↔domain mappers
- `ui/navigation/Screen.kt` — 7 new routes
- `ui/shell/AppShell.kt` — New composable destinations, title mappings
- `ui/booking/BookingsListScreen.kt` (BookingDetailScreen) — Help FAB, Cancel/Edit/Claim buttons
  (contextual by status)
- `ui/review/ReviewScreen.kt` — "Having an issue?" link → FileClaim
- `ui/review/LeaveClientReviewScreen.kt` — "Having an issue?" link → FileClaim (provider side)
- `ui/cleaner/CleanerScheduleScreen.kt` — "Report Issue with Client" button on completed bookings
- `ui/customer/CustomerProfileScreen.kt` — Help Center quick-link card
- `ui/cleaner/CleanerProfileScreen.kt` — Help Center quick-link card
- `app/di/Modules.kt` — ISupportRepository binding

### Implementation Order

1. Domain models and enums (core)
2. Room entities, DAOs, migration (data)
3. Backend DTOs, endpoints, stubs (data)
4. Repository interface + implementation (core + data)
5. DI wiring (app)
6. CancelBookingScreen + EditBookingScreen (simplest flows)
7. FileClaimScreen + ClaimDetailScreen
8. ContextualHelpScreen (ties everything together)
9. HelpCenterScreen (FAQ + ticket history)
10. Safety/emergency flow
11. Integration into BookingDetailScreen, review screens, profile screens
12. Seed demo data and test

### Tests

- [ ] Cancellation validation tests (only PENDING/ACCEPTED allowed)
- [ ] Edit booking validation tests (only PENDING allowed)
- [ ] Claim filing and status transition tests
- [ ] Contextual help action resolution tests (correct actions per status + role)
- [ ] Refund processing tests
- [ ] Support ticket creation and chat integration tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

> ⚠️ **Known gaps (audit April 26, 2026)**:
> - **14 `FirebaseBackendService` methods** (lines 485–524)
    return `Result.Error("not yet implemented")`
    > for support tickets, claims, cancel-with-reason, edit booking, refunds, help articles, and
    saved
    > locations. All Phase 15 UI is functional with the stub backend but **silently fails with
    Firebase**.
> - `SupportRepository`, `ClaimRepository` are untested — no unit tests exist.

---

## Phase 16: Detailed Cleaning and Location Options

Richer service configuration for both clients and cleaners.

### Tasks

#### Cleaning Types (Client side)

- [ ] Define expanded CleaningType enum: Standard, Deep Clean, End of Tenancy, Post-Construction,
  Carpet and Upholstery, Window Cleaning, Oven and Appliance, Move-In/Move-Out, Office/Commercial
- [ ] Update BookingFormScreen with a visual grid chip selector for cleaning types
- [ ] Store and sync selected cleaning type with bookings

#### Location Types (Client side)

- [ ] Define LocationType model: Apartment, House, Studio, Office, Retail, Warehouse,
  Airbnb/Short-Term Rental, Other
- [ ] Add location type selector to BookingFormScreen and CustomerProfileScreen
- [ ] Include room count, bathroom count, and approximate square footage inputs
- [ ] Allow clients to save multiple named locations (e.g. Home, Office) in their profile

#### Areas of Specialization (Cleaner side)

- [ ] Define Specialization model mirroring the expanded cleaning types above
- [ ] Update ServiceManagementScreen to allow cleaners to select and rank their specializations
- [ ] Show specialization badges on ProviderSearchScreen provider cards
- [ ] Update FilterSheet to filter providers by specialization
- [ ] Update CleanerProfileScreen to display specializations prominently
- [ ] Sync specializations with backend and Firestore

#### General

- [ ] Update IBackendService DTOs, BackendServiceStub, and FirebaseBackendService for new fields
- [ ] Add Room migration v9 to v10 for new columns on bookings, client profiles, and provider
  profiles
- [ ] Update search and matching logic to factor in cleaning type and location type compatibility
- [ ] Seed rich demo data using the expanded types

### New Files

- `ui/components/CleaningTypeSelector.kt` - Reusable grid chip selector for cleaning types
- `ui/components/LocationTypeSelector.kt` - Location type and room detail input component
- `ui/profile/SavedLocationsScreen.kt` - Client saved locations management screen

### Modified Files

- `core/domain/model/DomainModels.kt` - CleaningType enum, LocationType model, Specialization model
- `data/local/entity/Entities.kt` - New columns for cleaning type, location type, specializations
- `data/local/AppDatabase.kt` - Version 10, MIGRATION_9_10
- `data/remote/backend/IBackendService.kt` - Updated DTOs with new fields
- `data/remote/backend/BackendServiceStub.kt` - Rich seeded demo data with expanded types
- `data/remote/backend/FirebaseBackendService.kt` - Firestore support for new fields
- `ui/booking/BookingFormScreen.kt` - CleaningTypeSelector and LocationTypeSelector integration
- `ui/booking/ProviderSearchScreen.kt` - Specialization badges on provider cards
- `ui/booking/BookingViewModel.kt` - CleaningType and LocationType in form state
- `ui/components/FilterSheet.kt` - Specialization filter option
- `ui/cleaner/ServiceManagementScreen.kt` - Specialization selection and ranking
- `ui/cleaner/CleanerProfileScreen.kt` - Specializations section
- `ui/customer/CustomerProfileScreen.kt` - Saved locations section

### Tests

- [ ] Cleaning type selection and persistence tests
- [ ] Location type and room detail tests
- [ ] Specialization filter and search tests

**Estimated Duration**: 2-3 weeks

---

## Phase 17: Error Handling & Analytics
Comprehensive error handling and monitoring.

### Tasks
- [ ] Create error logging system
- [ ] Integrate Firebase Analytics
- [ ] Track key user actions
- [ ] Create crash reporting
- [ ] Build error recovery flows
- [ ] Add user-friendly error messages
- [ ] Implement error tracking dashboard
- [ ] Create bug reporting feature
- [ ] Add performance monitoring

### Tests
- [ ] Error handling tests
- [ ] Analytics event tests
- [ ] Crash reporting tests

**Estimated Duration**: 2 weeks

---

## Phase 18: Testing & Optimization
Comprehensive testing, optimization, and release prep.

### Tasks
- [ ] Achieve 60%+ code coverage
- [ ] Write integration tests
- [ ] Performance testing and optimization
- [ ] Memory leak detection
- [ ] Battery drain analysis
- [ ] Network bandwidth optimization
- [ ] Database query optimization
- [ ] Compose rendering optimization
- [ ] APK size reduction
- [ ] Accessibility compliance (a11y)

### Tests
- [ ] Unit test coverage expansion
- [ ] Integration tests
- [ ] E2E tests
- [ ] Performance benchmarks
- [ ] Accessibility tests

**Estimated Duration**: 3 weeks

---

## Phase 19: Release Preparation
Finalization and app store submission.

### Tasks
- [ ] Set up CI/CD pipeline
- [ ] Configure release build signing
- [ ] Create privacy policy
- [ ] Write terms of service
- [ ] Prepare app store screenshots
- [ ] Write app description
- [ ] Create user onboarding
- [ ] Build in-app help/tutorial
- [ ] Set up crash analytics
- [ ] Prepare for beta testing

**Estimated Duration**: 2 weeks

---

## Phase 20: Beta Testing & Refinement
Beta testing with real users.

### Tasks
- [ ] Set up beta testing program
- [ ] Gather user feedback
- [ ] Fix discovered issues
- [ ] Optimize based on metrics
- [ ] Add requested features
- [ ] Final security audit
- [ ] Performance benchmarking
- [ ] Final testing pass

**Estimated Duration**: 3-4 weeks

---

## Phase 21: Launch
App store submission and monitoring.

### Tasks
- [ ] Submit to Google Play Store
- [ ] Monitor crash rates
- [ ] Track key metrics
- [ ] Handle user support
- [ ] Push hotfixes if needed
- [ ] Plan for 1.1 features

**Ongoing**

---

## Summary

**Total Estimated Timeline**: 5-7 months

### Current Status: **Phases 1–15 built; ~5 are "structural, not working"** *(re-assessed 2026-06-25)*

(Architecture, Authentication, Booking, Provider Mgmt, Reviews, Payments, Navigation & App Flow,
Firebase
Integration, Maps & Location, Sync & Offline Features, In-App Chat, Subscriptions, Multi-Language,
Reverse Reviews, Help/Support & Claims)

> ⚠️ **Tech-Lead Review (2026-06-25)** — see [`TECH_LEAD_REVIEW.md`](TECH_LEAD_REVIEW.md):
> The codebase **compiles** (the committed `*_build.txt` failures are stale Windows runs; every
> error they cite is already fixed) and demos well on the mock backend, but "structurally complete"
> ≠ "working." Phases re-tagged `✅ STRUCTURAL ⚠️` above have material gaps:
> - **Offline-write/sync queue is dead code** (`queueOperation()` never called) — Phase 10 overstated.
> - **Multi-language is inert** — `stringResource` in 1/83 UI files; ~350 hardcoded `Text` literals — Phase 13.
> - **`release` routes to a Firebase backend with 14 unimplemented methods** + placeholder config —
>   Phases 8 & 15 silent-fail in production.
> - **Security pre-production:** plaintext auth token, no Firestore rules, payments `SUCCEEDED` with
>   no PSP — Phase 6.
> - **Real bugs:** recurring-booking VM hangs forever; chat drops offline messages; `createReview`
>   loses `direction`.
>
> *Corrections to the prior (April 2026) audit:* locale switching IS now wired; `BookingRepositoryTest`
> is no longer a no-op; real test coverage is well above the claimed 15–20%; `getReviewsForProvider`
> is filtered (not a full scan).

### Next Immediate Steps:

**Follow the 🧭 Remediation Roadmap at the top of this file (Tracks A→D), not the linear
Phase 16→21 march.** In short:

1. **Track A** — make the docs true + fix the two outright bugs + add CI.
2. **Track B** — externalize strings, real icons, accessibility.
3. **Track C** — implement the Firebase backend + security + signing **before** `USE_MOCK_BACKEND=false`.
4. **Track D** — test the sync/worker/DataStore core; folds into Phases 17–18.
5. Resume Phase 16 (Detailed Cleaning & Location Options) once Tracks A–B are clear.

### Architecture Strengths *(confirmed by review)*

- Clean, acyclic module graph with a framework-free `:core` and real dependency inversion
- Read-side offline-first genuinely implemented (cache-first reads, network-failure fallback)
- Swappable backend (`IBackendService`/`IAuthService`) selected via `BuildConfig`
- Sound coroutine hygiene (no `GlobalScope`/`runBlocking`; listeners scoped via `awaitClose`)
- 9 proper additive Room migrations; broad, real test suite (49 test files)

### Risk Areas to Monitor
- **Offline-write correctness** — queue is currently bypassed; writes are dropped, not retried
- **Real-backend parity** — Stub implements features the Firebase impl stubs out; debug ≠ release
- Sync conflict resolution edge cases (untested — no `SyncManager` tests)
- Payment integration security (no PSP today)
- `:ui`→`:data` concrete coupling (`ConnectivityObserver`) hurting testability
- Firestore query scalability (`searchProviders`/`getConversationsForUser` full scans) at scale

### Future Enhancements
- [ ] Machine learning for provider matching
- [ ] Predictive scheduling
- [ ] Advanced analytics dashboard
- [ ] Admin panel for moderation
- [ ] A/B testing framework
