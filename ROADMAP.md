# Implementation Roadmap

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

## Phase 5: Reviews & Ratings ✅ COMPLETE
Post-job review system.

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

## Phase 6: Payments ✅ COMPLETE
Payment processing with mock/stub implementation, ready for Stripe swap-in.

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

## Phase 8: Firebase Integration ✅ COMPLETE
Backend and real-time features via Firebase.

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

**Status**: ✅ COMPLETE

---

## Phase 9: Maps & Location
Maps integration and location services.

### Tasks
- [ ] Integrate Google Maps SDK
- [ ] Implement location permissions
- [ ] Create MapScreen for booking
- [ ] Add provider location display
- [ ] Implement service area visualization
- [ ] Create real-time tracking during job
- [ ] Add ETA calculation
- [ ] Build distance display
- [ ] Implement geofencing (future)

### Tests
- [ ] Location permission tests
- [ ] Map display tests
- [ ] Distance calculation tests

**Estimated Duration**: 2-3 weeks

---

## Phase 10: Sync & Offline Features
Complete offline-first implementation and sync.

### Tasks
- [ ] Implement SyncWorker background job
- [ ] Add connectivity change triggers
- [ ] Build sync status UI
- [ ] Create pending operations list
- [ ] Add manual retry for failed syncs
- [ ] Implement operation cancellation UI
- [ ] Add sync conflict resolution
- [ ] Create sync error reporting
- [ ] Test offline → online transitions extensively

### Tests
- [ ] Sync queue tests
- [ ] Connectivity change tests
- [ ] Offline data preservation tests

**Estimated Duration**: 2 weeks

---

## Phase 11: Error Handling & Analytics
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

## Phase 12: Testing & Optimization
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

## Phase 13: Release Preparation
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

## Phase 14: Beta Testing & Refinement
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

## Phase 15: Launch
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

### Current Status: **Phase 1-8 Complete
** ✅ (Authentication, Booking, Provider Mgmt, Reviews, Payments, Navigation & App Flow, Firebase Integration)

### Next Immediate Steps:

1. Begin Phase 9 (Maps & Location services)
2. Continue with Phase 10 (Advanced Sync & Offline)
3. Start Phase 11 (Error Handling & Analytics)

### Architecture Strengths
- ✅ Modular structure for parallel development
- ✅ Offline-first foundation
- ✅ Flexible backend abstraction
- ✅ Comprehensive testing patterns
- ✅ Clean separation of concerns

### Risk Areas to Monitor
- Network handling complexity
- Sync conflict resolution edge cases
- Payment integration security
- Maps performance on large bookings
- Real-time updates at scale

### Future Enhancements
- [ ] Machine learning for provider matching
- [ ] Predictive scheduling
- [ ] In-app chat system
- [ ] Subscription/recurring bookings
- [ ] Multi-language support
- [ ] Advanced analytics dashboard
- [ ] Admin panel for moderation
- [ ] A/B testing framework
