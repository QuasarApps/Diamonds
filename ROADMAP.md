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

## Phase 2: Authentication & User Management
User authentication and profile management screens.

### Tasks
- [ ] Create auth screen ViewModel
- [ ] Build Login screen (Compose)
- [ ] Build Signup screen (Compose)
- [ ] Build role selection (Client vs Provider)
- [ ] Implement token refresh logic
- [ ] Add JWT token decoding
- [ ] Store auth token securely in DataStore
- [ ] Create ProfileScreen for users
- [ ] Build profile edit flows
- [ ] Add profile picture upload

### Tests
- [ ] Auth flow unit tests
- [ ] Login/Signup UI tests
- [ ] Token management tests

**Estimated Duration**: 2-3 weeks

---

## Phase 3: Booking Flow (Client Side)
Core booking functionality for clients.

### Tasks
- [ ] Create ProviderSearchScreen with filters
- [ ] Build ServiceListScreen
- [ ] Create BookingDetailsScreen
- [ ] Build BookingConfirmationScreen
- [ ] Implement date/time picker
- [ ] Add address input with Maps integration
- [ ] Create BookingHistoryScreen
- [ ] Build BookingDetailsScreen (track status)
- [ ] Add booking status updates (real-time)
- [ ] Implement pull-to-refresh

### Tests
- [ ] Search repository tests
- [ ] Booking creation tests
- [ ] Offline booking list tests

**Estimated Duration**: 3-4 weeks

---

## Phase 4: Provider Management
Provider-side features.

### Tasks
- [x] Create CleanerViewModel with booking requests / schedule / dashboard state
- [x] Build CleanerBookingRequestsScreen (accept / decline PENDING bookings)
- [x] Build CleanerScheduleScreen (today's + upcoming ACCEPTED/IN_PROGRESS jobs with Start / Complete actions)
- [x] Live CleanerDashboardTab (pending count, today's job count, weekly earnings)
- [x] Add "Requests" tab to cleaner bottom nav
- [x] Seed stub with realistic cleaner-side demo bookings (requests, jobs, past completions)
- [x] Add demo cleaner account (cleaner@demo.com → p1) with matching hint on Login screen
- [x] Add CleanerType enum (INDEPENDENT / EMPLOYED) to domain model and Provider
- [x] Thread cleanerType / employerId / employerName through entity, DTO, and mappers
- [x] Room migration v1→v2 for new provider columns
- [x] Seed stub: independent (p1, p4), company (p5 – Sparkle Pro), employed cleaners (p2, p3)
- [x] SignupScreen: INDEPENDENT / EMPLOYED sub-selector + info card when Cleaner role selected
- [x] ProviderSearchScreen: Independent / Employed / Company badge on each card
- [x] LoginScreen: tappable demo account table (4 accounts, tap auto-fills email + password)
- [x] MockAuthService: employed@demo.com → p2, company@demo.com → p5 demo shortcuts
- [x] Add COMPANY to CleanerType enum; persist cleanerType in UserSession + DataStore
- [x] IAuthRepository.login / signup: accept & persist cleanerType hint
- [x] LoginScreen: tapping a demo row sets role + cleanerType so routing is immediate
- [x] Three distinct dashboards: CustomerHomeTab (popular services, CTA), CleanerDashboardTab (employed-cleaner badge, live stats), CompanyDashboardScreen (KPI cards, team size, revenue)
- [x] CompanyViewModel: fetches all employed-cleaner bookings for aggregate stats
- [x] CompanyTeamScreen: list of team members with per-cleaner active jobs / pending / week earnings
- [x] CompanyBookingsScreen: tabbed (Pending / Active / Completed) across all company cleaners, accept/decline actions
- [x] BottomTab: companyTabs (Overview / Bookings / Team / Earnings / Profile); tabsForSession / startTabForSession route by cleanerType
- [x] AppShell: full session-aware routing — customer / cleaner / company NavHosts with per-role top-bar titles
- [x] Create CleanerProfileScreen — avatar, editable name/bio/phone, employment badge, services summary, "Manage" CTA
- [x] Build ServiceManagementScreen — list services with active toggle (Switch), edit sheet (ModalBottomSheet), add new service with category dropdown
- [x] Create CleanerProfileViewModel — profile load/save, service CRUD, add/edit sheet state
- [x] CleanerEarningsScreen — Canvas bar chart (last 7 days), monthly KPI row (revenue / jobs / avg), per-booking history list
- [x] CleanerEarningsViewModel — aggregates completed bookings into daily/weekly/monthly buckets
- [x] Seed: expanded p1 completions across last 7 days; p2/p3 employed-cleaner bookings added
- [x] Service management also wired for Company profile tab (CompanyServiceManage route)
- [x] Implement provider ratings display — ProviderRatingsScreen (avg + star breakdown bar + review cards), tappable star row on ProviderCard, wired from search and booking detail
- [x] Create company earnings dashboard — CompanyEarningsScreen + CompanyEarningsViewModel (week hero, monthly KPIs, per-cleaner breakdown, recent jobs list)

### Tests
- [ ] Provider profile tests
- [ ] Service management tests
- [ ] Booking request handling tests

**Estimated Duration**: 3-4 weeks ✅ COMPLETE

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

## Phase 7: Navigation & App Flow
Comprehensive navigation and deep linking.

### Tasks
- [ ] Set up Jetpack Navigation with typed routes
- [ ] Create bottom navigation for main screens
- [ ] Implement deep linking for bookings
- [ ] Add back stack management
- [ ] Create splash screen
- [ ] Build app-level navigation logic
- [ ] Add error handling screens
- [ ] Implement bottom sheets for actions
- [ ] Add dialogs for confirmations

### Tests
- [ ] Navigation flow tests
- [ ] Deep link tests
- [ ] Screen transition tests

**Estimated Duration**: 2 weeks

---

## Phase 8: Firebase Integration
Backend and real-time features via Firebase.

### Tasks
- [ ] Create FirebaseBackendService implementation
- [ ] Implement Firebase Auth integration
- [ ] Set up Firestore collections
- [ ] Add Firebase Cloud Messaging (FCM)
- [ ] Implement push notifications
- [ ] Add real-time booking updates
- [ ] Create notification handler
- [ ] Build notification management screen
- [ ] Add notification preferences

### Tests
- [ ] Firebase Auth tests
- [ ] Firestore integration tests
- [ ] FCM notification tests

**Estimated Duration**: 3 weeks

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

### Current Status: **Phase 1 Complete** ✅

### Next Immediate Steps:
1. Build out Phase 2 (Authentication)
2. Implement Firebase backend service
3. Create login/signup screens
4. Test auth flow with real backend

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
