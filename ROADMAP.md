# Implementation Roadmap

## 🧭 Remediation Roadmap — supersedes "Next Steps" *(Tech-Lead Review, 2026-06-25)*

> The original Phase 1–21 plan below tracks **what was built**. An independent review
> ([`TECH_LEAD_REVIEW.md`](TECH_LEAD_REVIEW.md)) found that several phases marked "✅ COMPLETE" are
> **structurally complete but not functionally working** (re-tagged `✅ STRUCTURAL ⚠️` inline). The
> architecture is strong; the gap is "compiles & demos on the mock backend" vs "works against real
> Firebase." This section is the corrected, evidence-based work order. Each item cites the review
> section that substantiates it. **Do these before resuming the linear Phase 17→21 march**
> (Phase 16 shipped in the meantime — see its corrected entry below).

**Last verified: 2026-07-27, against `develop` @ `4892952`.** Status at that commit:
Track A ✅ **done** except one open decision · Track B ✅ **done** (string externalization landed in
PRs #7–#17); the lint gate and icon/lifecycle items are still open · Track C ❌ **entirely
untouched**, and it now has a newly-found blocker at the top · Track D 🟡 **partial** ·
Track E ❌ **new, open**.

### Track A — Make the docs true (cheap, high trust) — ✅ done bar one decision
- [ ] 🔴 **DECISION REQUIRED (tech lead):** the offline-write story. `SyncManager.queueOperation()`
  still has **zero production call sites** — every write repo hard-fails when offline instead of
  enqueuing, so the backoff/conflict machinery processes a table nothing populates. Two honest
  options: (a) wire `queueOperation()` into the write repos and make offline writes real, or
  (b) delete the queue/backoff/conflict code and stop advertising offline writes. This is a product
  call, not a coding task that can be started blind — everything else in Track A is finished, so it
  is the only thing keeping the track open. *(§4)*
- [x] Fix `RecurringBookingViewModel.getCurrentSession()` infinite-suspend → now uses `.first()`
  (`ui/subscription/RecurringBookingViewModel.kt:193`). *(§3.3)*
- [x] Fix `MessageRepository.sendMessage` offline message loss — it now persists locally **and**
  returns `Result.Error(OfflineException)` when offline, and propagates backend errors instead of
  reporting success (`data/repository/MessageRepository.kt:131–150`). *(§3.4)*
- [x] Correct stale/inaccurate doc claims (§6): locale switching IS wired; `BookingRepositoryTest`
  is no longer a no-op; the "15–20% coverage" figure and the "`getReviewsForProvider` is a full
  scan" claim are retracted; "encrypted session storage" corrected to **plaintext** everywhere.
- [x] Add CI — `.github/workflows/ci.yml` runs `./gradlew assembleDebug allUnitTests` on every PR to
  `develop` and on pushes to `develop`, on JDK 17, with test reports uploaded. *(§7)*
  *(The CI job has real gaps of its own — it never compiles or runs `androidTest`, runs no lint and
  no coverage gate. Those are tracked in **Track E**, not here.)*

### Track B — Make it actually multilingual & accessible — ✅ externalization done, gate still open
- [x] Externalize the hardcoded `Text("…")` literals to `stringResource(R.string.*)`. **Done:**
  `stringResource` is now used in **46 of the 83** `:ui/src/main` Kotlin files across **608**
  references. `ui/src/main/res/values/strings.xml` holds **531 `<string>` + 16 `<plurals>`** entries,
  key-complete across **5 locales** (`values`, `values-fr`, `values-es`, `values-pt`, `values-ar`).
  The old "1 of 83 files / ~350 hardcoded literals" figure is obsolete. *(§3.2)*
- [x] Localize `contentDescription`s — every `Icon` `contentDescription` now resolves through a
  `cd_*` string key.
- [ ] Add a lint rule + baseline that fails the build on **new** hardcoded UI strings. **Not started
  — no lint rule, no baseline, and no lint step in CI exists anywhere in the repo.** Without it the
  ~67 residual `Text("…")` literals (almost all non-translatable glyphs: emoji, `"$"`, `"›"`) can
  quietly grow back.
- [ ] Replace emoji-as-icons with `androidx.compose.material.icons.*` + `contentDescription`. *(§4)*
- [ ] Adopt `collectAsStateWithLifecycle` — **0 uses** today vs **87 `collectAsState()`** call sites
  across 44 files. *(§4)*

### Track C — Before flipping `USE_MOCK_BACKEND=false` (Firebase go-live gate) — ❌ entirely open
- [ ] 🔴 **BLOCKER, found 2026-07-27 — every Firestore *read* fails today.** Every DTO in
  `data/remote/backend/IBackendService.kt` is a `data class` whose constructor parameters are all
  **required** (no defaults) — e.g. `ClientDto:113`, `BookingDto:162` — so Kotlin generates **no
  no-arg constructor**. Firestore's object mapper requires one. `FirebaseBackendService` calls
  `toObject()`/`toObjects()` **26 times**, so every read throws and is swallowed into a
  `Result.Error` by the `firestoreCall` wrapper, while writes succeed. Flipping
  `USE_MOCK_BACKEND=false` today therefore yields a **write-only app**: data goes in and nothing
  comes back. Fix: give every DTO defaults for all constructor params (or hand-write the mapping).
  **Nothing else in this track is worth doing until this is fixed** — the other items are invisible
  behind it.
- [ ] Implement the 14 `FirebaseBackendService` stubs at `FirebaseBackendService.kt:484–524` (support
  tickets, claims, cancel-with-reason, edit booking, refunds, help articles, saved locations) + the
  repo stubs (`getCurrentClient`, `updateProvider`, `getPaymentsForProvider`, `updatePaymentStatus`,
  `updateService`). *(§3.1, §4)*
- [ ] Fix the silent data drops on the Firebase path: `createReview` still discards `direction` and
  `locationTags`; `createBooking` hardcodes `totalPrice = 0.0` and `estimatedDuration = 60`;
  `FirebaseAuthService.signup` creates no Firestore profile doc and discards the `role` argument;
  the FCM token is written to DataStore but never registered server-side (`observeFcmToken` has
  **0 callers**, so targeted push cannot work); `searchProviders` ignores `latitude`/`longitude`/
  `radius` entirely. *(§3.5, §4)*
- [ ] **Security:** the session store is **plaintext** DataStore holding the auth token and PII
  (`data/local/preferences/PreferencesDataStore.kt:16`) — encrypt it; set `allowBackup=false` (or
  exclude DataStore — `android:allowBackup="true"` currently ships with *empty* backup rule files);
  **write and deploy Firestore Security Rules — the repo contains none at all** (no
  `firestore.rules`, `firebase.json` or `.firebaserc`); integrate a real PSP — payments today
  fabricate `status = "SUCCEEDED"` client-side (`FirebaseBackendService.kt:267`) with no processor,
  and `PaymentScreen.kt` collects card PAN/CVV in **unmasked** `OutlinedTextField`s with no PSP SDK
  and no `FLAG_SECURE`. *(§3.6)*
- [ ] Release configuration: `app/google-services.json` is still the placeholder
  (`project_number "000000000000"`, `api_key "placeholder-key-for-testing"`); there is **no
  `signingConfig` anywhere**; `applicationId` is still `com.example.diamonds` (Play rejects
  `com.example.*`); `isMinifyEnabled = false` and `proguard-rules.pro` is the untouched empty
  template; supply a real `MAPS_API_KEY`; add Coil. *(§4, §5)*

### Track D — Hardening (folds into Phase 17/18) — 🟡 partial
**Landed:**
- [x] `SyncManagerTest.kt` — 21 tests covering queue/cancel/retry/conflict-resolution and the
  `processSyncQueue` dispatch paths, plus the correctness fixes they surfaced (PRs #19–#21).
- [x] `LocationRepositoryTest.kt` — 8 tests (Haversine distance identities, symmetry, a known city
  pair, ETA).
- [x] Boilerplate `Example*Test` files removed — **0** remain.

**Still open:**
- [ ] No tests at all for `SyncWorker`, `RecurringBookingWorker`, `PreferencesDataStore` or
  `ConnectivityObserver`.
- [ ] **Room schema export is broken:** `AppDatabase` declares `exportSchema = true` but no
  `room.schemaLocation` is configured and no `schemas/` directory exists, so all **9** migrations
  (`MIGRATION_1_2`…`MIGRATION_9_10`) are unverified and the declared `room-testing` dependency is
  dead weight. Configure the schema location, commit the JSONs, then add migration tests.
- [ ] **Turbine is declared but unused** — `testImplementation(libs.turbine)` in both
  `data/build.gradle.kts` and `ui/build.gradle.kts`, **0** imports in any test. Either use it to
  assert state transitions or drop the dependency.
- [ ] No `DispatcherProvider` — **0** references repo-wide; tests still lean on `MainDispatcherRule`
  plus direct `runTest`. Introduce one alongside a `:core` connectivity interface (`:ui` still
  couples to the concrete `:data` `ConnectivityObserver`).
- [ ] Fix the captive-portal `NET_CAPABILITY_VALIDATED` check; cancel app/service coroutine scopes;
  apply the `kotlin-serialization` plugin to `:core`. *(§4, §5)*
- [ ] Drop or actually use `ACCESS_BACKGROUND_LOCATION` **and** `POST_NOTIFICATIONS` — both are
  declared in the manifest but never requested or used at runtime.

### Track E — Test-infrastructure & error-handling defects *(new, found 2026-07-27)* — ❌ open
These were invisible to every previous review because CI never exercises the instrumentation suite.
- [ ] **CI does not build or run `androidTest`.** The workflow runs only `assembleDebug allUnitTests`
  — no `assembleDebugAndroidTest`, no lint, no coverage gate — which is exactly why the two defects
  below shipped unnoticed. `allUnitTests` also aggregates `:app` and `:common`, **neither of which
  has a `src/test` directory**, and excludes `:core` entirely; only `:data` (12 files) and `:ui`
  (14 files) actually contribute. At minimum add `assembleDebugAndroidTest` so the instrumentation
  suite is compile-checked on every PR.
- [ ] **`LoginScreenTest` is broken by the Track B externalization.**
  `app/src/androidTest/.../ui/LoginScreenTest.kt:153` asserts the text
  `"Demo accounts  (tap to fill)"` (**two** spaces), but the shipped
  `R.string.demo_accounts_hint` is `"Demo accounts (tap to fill)"` (**one** space). The test fails
  on a device today. Fix the assertion — better, assert against the string resource.
- [ ] **The `androidTest` Hilt graph is incomplete.**
  `app/src/androidTest/.../di/FakeRepositoryModule.kt` `@TestInstallIn`-replaces `RepositoryModule`
  wholesale with 13 `@Provides`, but omits `ISavedLocationRepository`, which production
  `app/di/Modules.kt` does provide. Any instrumentation test that reaches a saved-locations
  dependency fails to construct its graph.
- [ ] **`SavedLocationRepository` reports failed writes as success.**
  `data/repository/SavedLocationRepository.kt:32` maps `Result.Error → Result.Success(emptyList())`
  and `:45` maps `Result.Error → Result.Success(location)`, so a failed remote call surfaces to the
  UI as a success. This is the same silent-data-loss class as the `MessageRepository` bug fixed in
  Track A, and it has no tests.
- [ ] **37 `as? Result.Success` sites across 10 ViewModel files** collapse `Result.Error` into `null`
  with no user-visible error, contradicting the documented three-arm `Result` convention
  (`Success`/`Error`/`Loading`). Convert them to exhaustive `when` blocks that surface errors.

**Gate:** treat Tracks A–C as exit criteria for "production candidate." The linear roadmap's
Phase 19 (Release Prep) cannot truthfully start until Track C is done. Track E is cheap and should
be folded into the next PR touching CI.

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
> [TECH_LEAD_REVIEW.md §3.5](TECH_LEAD_REVIEW.md#35-createreview-never-persists-direction-breaks-review-separation).

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
> swap-in. See [TECH_LEAD_REVIEW.md §3.6](TECH_LEAD_REVIEW.md#36-security-must-fixes-before-firebase-go-live).

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
> ⚠️ **Review 2026-06-25, re-verified 2026-07-27 — worse than first reported.** The Firebase path is
> materially incomplete and `release` builds enable it (`USE_MOCK_BACKEND=false`): **14
> `FirebaseBackendService` methods return `Result.Error("… not yet implemented")`**
> (`FirebaseBackendService.kt:484–524`), `google-services.json` is a placeholder, `signup` never
> writes a Client/Provider profile doc (and discards the `role` argument), and the FCM token is saved
> locally but never registered server-side (`observeFcmToken` has 0 callers, so targeted push can't
> work). See [TECH_LEAD_REVIEW.md §3.1, §4](TECH_LEAD_REVIEW.md#31-the-release-build-routes-to-an-unimplemented-firebase-backend).
>
> 🔴 **New, 2026-07-27 — this is the real blocker.** Even the *implemented* Firebase methods cannot
> read. Every DTO in `IBackendService.kt` has required (no-default) constructor params, so Kotlin
> emits no no-arg constructor and Firestore's object mapper cannot deserialize; all 26
> `toObject()`/`toObjects()` calls throw and are swallowed into `Result.Error` by `firestoreCall`.
> With `USE_MOCK_BACKEND=false` the app writes fine and reads **nothing**. Fix this first —
> **Track C, item 1**.

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
- [x] Distance calculation tests — `data/src/test/.../LocationRepositoryTest.kt` (8 tests: Haversine
  identities, symmetry, a known city pair, ETA)

**Status**: ✅ COMPLETE

**Estimated Duration**: 2-3 weeks

---

## Phase 10: Sync & Offline Features ✅ STRUCTURAL ⚠️
Complete offline-first implementation and sync.
> ⚠️ **Review 2026-06-25, re-verified 2026-07-27 — still open.** The sync engine is **dead code**:
> `SyncManager.queueOperation()` has zero production call sites. Every write path hard-fails when
> offline instead of enqueuing, so the backoff/conflict-resolution machinery processes a table that
> is never populated. Read-side offline-first is real; the offline-**write** story advertised here
> does not run. The engine is now properly unit-tested (`SyncManagerTest.kt`, 21 tests — PRs
> #19–#21) and several correctness bugs were fixed, but *tested dead code is still dead code*. The
> decision — wire the queue into write repos, or delete it and correct the docs — is the one
> remaining Track A item. See [TECH_LEAD_REVIEW.md §4](TECH_LEAD_REVIEW.md#4-important-findings-p2).

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
- [x] Sync queue tests — `data/src/test/.../sync/SyncManagerTest.kt` (21 tests: queue/cancel/retry,
  conflict resolution, `processSyncQueue` dispatch, retry-limit and unroutable-entity failure paths)
- [ ] Connectivity change tests — `SyncManagerTest` covers "does nothing when offline", but
  `ConnectivityObserver` and `ConnectivitySyncTrigger` themselves remain untested
- [ ] Offline data preservation tests
- [ ] `SyncWorker` scheduling tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

---

## Phase 11: In-App Chat System ✅ STRUCTURAL ⚠️
> ✅ **Fixed 2026-07 (Track A).** The 2026-06-25 review found that `MessageRepository.sendMessage`
> reported `Result.Success` for messages composed offline or on a transient backend failure but
> never sent or retried them — silent chat **data loss**. As of `develop` @ `4892952` the message is
> persisted locally *and* the call returns `Result.Error(OfflineException)` when offline, and
> backend errors are propagated rather than swallowed (`MessageRepository.kt:131–150`), with
> regression tests in `MessageRepositoryTest.kt`. See
> [TECH_LEAD_REVIEW.md §3.4](TECH_LEAD_REVIEW.md#34-messagerepositorysendmessage-silently-loses-messages-sent-offline).
>
> The phase stays `STRUCTURAL ⚠️` for a different reason: the caller now *knows* a message failed,
> but there is still no retry/pending-outbox — that depends on the open Track A sync-queue decision.
> The Firestore chat collections are also subject to the Track C read blocker.

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
- `app/fcm/DiamondsFcmService.kt` - New message push notification handling
- `app/di/Modules.kt` - IMessageRepository binding

### Tests

- [x] Message repository tests — `data/src/test/.../MessageRepositoryTest.kt` (16 tests, including
  the offline/backend-error regressions from the Track A fix)
- [x] Chat ViewModel tests — `ui/src/test/.../chat/ChatViewModelTest.kt` (13 tests)
- [ ] Real-time listener tests (`FirestoreBookingListener` / chat snapshot listeners)

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

**Estimated Duration**: 2-3 weeks

---

## Phase 12: Subscription and Recurring Bookings ✅ STRUCTURAL ⚠️
> ✅ **Fixed 2026-07 (Track A).** The 2026-06-25 review found that
> `RecurringBookingViewModel.getCurrentSession()` collected a never-completing DataStore flow with
> `return@collect` — which only returns the lambda, so `collect()` never returned and the suspend
> function **hung forever**, breaking recurring-booking submit and load. As of `develop` @ `4892952`
> it uses `authRepository.getCurrentUserSession().first()`
> (`ui/subscription/RecurringBookingViewModel.kt:193`), with a regression test
> (`submitRecurringBooking proceeds when session flow never completes`) and null-session handling.
> See [TECH_LEAD_REVIEW.md §3.3](TECH_LEAD_REVIEW.md#33-recurringbookingviewmodelgetcurrentsession-suspends-forever).
>
> The phase stays `STRUCTURAL ⚠️` because `RecurringBookingWorker` has no tests and the Firestore
> `recurringBookings` collection is subject to the Track C read blocker.

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

- [x] Subscription repository tests — `data/src/test/.../SubscriptionRepositoryTest.kt` (16 tests:
  offline-write errors, cache-first reads, status updates, schedule updates, `advanceNextBookingDate`)
- [x] Recurring booking ViewModel tests — `ui/src/test/.../subscription/RecurringBookingViewModelTest.kt`
  (15 tests: setup state, submit success/failure, pause/resume/cancel, never-completing session flow)
- [ ] Recurring booking *generation* tests (`RecurringBookingWorker` — untested)
- [ ] WorkManager scheduling tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

**Estimated Duration**: 2-3 weeks

---

## Phase 13: Multi-Language Support ✅ COMPLETE
> ✅ **Resolved 2026-07 (Track B, PRs #7–#17). Multi-language is no longer inert.** The 2026-06-25
> review found `stringResource` used in **1 of 83** UI files, with ~350 hardcoded English `Text("…")`
> literals, so switching locale changed almost nothing on screen. As of `develop` @ `4892952`:
> `stringResource` is used in **46 of the 83** `:ui/src/main` Kotlin files across **608** references;
> `ui/src/main/res/values/strings.xml` carries **531 `<string>` + 16 `<plurals>`** entries and every
> one of the five locale folders (`values`, `values-fr`, `values-es`, `values-pt`, `values-ar`) is
> key-complete at 531 + 16; `app/src/main/res` adds a further 169 strings per locale; and all `Icon`
> `contentDescription`s resolve through `cd_*` keys. Switching language now visibly re-renders the
> app. See [TECH_LEAD_REVIEW.md §3.2](TECH_LEAD_REVIEW.md#32-user-facing-strings-are-hardcoded--the-multi-language-feature-does-not-work).
>
> **Remaining (tracked in Track B, not blocking this phase):** ~67 residual `Text("…")` literals —
> almost all non-translatable glyphs (emoji, `"$"`, `"›"`) — and **no lint rule or baseline** to stop
> new hardcoded strings creeping back in.

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
- [ ] Add a lint rule/baseline so new hardcoded strings fail the build *(Track B)*

### New Files

- `ui/settings/LanguageSelectorScreen.kt` — In-app language picker screen with flag + radio buttons
- `ui/settings/LanguageViewModel.kt` — Language preference state management via PreferencesDataStore
- `ui/src/main/res/values/strings.xml` — All UI strings extracted (531 `<string>` + 16 `<plurals>`)
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

- `app/src/main/res/values/strings.xml` — App-module strings (169 entries, mirrored in all 4 locales)
- `data/local/preferences/PreferencesDataStore.kt` — LANGUAGE_KEY, saveLanguage(), observeLanguage()
- `ui/navigation/Screen.kt` — LanguageSelector route
- `ui/shell/AppShell.kt` — Language selector destination, title mapping, wired onLanguage callbacks
- `ui/customer/CustomerProfileScreen.kt` — onLanguage callback, 🌐 Language quick-link card
- `ui/cleaner/CleanerProfileScreen.kt` — onLanguage callback, 🌐 Language card in profile
- `app/MainActivity.kt` — Switched from ComponentActivity to AppCompatActivity for per-app locale
- `app/AndroidManifest.xml` — android:localeConfig for per-app language (Android 13+)

### Tests

- [ ] String resource completeness tests (no missing keys per locale) — key-parity was verified by
  hand on 2026-07-27 (531 + 16 in all five folders) but nothing enforces it automatically
- [ ] RTL layout tests
- [ ] Date and time formatting tests per locale

**Status**: ✅ COMPLETE — see the phase callout above

> 📎 **History (both older notes are now resolved — kept for the record).**
>
> <details><summary>2026-06-25 tech-lead note (superseded by the Track B work above)</summary>
>
> > Runtime locale switching **is** wired — `LanguageSelectorScreen` calls
> > `AppCompatDelegate.setApplicationLocales()`. The real remaining gap is that the UI hardcodes
> > strings, so switching locale has almost no visible effect.
> > *Resolved 2026-07: 46/83 UI files now use `stringResource`; see the phase callout.*
> </details>
>
> <details><summary>Original April 26, 2026 audit note (resolved 2026-06)</summary>
>
> > `LanguageViewModel` saves the locale code to DataStore but nothing reads it to reconfigure the
> > app locale. `AppCompatDelegate.setApplicationLocales()` must be called for runtime switching to
> > actually work.
> </details>

**Estimated Duration**: 2 weeks

---

## Phase 14: Reverse Reviews ✅ STRUCTURAL ⚠️

Allow cleaners to review clients and their locations after a job.

> 📋 **Reconciled 2026-07-27:** this phase used to declare `**Status**: ✅ COMPLETE` while every
> checkbox below was unticked. The boxes have been ticked against source; the one item that was
> never built (the client-rating badge on `BookingRequestCard`) is left unchecked, and the phase is
> re-tagged `STRUCTURAL ⚠️` because the Firebase path still drops `direction` and `locationTags`
> (see the known-gap note at the end of this phase and Track C).

### Tasks

- [x] Extend Review domain model to support direction: CLIENT_REVIEWS_PROVIDER and
  PROVIDER_REVIEWS_CLIENT (`ReviewDirection` enum, `DomainModels.kt:201`)
- [x] Add reviewDirection field to ReviewEntity and update Room migration v7 to v8 (`MIGRATION_7_8`
  adds `reviewDirection` + `locationTags`)
- [x] Update IBackendService, BackendServiceStub, and FirebaseBackendService for bidirectional
  reviews — *stub complete; the Firebase write still drops `direction`/`locationTags`*
- [x] Update ReviewRepository to handle both review directions
  (`getReviewForBookingByDirection`, direction-filtered queries)
- [x] Build ClientReviewViewModel - load booking context, check if cleaner has already reviewed
- [x] Build LeaveClientReviewScreen - star picker, written feedback, optional location tags (e.g.
  Easy parking, Clear instructions, Pet-friendly)
- [x] Build ClientRatingsScreen - customer average rating and review history visible to cleaners
- [ ] Show client rating badge on BookingRequestCard in CleanerBookingRequestsScreen — **not built**;
  `CleanerBookingRequestsScreen.kt` contains no rating UI. (`CleanerScheduleScreen` *does* expose an
  `onViewClientRatings` tap target, so the entry point exists elsewhere.)
- [x] Add Review Client button in CleanerScheduleScreen for COMPLETED bookings (`onReviewClient`)
- [x] Prevent duplicate reverse reviews — `ClientReviewViewModel` loads any `existingReview` for the
  booking + direction and renders read-only
- [x] Seed demo reverse review data for demo accounts (4 seeded `PROVIDER_REVIEWS_CLIENT` reviews in
  `BackendServiceStub`)

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

**Status**: ✅ STRUCTURAL ⚠️ — works on the stub backend; see the gap below

> ⚠️ **Known gap (audit April 26, 2026 — re-verified 2026-07-27, still open)**:
> `FirebaseBackendService.createReview` does not write the `direction` field (nor `locationTags`) to
> Firestore. The `ReviewDirection` enum and the Room columns exist correctly, but the values are
> silently dropped on the Firebase backend, so every review would read back as
> `CLIENT_REVIEWS_PROVIDER`. Fix: include `direction` and `locationTags` in the `ReviewDto` Firestore
> write. Tracked in **Track C**.

---

## Phase 15: Help, Support & Claims System ✅ STRUCTURAL ⚠️
> ⚠️ **Review 2026-06-25:** Fully implemented in the Stub (so it works in debug), but **every**
> backing Firebase method is one of the 14 `"not yet implemented"` stubs — support tickets, claims,
> cancel-with-reason, edit-booking, refunds, help articles and saved locations all silent-fail in a
> `release` build. See [TECH_LEAD_REVIEW.md §3.1](TECH_LEAD_REVIEW.md#31-the-release-build-routes-to-an-unimplemented-firebase-backend).

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

- [x] Cancellation flow tests — `ui/src/test/.../support/CancelBookingViewModelTest.kt` (9 tests:
  load, reason selection, missing-reason error, success, repository failure, notes handling) and
  `SupportRepositoryTest` `cancelBookingWithReason` success/error cases
- [x] Edit booking tests — `SupportRepositoryTest` `editBooking` success/error cases
- [x] Claim filing tests — `SupportRepositoryTest` `fileClaim` / `getClaim` / `getClaimsForUser` /
  `getClaimForBooking` / `observeClaimsForUser`
- [x] Refund processing tests — `SupportRepositoryTest` `requestRefund` success/error cases
- [x] Support ticket creation tests — `SupportRepositoryTest` `createSupportTicket`,
  `getSupportTicket`, `getTicketsForUser` (incl. cache fallback), `observeTicketsForUser`
  *(25 tests in total across `data/src/test/.../SupportRepositoryTest.kt`)*
- [ ] Status-transition tests for claims/tickets (`UNDER_REVIEW → APPROVED/REJECTED → RESOLVED`)
- [ ] Contextual help action resolution tests (correct actions per status + role) —
  `ContextualHelpViewModel` is untested
- [ ] Support-ticket ↔ chat integration tests

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

> ⚠️ **Known gaps (audit April 26, 2026 — re-verified 2026-07-27)**:
> - **14 `FirebaseBackendService` methods** (`FirebaseBackendService.kt:484–524`) still return
>   `Result.Error("… not yet implemented")` for support tickets, claims, cancel-with-reason, edit
>   booking, refunds, help articles and saved locations. All Phase 15 UI is functional against the
>   stub backend but **silently fails with Firebase**. Tracked in **Track C**.
> - ~~`SupportRepository` is untested~~ — **resolved**: `SupportRepositoryTest.kt` now covers it with
>   25 tests. `ContextualHelpViewModel`, `FileClaimViewModel`, `ClaimDetailViewModel`,
>   `HelpCenterViewModel` and `EditBookingViewModel` remain untested.

---

## Phase 16: Detailed Cleaning and Location Options ✅ STRUCTURAL ⚠️

Richer service configuration for both clients and cleaners.

> ✅ **Shipped — corrected 2026-07-27.** This phase was previously listed as entirely unstarted with
> every box unticked; that was wrong. Verified against `develop` @ `4892952`: the `CleaningType` and
> `LocationType` enums, `LocationDetail`/`SavedLocation` models, `CleaningTypeSelector.kt`,
> `LocationTypeSelector.kt`, `SavedLocationsScreen.kt`, the specialization badges + filter, and
> `MIGRATION_9_10` (DB version **10**) all exist and are wired. Tagged `STRUCTURAL ⚠️` rather than
> `COMPLETE` for three reasons: the three Firestore `savedLocations` methods are among the 14
> `"not yet implemented"` stubs (`FirebaseBackendService.kt:517–523`); specialization *ranking* was
> never built; and `SavedLocationRepository` reports failed remote writes as success (see **Track E**).

### Tasks

#### Cleaning Types (Client side)

- [x] Define expanded CleaningType enum: Standard, Deep Clean, End of Tenancy, Post-Construction,
  Carpet and Upholstery, Window Cleaning, Oven and Appliance, Move-In/Move-Out, Office/Commercial
  (`DomainModels.kt:109` — all 9 values)
- [x] Update BookingFormScreen with a visual grid chip selector for cleaning types
  (`CleaningTypeSelector` at `BookingFormScreen.kt:125`)
- [x] Store and sync selected cleaning type with bookings — `Booking.cleaningType`,
  `BookingEntity.cleaningType`, `BookingDto.cleaningType`, persisted by `MIGRATION_9_10`

#### Location Types (Client side)

- [x] Define LocationType model: Apartment, House, Studio, Office, Retail, Warehouse,
  Airbnb/Short-Term Rental, Other (`DomainModels.kt:125` — all 8 values, plus `LocationDetail`)
- [x] Add location type selector to BookingFormScreen (`LocationTypeSelector` at
  `BookingFormScreen.kt:133`) and reach it from CustomerProfileScreen — the profile links out to
  `SavedLocationsScreen`, which hosts the selector, rather than embedding it inline
- [x] Include room count, bathroom count, and approximate square footage inputs
  (`LocationDetail.roomCount` / `bathroomCount` / `sqFootage`, all editable in `LocationTypeSelector`)
- [x] Allow clients to save multiple named locations (e.g. Home, Office) in their profile —
  `SavedLocationsScreen`, `saved_locations` Room table, `ISavedLocationRepository`,
  `Screen.SavedLocations` route wired in `AppShell.kt:826`

#### Areas of Specialization (Cleaner side)

- [x] Define Specialization model mirroring the expanded cleaning types above —
  `Provider.specializations: List<CleaningType>` (`DomainModels.kt:67`)
- [ ] Allow cleaners to select **and rank** their specializations — selection shipped, but on
  **`CleanerProfileScreen`** (edit mode, `onToggleSpecialization`), **not** `ServiceManagementScreen`,
  and there is **no ranking** — the list is an unordered toggle set
- [x] Show specialization badges on ProviderSearchScreen provider cards (up to 3 per card,
  `ProviderSearchScreen.kt:213`)
- [x] Update FilterSheet to filter providers by specialization (`FilterSheet.kt:124`, applied in
  `BookingViewModel.loadProviders`)
- [x] Update CleanerProfileScreen to display specializations prominently
  (`CleanerProfileScreen.kt:165`)
- [x] Sync specializations with backend — `ProviderDto.specializations`, entity column + mappers,
  seeded in the stub. *Firestore inherits the Track C read blocker like every other collection.*

#### General

- [x] Update IBackendService DTOs and BackendServiceStub for new fields (`cleaningType`,
  `locationType`, `specializations`, `SavedLocationDto`)
- [ ] Update FirebaseBackendService for the new fields — `getSavedLocations`, `upsertSavedLocation`
  and `deleteSavedLocation` all still return `Result.Error("Firebase saved locations not yet
  implemented")` (`FirebaseBackendService.kt:517–523`)
- [x] Add Room migration v9 to v10 for new columns on bookings and provider profiles
  (`MIGRATION_9_10`: `providers.specializations`, `bookings.cleaningType`, `bookings.locationType`,
  new `saved_locations` table; DB version bumped to 10)
- [ ] Update search and matching logic to factor in cleaning type and location type compatibility —
  only *half* done: `BookingViewModel` filters providers by specialization client-side, but
  `BackendServiceStub.searchProviders` returns every seeded provider unfiltered and **location-type
  compatibility is not considered at all**
- [x] Seed rich demo data using the expanded types (specializations on all 5 seed providers, 2 seeded
  saved locations)

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
- `data/remote/backend/FirebaseBackendService.kt` - saved-location endpoints declared but **still
  stubbed** (`:517–523`)
- `data/repository/SavedLocationRepository.kt` - saved-location persistence *(new — has the
  error-to-success defect noted in Track E)*
- `ui/booking/BookingFormScreen.kt` - CleaningTypeSelector and LocationTypeSelector integration
- `ui/booking/ProviderSearchScreen.kt` - Specialization badges on provider cards
- `ui/booking/BookingViewModel.kt` - CleaningType and LocationType in form state, specialization filter
- `ui/components/FilterSheet.kt` - Specialization filter option
- `ui/cleaner/CleanerProfileScreen.kt` - Specializations section + edit-mode toggle selection
  *(this, not `ServiceManagementScreen`, is where cleaners pick specializations)*
- `ui/customer/CustomerProfileScreen.kt` - Saved locations quick link
- `ui/navigation/Screen.kt` / `ui/shell/AppShell.kt` - `SavedLocations` route

### Tests

- [ ] Cleaning type selection and persistence tests
- [ ] Location type and room detail tests
- [ ] Specialization filter and search tests
- [ ] `SavedLocationRepository` tests — **none exist**, and the repository currently maps
  `Result.Error → Result.Success` on both read and write (Track E)

**Status**: ✅ STRUCTURAL ⚠️ — see the phase callout above

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

### Current Status: **Phases 1–16 built; 9 still "structural, not working"** *(verified 2026-07-27, `develop` @ `4892952`)*

(Architecture, Authentication, Booking, Provider Mgmt, Reviews, Payments, Navigation & App Flow,
Firebase Integration, Maps & Location, Sync & Offline Features, In-App Chat, Subscriptions,
Multi-Language, Reverse Reviews, Help/Support & Claims, Detailed Cleaning & Location Options)

**Scale at that commit:** 124 production `.kt` files / **25,598 production LOC**; 25 ViewModels;
Room DB version **10** with 9 migrations; **26 unit-test files with 300 `@Test` methods** plus 23
`androidTest` files.

Still tagged `✅ STRUCTURAL ⚠️` — **9 phases**: **5** and **14** (reverse-review `direction` /
`locationTags` dropped on the Firebase write), **6** (no PSP), **8** (Firebase backend), **10**
(dead sync queue), **11** (no retry/outbox — depends on the Track A decision), **12** (untested
`RecurringBookingWorker`), **15** (14 stubbed methods), **16** (stubbed saved-locations). Every one
of these is blocked on Track A or Track C, not on new feature work.

**Promoted since the 2026-06-25 review:** Phase **13** → `✅ COMPLETE` (multi-language really works
now), and the blocking *bugs* in Phases **11** and **12** are fixed (`sendMessage` no longer reports
false success; `getCurrentSession()` no longer hangs). Phase **16**, previously listed as entirely
unstarted, is in fact shipped.

> ⚠️ **Where the risk sits now (2026-07-27).** The codebase compiles, CI gates every PR on
> `assembleDebug allUnitTests`, and the app demos well on the mock backend. The remaining gap is
> almost entirely **"works on the stub" vs "works against real Firebase"**:
> - 🔴 **Every Firestore *read* fails.** All DTOs in `IBackendService.kt` have required (no-default)
>   constructor params, so Kotlin emits no no-arg constructor and Firestore's mapper cannot
>   deserialize. `FirebaseBackendService` calls `toObject()`/`toObjects()` 26 times, and each one
>   throws into a swallowed `Result.Error`. `USE_MOCK_BACKEND=false` today = a **write-only app**.
> - **`release` routes to a Firebase backend with 14 unimplemented methods** + a placeholder
>   `google-services.json` — Phases 8, 15 and 16 silent-fail in production.
> - **Security pre-production:** plaintext auth token + PII in DataStore, **zero** Firestore Security
>   Rules committed, `allowBackup="true"` with empty rule files, payments fabricating `SUCCEEDED`
>   client-side with no PSP, and unmasked PAN/CVV fields with no `FLAG_SECURE` — Phase 6.
> - **No release config at all:** no `signingConfig`, `applicationId` still `com.example.diamonds`,
>   `isMinifyEnabled = false`, empty ProGuard template.
> - **Offline-write/sync queue is still dead code** (`queueOperation()` has no production callers) —
>   Phase 10 remains overstated until the Track A decision is made.
> - **CI never compiles or runs `androidTest`**, which is how a broken `LoginScreenTest` assertion
>   and an incomplete test Hilt graph reached `develop` unnoticed — Track E.
>
> *Superseded claims (do not reuse):* "multi-language is inert / `stringResource` in 1 of 83 files"
> — fixed, it is now 46 of 83; "no `SyncManager` tests" — 21 now exist; "coverage ~15–20%" and
> "`BookingRepositoryTest` is a no-op" — retracted; "`getReviewsForProvider` is a full scan" — it is
> filtered.

### Next Immediate Steps:

**Follow the 🧭 Remediation Roadmap at the top of this file, not the linear Phase 17→21 march.**
In priority order:

1. **Track C, item 1 — fix the Firestore DTO no-arg-constructor blocker.** Everything else on the
   Firebase go-live path is invisible behind it, and it is a small, mechanical change.
2. **Track A — make the offline-write decision.** Wire `SyncManager.queueOperation()` into the write
   repos, or delete the queue and correct the docs. It is the only Track A item left and it is
   blocking honest status on Phases 10 and 11.
3. **Track E — make CI tell the truth.** Add `assembleDebugAndroidTest` (plus lint), then fix the
   broken `LoginScreenTest` assertion, the missing `ISavedLocationRepository` test binding, and the
   `SavedLocationRepository` error-to-success mapping.
4. **Track C, rest — the Firebase implementation, security and release config**, all *before*
   flipping `USE_MOCK_BACKEND=false`.
5. **Track B leftovers** — a lint baseline so hardcoded strings cannot creep back, real icons,
   `collectAsStateWithLifecycle`.
6. **Track D — hardening**: Worker/DataStore/Connectivity tests, Room schema export + migration
   tests, `DispatcherProvider`; folds into Phases 17–18.
7. Finish the two genuinely-open Phase 16 items (specialization ranking, location-type-aware
   matching) alongside the above.

### Architecture Strengths *(confirmed 2026-07-27)*

- Clean, **acyclic** module graph — `:common`→`:core`; `:data`→`:core`,`:common`;
  `:ui`→`:core`,`:data`,`:common`; `:app`→all — with `:core` a pure `java-library` containing
  **zero** Android imports
- Read-side offline-first genuinely implemented (cache-first reads, network-failure fallback)
- Swappable backend (`IBackendService`/`IAuthService`) selected via `BuildConfig`
- Sound coroutine hygiene: **zero** `GlobalScope` and **zero** `runBlocking` in any main source set;
  listeners scoped via `awaitClose`
- **Zero** `Log.*`/`println` calls anywhere; no secrets ever committed; `PendingIntent` uses
  `FLAG_IMMUTABLE`; cleartext traffic blocked by the `targetSdk 34` default
- 9 proper additive Room migrations; 26 unit-test files / 300 `@Test` methods
- Fully localized string layer: 531 strings + 16 plurals, key-complete across 5 locales

### Risk Areas to Monitor
- 🔴 **Firestore reads are non-functional** (DTO no-arg-constructor blocker) — the single highest
  risk; it makes the whole real-backend path untestable until fixed
- **Real-backend parity** — the Stub implements features the Firebase impl stubs out; debug ≠ release
- **Offline-write correctness** — the queue is still bypassed; writes fail rather than retry
- **Silent error swallowing** — `SavedLocationRepository` turns failed writes into successes, and 37
  `as? Result.Success` sites across 10 ViewModels drop errors on the floor
- **Room migrations are unverified** — `exportSchema = true` with no schema location and no
  `schemas/` directory, so none of the 9 migrations has a test
- Payment integration security (no PSP today; unmasked card fields)
- `:ui`→`:data` concrete coupling (`ConnectivityObserver`) hurting testability
- Firestore query scalability (`searchProviders` ignores its geo arguments entirely;
  `getConversationsForUser` full scans) at scale

### Future Enhancements
- [ ] Machine learning for provider matching
- [ ] Predictive scheduling
- [ ] Advanced analytics dashboard
- [ ] Admin panel for moderation
- [ ] A/B testing framework
