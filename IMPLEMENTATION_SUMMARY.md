# Implementation Status & Next Steps

**Status**: Phases 1–16 built of the 21-phase roadmap (**16/21 ≈ 76% by phase count**) — but **9 of
those 16 are structural only**, i.e. they compile and demo on the mock backend and do **not** work
against real Firebase.
**Last verified**: 2026-07-27, against `develop` @ `4892952`.

> Read this alongside [`ROADMAP.md`](ROADMAP.md) (the 🧭 Remediation Roadmap, Tracks A–E, is the
> current work order) and [`TECH_LEAD_REVIEW.md`](TECH_LEAD_REVIEW.md) (file:line-cited findings).
> Where this file and the roadmap disagree, the roadmap wins — see the single precedence hierarchy
> in `QUICK_REFERENCE.md` ("Documentation Files").

---

## What's Done

### Phase 1: Core Infrastructure ✅
- 5 Gradle modules (`:core`/`:common`/`:data`/`:ui`/`:app`), strictly acyclic
- Hilt dependency injection (`app/di/Modules.kt`, `app/di/AuthServiceModule.kt`)
- Room database — now **version 10**: 15 entities, 15 DAOs, 9 migrations
- 13 repository implementations against 14 `:core` interfaces (`SyncManager` implements the 14th)
- `SyncManager` with exponential backoff and conflict resolution
- `BaseViewModel` with `StateFlow` state, error and connectivity streams
- Material 3 Compose theme

### Phase 2: Authentication & User Mgmt ✅
- Login/Signup screens with a demo-account picker
- `AuthViewModel` + `AuthRepository` session management
- Session token stored in DataStore — ⚠️ **plaintext, not encrypted** (see P1 below)
- Profile screens (Customer/Cleaner/Company)
- Role & `CleanerType` selection

### Phase 3: Booking Flow (Client) ✅
- Provider search with filter sheet and badges
- Service listings, booking form, confirmation screen
- Booking history with pull-to-refresh
- Date/time picker, booking status state machine

### Phase 4: Provider Management ✅
- Cleaner dashboard (requests, schedule, earnings), company dashboard with KPIs
- Team management, service management (add/edit/toggle)
- 7-day earnings chart, role-based navigation

### Phase 5: Reviews & Ratings ✅ structural
- Review submission & viewing, interactive star picker, provider ratings screen
- Seeded review data in `BackendServiceStub`
- ⚠️ `FirebaseBackendService.createReview` silently drops `direction` and `locationTags`

### Phase 6: Payments ✅ structural
- Payment form with card validation/formatting, success screen, payment history
- ⚠️ **No PSP.** `FirebaseBackendService.kt:267` fabricates `status = "SUCCEEDED"` client-side
- ⚠️ `PaymentScreen` collects PAN/CVV in unmasked `OutlinedTextField`s with no `FLAG_SECURE`

### Phase 7: Navigation & App Flow ✅
- 49 routes in `ui/navigation/Screen.kt`; 25 `navArgument` declarations in `ui/shell/AppShell.kt`
- Deep linking — `diamonds://` scheme in the manifest plus two `navDeepLink` destinations
- Slide transitions for push/pop, fade for tab switches; `BackHandler` on terminal screens
- Reusable `ConfirmationDialog`; error screens (GenericError, NoInternet, NotFound, OfflineBanner)
- `FilterSheet` bottom sheet for provider search; `PullToRefresh`; back-stack cleanup on logout

### Phase 8: Firebase Integration ✅ structural
- `FirebaseAuthService` (email/password), `FirebaseBackendService`, `FirestoreBookingListener`
- FCM push notifications (`DiamondsFcmService`), 3 notification channels
- Notification model/entity/DAO/repository, `NotificationScreen` with swipe-to-dismiss,
  `NotificationPreferencesScreen`, unread badge in the top app bar
- Room migration v2→v3; `USE_MOCK_BACKEND` / `USE_MOCK_AUTH` BuildConfig switch
- ⚠️ **Does not work against real Firebase** — see the Track C blockers below

### Phase 9: Maps & Location ✅
- Maps Compose + play-services-maps/location
- `BookingMapScreen` (drag-pin address selection), `BookingLocationMapCard`,
  `ProviderTrackingScreen` (ETA + distance), `RequireLocationPermission` with rationale
- `MapViewModel`, `ILocationRepository`/`LocationRepository` (FusedLocation + Haversine)
- `ProviderLocationEntity` + DAO + migration v3→v4; seeded locations and service areas
- Maps API key via a Gradle property + `manifestPlaceholders`

### Phase 10: Sync & Offline Features ✅ structural
- `SyncWorker` (`@HiltWorker`, periodic + immediate, network constraints)
- `ConnectivitySyncTrigger` for debounced OFFLINE→ONLINE auto-sync
- `SyncManager` dispatch (EntityType × OperationType → `IBackendService`), 5-retry backoff
- Conflict resolution UI (`SyncStatusScreen`, Pending/Failed/Conflicts tabs, Keep Local / Use Server)
- Migration v4→v5 for `serverPayload`; `@Serializable` DTOs for sync payloads
- ⚠️ **`SyncManager.queueOperation()` has zero production call sites** — no write repo enqueues, so
  the queue stays empty and offline writes simply fail. Decision pending (Track A)

### Phase 11: In-App Chat ✅ structural
- `Message`/`Conversation` domain models, `IMessageRepository`, entities + DAOs, migration v5→v6
- `MessageRepository` (local-first cache + online sync), `ChatViewModel`
- `ConversationListScreen`, `ChatScreen` (bubbles, timestamps, read receipts), unread badge
- Chat entry point from `BookingDetailScreen`; seeded demo conversations
- ✅ `sendMessage` no longer reports false success when offline (fixed in Track A)
- ⚠️ No retry/outbox for failed sends — depends on the Track A decision

### Phase 12: Subscriptions & Recurring Bookings ✅ structural
- `RecurringBookingEntity` + DAO + migration v6→v7, `ISubscriptionRepository`/`SubscriptionRepository`
- `RecurringBookingSetupScreen`, `SubscriptionManagementScreen`, `RecurringBookingViewModel`
- `RecurringBookingWorker` scheduled daily from `DiamondsApplication`
- ✅ `RecurringBookingViewModel.getCurrentSession()` infinite-suspend fixed (`.first()`, line 193)
- ⚠️ `RecurringBookingWorker` has no tests; `SubscriptionRepository` still returns a generic
  `Exception("No internet connection")` instead of `OfflineException`

### Phase 13: Multi-Language Support ✅
- 531 `<string>` + 16 `<plurals>` in `ui/src/main/res/values/strings.xml`, key-complete across
  `values-fr`, `values-es`, `values-pt`, `values-ar` (plus 169 strings × 5 locales in `:app`)
- `LanguageSelectorScreen` + `LanguageViewModel`; runtime switching **is** wired —
  `AppCompatDelegate.setApplicationLocales()` is called (`LanguageSelectorScreen.kt:89`)
- `locale_config.xml`, RTL support for Arabic, `DateTimeFormatUtil` for locale-aware formatting
- 46 of the 83 `:ui` files reference `stringResource` (608 occurrences); ~67 residual `Text("…")`
  literals remain, almost all non-translatable glyphs (emoji, `"$"`, `"›"`)
- All `Icon` `contentDescription`s resolve through `cd_*` keys

### Phase 14: Reverse Reviews ✅ structural
- `ReviewDirection` + `locationTags` on reviews (migration v7→v8)
- `LeaveClientReviewScreen`, `ClientRatingsScreen`, `ClientReviewViewModel`
- ⚠️ `FirebaseBackendService.createReview` drops both new fields, so the feature is stub-only

### Phase 15: Help, Support & Claims ✅ structural
- `SupportTicketEntity` + `ClaimEntity` + DAOs + migration v8→v9, `SupportRepository`
- `HelpCenterScreen`, `ContextualHelpScreen`, `FileClaimScreen`, `CancelBookingScreen`,
  `EditBookingScreen` and their ViewModels
- ⚠️ All support/claims methods on `FirebaseBackendService` return "not yet implemented"

### Phase 16: Detailed Cleaning & Location Options ✅ structural
- `CleaningType` and `LocationType` enums (`DomainModels.kt:109`, `:125`), provider
  `specializations`
- Migration v9→v10: `providers.specializations`, `bookings.cleaningType`/`locationType`,
  `saved_locations` table
- `CleaningTypeSelector`, `LocationTypeSelector`, `SavedLocationsScreen` +
  `SavedLocationsViewModel`, `SavedLocationRepository`
- Specialization badges on provider cards and a specialization filter in `FilterSheet`
- ⚠️ Saved-locations backend methods are stubbed, and `SavedLocationRepository` reports failed
  remote writes as success (see P1 below)

---

## Metrics

| Item                        | Count      |
|-----------------------------|------------|
| Production `.kt` files      | 124        |
| Production LOC              | 25,598     |
| Screen files (`*Screen.kt`) | 45         |
| ViewModels                  | 25         |
| Navigation routes           | 49         |
| Repository implementations  | 13 (+ `SyncManager`) |
| Repository interfaces       | 14         |
| Room DB version / entities  | 10 / 15    |
| Room migrations             | 9          |
| Unit-test files / `@Test`   | 26 / 300   |
| Instrumentation-test files  | 23         |
| Locales                     | 5          |
| Demo accounts               | 4          |
| Modules                     | 5          |

---

## What's NOT Done Yet

**Phase 17**: Error Handling & Analytics
**Phase 18**: Testing & Optimization
**Phase 19**: Release Preparation & CI/CD
**Phase 20**: Beta Testing
**Phase 21**: Launch

⚠️ Do **not** start the linear Phase 17→21 march yet. Phase 19 cannot truthfully begin until the
Firebase go-live gate (ROADMAP Track C) is cleared — the release variant currently routes to a
backend whose reads all fail. Work the Remediation Roadmap first.

---

## Audit Findings (2026-07-27, `develop` @ `4892952`)

### 🔴 P1 — Must fix before flipping `USE_MOCK_BACKEND=false`

| Issue                                                                                                                                                | File                                          |
|------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| **Every Firestore read fails.** All DTOs in `IBackendService.kt` are `data class`es with required (no-default) params (`ClientDto:113`, `BookingDto:162`), so Kotlin emits no no-arg constructor; Firestore's mapper needs one. `toObject()`/`toObjects()` is called 26 times and every call throws into a swallowed `Result.Error`. Release today = a **write-only app**. | `IBackendService.kt`, `FirebaseBackendService.kt` |
| 14 `FirebaseBackendService` methods return `Result.Error("…not yet implemented")`                                                                     | `FirebaseBackendService.kt:484–524`           |
| `FirebaseAuthService.signup` creates no Firestore profile document and discards the `role` argument                                                   | `FirebaseAuthService.kt`                      |
| `createBooking` hardcodes `totalPrice = 0.0` and `estimatedDuration = 60`                                                                             | `FirebaseBackendService.kt`                   |
| `createReview` drops `direction` and `locationTags`                                                                                                   | `FirebaseBackendService.kt`                   |
| `searchProviders` ignores `latitude`/`longitude`/`radius` entirely                                                                                    | `FirebaseBackendService.kt`                   |
| Payments fabricate `status = "SUCCEEDED"` client-side; no PSP integrated                                                                              | `FirebaseBackendService.kt:267`               |
| Auth token **and** PII stored in **plaintext** DataStore (not encrypted)                                                                              | `PreferencesDataStore.kt:16`                  |
| **Zero** Firestore Security Rules in the repo — no `firestore.rules`, `firebase.json` or `.firebaserc`                                                | (absent)                                      |
| `app/google-services.json` is a placeholder (`project_number "000000000000"`, `api_key "placeholder-key-for-testing"`)                                | `app/google-services.json`                    |
| No `signingConfig` anywhere; `applicationId = "com.example.diamonds"` (Play rejects `com.example.*`); `isMinifyEnabled = false`; ProGuard file is the untouched empty template | `app/build.gradle.kts`, `app/proguard-rules.pro` |
| FCM token is saved to DataStore but never registered server-side (`observeFcmToken` has 0 callers)                                                    | `PreferencesDataStore.kt`                     |
| `android:allowBackup="true"` with empty backup rule files — session data is ADB-extractable                                                           | `AndroidManifest.xml`                         |
| `PaymentScreen` collects card PAN/CVV in unmasked fields with no PSP SDK and no `FLAG_SECURE`                                                         | `PaymentScreen.kt`                            |
| `SavedLocationRepository` maps `Result.Error → Result.Success(emptyList())` (`:32`) and `Result.Error → Result.Success(location)` (`:45`) — a failed remote write is reported to the UI as success | `SavedLocationRepository.kt`                  |

### 🟡 P2 — Important

| Issue                                                                                                                     | File                                     |
|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| **Offline-write decision still open.** `SyncManager.queueOperation()` has zero production callers: either wire it into the write repos or delete the queue and stop advertising offline writes | `SyncManager.kt`, all write repositories |
| `LoginScreenTest.kt:153` asserts `"Demo accounts  (tap to fill)"` (two spaces) but `R.string.demo_accounts_hint` is `"Demo accounts (tap to fill)"` (one space) — the test fails on a device | `app/src/androidTest/.../LoginScreenTest.kt` |
| The `androidTest` Hilt graph is incomplete: `FakeRepositoryModule` `@TestInstallIn`-replaces `RepositoryModule` wholesale with 13 `@Provides` but omits `ISavedLocationRepository`, which production `Modules.kt` provides | `app/src/androidTest/.../FakeRepositoryModule.kt` |
| CI never compiles or runs `androidTest` (no `assembleDebugAndroidTest`), runs no lint and no coverage gate — which is why the two rows above went unnoticed. `allUnitTests` also aggregates `:app` and `:common`, neither of which has a `src/test` directory, and excludes `:core` | `.github/workflows/ci.yml`, `build.gradle.kts` |
| 37 `as? Result.Success` sites across 10 ViewModel files collapse `Result.Error` into `null` with no user-visible error, contradicting the documented three-arm `Result` convention | `ui/**/*ViewModel.kt`                    |
| Room `exportSchema = true` with no `room.schemaLocation` and no `schemas/` directory — all 9 migrations are unverified and the declared `room-testing` dependency is dead weight | `data/build.gradle.kts`, `AppDatabase.kt` |
| No lint rule or baseline anywhere to stop new hardcoded UI strings creeping back in                                        | (absent)                                 |
| `ConnectivityObserver.isOnline()` can false-positive on captive portals (no `NET_CAPABILITY_VALIDATED` check)              | `ConnectivityObserver.kt`                |
| `DiamondsApplication` constructs its own `ConnectivitySyncTrigger`; the Hilt-provided one (`Modules.kt:114`) is never used  | `DiamondsApplication.kt`                 |
| `getCurrentClient()` returns `Result.Error("Not implemented")`; `updateProvider()`, two `PaymentRepository` methods and one `ServiceRepository` method return `"Not implemented in backend service"` | `ClientRepository.kt:87`, `ProviderRepository.kt:128`, `PaymentRepository.kt:119,132`, `ServiceRepository.kt:151` |
| The deep-link intent filter sets `autoVerify="true"` on a custom `diamonds` scheme, where it does nothing — App Links verification only applies to `http`/`https` with a published Digital Asset Links file | `AndroidManifest.xml`                    |
| No image-loading library at all — zero references to Coil, Glide, Picasso or `AsyncImage` repo-wide; remote avatars/photos cannot be rendered | `gradle/libs.versions.toml`              |

### 🔵 P3 — Code quality

| Issue                                                                                                          | File                       |
|----------------------------------------------------------------------------------------------------------------|----------------------------|
| No tests for `SyncWorker`, `RecurringBookingWorker`, `PreferencesDataStore` or `ConnectivityObserver`           | `data/src/test/`           |
| Turbine is declared in both `data/build.gradle.kts` and `ui/build.gradle.kts` but has **0** uses                | build files                |
| No `DispatcherProvider`; tests rely on `MainDispatcherRule` + `runTest`                                          | `ui/src/test/`             |
| 0 uses of `collectAsStateWithLifecycle` vs 87 `collectAsState()` call sites across 44 files                      | `ui/src/main/`             |
| `ACCESS_BACKGROUND_LOCATION` and `POST_NOTIFICATIONS` are declared but never requested or used at runtime        | `AndroidManifest.xml`      |
| Toolchain is behind: AGP 8.5.0, Kotlin 1.9.0, Compose 1.6.0 / Material3 1.2.0, Hilt 2.50, Room 2.6.1 | `gradle/libs.versions.toml`|
| `:ui` couples to the concrete `:data` `ConnectivityObserver` instead of a `:core` interface                      | `BaseViewModel.kt`         |

### ✅ Retracted / fixed since the April audit — do not re-file

- "Runtime locale switching not wired" — **fixed**, `AppCompatDelegate.setApplicationLocales()` is
  called from `LanguageSelectorScreen`
- "Token storage (DataStore encrypted)" — **was never true**; it is plaintext, and is now filed
  correctly as a P1
- "Test coverage ~15–20%" and "`BookingRepositoryTest` is a no-op" — **retracted**; there are now
  26 unit-test files with 300 `@Test` methods
- "`getReviewsForProvider` does a full collection scan" — **retracted**, the query is filtered
- "`MessageRepository.sendMessage` reports success when offline" — **fixed**
  (`MessageRepository.kt:131–150`)
- "`RecurringBookingViewModel.getCurrentSession()` suspends forever" — **fixed** (uses `.first()`)
- "No CI" — **fixed**, `.github/workflows/ci.yml` gates every PR to `develop`

---

## Architecture Highlights

**What genuinely holds up**:
- The module graph is acyclic and matches the docs — `:common` → `:core`; `:data` → `:core`,
  `:common`; `:ui` → `:core`, `:data`, `:common`; `:app` → all four. `:core` is a pure
  `java-library` with **zero** Android imports.
- **Read-side offline-first is real**: reads fall back to the Room cache when offline or when a
  network call fails, and refresh from the backend when online.
- Zero `GlobalScope` and zero `runBlocking` in any main source set.
- Zero `Log.*`/`println` calls; no secrets have ever been committed; `PendingIntent` uses
  `FLAG_IMMUTABLE`; cleartext traffic is blocked by the `targetSdk 34` default.

**What is overstated elsewhere**:
- **Writes are online-only.** They fail with an offline error; they are *not* queued. The
  `SyncManager` queue, backoff and conflict UI all exist but nothing feeds them.
- Most repos return `OfflineException`, but `SubscriptionRepository` and
  `MessageRepository.getOrCreateConversation` still return a generic
  `Exception("No internet connection")`.

**Type safety**: `Result<T>` (`Success`/`Error`/`Loading`), sealed UI state, null safety — though
37 `as? Result.Success` sites currently short-circuit the convention.

**Testability**: repository interfaces live in `:core` and are easy to fake; `:data` unit tests mock
the DAOs and `IBackendService` with MockK; `MainDispatcherRule` covers ViewModel tests; in-memory
Room is used only by the instrumentation Hilt graph (`app/src/androidTest/di/FakeDataModule.kt`).
Gaps: no `DispatcherProvider`, no worker tests, no schema-verified migrations.

---

## How to Continue

### For Developers Joining Now

1. **Read** [`README.md`](README.md) (5 min) — overview and Known Issues
2. **Study** [`ARCHITECTURE.md`](ARCHITECTURE.md) (20 min) — module/data-flow/ViewModel patterns
3. **Skim** [`TECH_LEAD_REVIEW.md`](TECH_LEAD_REVIEW.md) — what is really broken and why
4. **Pick work from** [`ROADMAP.md`](ROADMAP.md) — the Tracks A–E section, not the phase list
5. **Follow** [`QUICK_REFERENCE.md`](QUICK_REFERENCE.md) for copy-paste patterns, and
   [`CLAUDE.md`](CLAUDE.md) for repo conventions

### To Add a Feature

**New Screen**:
1. Create the `UiState` data class
2. Create the ViewModel extending `BaseViewModel<UiState>`
3. Create the `@Composable` screen in the same feature package
4. Add a route to `ui/navigation/Screen.kt` and wire it into `ui/shell/AppShell.kt`
5. Externalize every user-facing literal to `strings.xml` in all five locales

**New Repository**:
1. Add the interface to `core/domain/repository/Repositories.kt`
2. Implement it in `data/repository/`
3. Bind it in `app/di/Modules.kt` — **and** add a fake to `app/src/androidTest/di/FakeRepositoryModule.kt`

**New Entity**:
1. Add the `@Entity` to `data/local/entity/Entities.kt`
2. Add the `@Dao` to `data/local/dao/Daos.kt`
3. Register both on `AppDatabase`, bump the version and add a `Migration`
4. Add Entity ↔ Domain ↔ DTO mappers in `data/mapper/Mappers.kt`

### Testing

Run `./gradlew allUnitTests` (JDK 17 required). Instrumentation tests need an emulator:
`./gradlew :app:connectedDebugAndroidTest` — CI does not run them, so run them locally before
touching `:ui` strings or Hilt modules.

**Unit test pattern** — MockK + `runTest`, mocking the DAO, `IBackendService` and
`ConnectivityObserver` (see `data/src/test/.../BookingRepositoryFullTest.kt`):
```kotlin
@Test
fun `read falls back to cache when offline`() = runTest {
    every { connectivityObserver.isOnline() } returns false
    coEvery { bookingDao.getForClient("c1") } returns listOf(cachedEntity)
    assertTrue(repository.getClientBookings("c1") is Result.Success)
}

@Test
fun `write fails with OfflineException when offline`() = runTest {
    every { connectivityObserver.isOnline() } returns false
    val result = repository.createBooking(booking)
    assertTrue((result as Result.Error).exception is OfflineException)
}
```

---

## Key Code Locations

| Item                 | Location                                                        |
|----------------------|-----------------------------------------------------------------|
| Domain Models        | `core/src/main/java/com/example/diamonds/domain/model/`          |
| Repo Interfaces      | `core/src/main/java/com/example/diamonds/domain/repository/`     |
| Repo Implementations | `data/src/main/java/com/example/diamonds/data/repository/`       |
| Room Entities / DAOs | `data/src/main/java/com/example/diamonds/data/local/`            |
| Screens + ViewModels | `ui/src/main/java/com/example/diamonds/ui/<feature>/` (side by side) |
| Navigation           | `ui/src/main/java/com/example/diamonds/ui/navigation/`, `ui/shell/AppShell.kt` |
| Strings              | `ui/src/main/res/values*/strings.xml`, `app/src/main/res/values*/strings.xml` |
| Hilt Config          | `app/src/main/java/com/example/diamonds/di/`                     |
| Demo Data            | `data/src/main/java/com/example/diamonds/data/remote/backend/BackendServiceStub.kt` |

---

## Demo Accounts to Test

Debug builds run on `MockAuthService` (`USE_MOCK_AUTH = true`). Four shortcut accounts map to
UIDs that match the seeded stub data; **any password works**:

```
customer@demo.com → CUSTOMER, uid "demo_customer" (Demo Customer)
cleaner@demo.com  → CLEANER,  uid "p1" (Maria Garcia, independent)
employed@demo.com → CLEANER,  uid "p2" (James Okafor, employed by Sparkle Pro)
company@demo.com  → CLEANER,  uid "p5" (Sparkle Pro Cleaning Co.)
```

`fail@test.com` always returns an error, for exercising error states. Any other well-formed email
also logs in, with a synthetic UID that has no seeded data behind it.

---

## Common Patterns to Follow

**Repository — offline-capable read** (network-first with cache fallback, per `BookingRepository`):
```kotlin
override suspend fun getClientBookings(clientId: String): Result<List<Booking>> {
    if (connectivityObserver.isOnline()) {
        return try {
            when (val remote = backendService.getClientBookings(clientId)) {
                is Result.Success -> { /* upsert into Room, return the domain models */ }
                is Result.Error   -> remote
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            bookingDao.getForClient(clientId)
                .takeIf { it.isNotEmpty() }
                ?.let { Result.Success(it.map { row -> row.toDomain() }) }
                ?: Result.Error(e)
        }
    }
    // Offline — serve from cache, or report it honestly
    val cached = bookingDao.getForClient(clientId)
    return if (cached.isNotEmpty()) Result.Success(cached.map { it.toDomain() })
    else Result.Error(OfflineException("Bookings not available offline"))
}
```
Exact ordering varies by repo — `getOrCreateConversation`, for instance, is cache-first. Check the
repository you are copying before assuming.

**Repository — online-only write**:
```kotlin
override suspend fun createBooking(booking: Booking): Result<Booking> {
    if (!connectivityObserver.isOnline()) {
        return Result.Error(OfflineException("Booking creation requires internet connection"))
    }
    return when (val remote = backendService.createBooking(booking.toRequest())) {
        is Result.Success -> { bookingDao.upsert(...); Result.Success(...) }
        is Result.Error   -> remote          // propagate, never fake success
        is Result.Loading -> Result.Loading
    }
}
```

**ViewModel — state**:
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    private val repo: IMyRepository
) : BaseViewModel<MyUiState>(connectivityObserver, MyUiState()) {

    fun load() = viewModelScope.launch {
        updateState { it.copy(isLoading = true) }
        when (val result = repo.get()) {
            is Result.Success -> updateState { it.copy(isLoading = false, items = result.data) }
            is Result.Error   -> { setError(result.exception.message); updateState { it.copy(isLoading = false) } }
            is Result.Loading -> Unit
        }
    }
}
```
Handle all three `Result` arms — do **not** write `result as? Result.Success` and drop the error.

**Reading the session**:
```kotlin
// getCurrentUserSession() is a never-completing DataStore flow — .first(), never collect { }
val session = authRepository.getCurrentUserSession().first() ?: return  // null = logged out
```

---

## Next Sync Point

Before opening the next PR, confirm:
- ✅ `./gradlew assembleDebug allUnitTests` is green (CI runs exactly this)
- ✅ `./gradlew :app:connectedDebugAndroidTest` still compiles — CI does not check it
- ✅ New user-facing strings exist in all five locales
- ✅ New `Result` handling covers `Success`/`Error`/`Loading`
- ✅ Any doc claim you added is backed by the source, not by an older doc

---

**Last Updated**: 2026-07-27 (`develop` @ `4892952`)
**Status**: Phases 1–16 built, 9 of them structural only; clear ROADMAP Track C before Firebase go-live
**Questions?** Start with `ROADMAP.md` and `TECH_LEAD_REVIEW.md`.
