# Diamonds — Senior Tech-Lead / Architecture Review

**Reviewer:** Tech Lead (independent architectural review)
**Date:** 2026-06-25
**Scope:** Full codebase — 5 Gradle modules, ~124 production Kotlin files, 49 test files
**Method:** Static review of the repository source at the PR branch's HEAD at review time (the repo's then-latest commit). No Android SDK in the review environment, so no Gradle build was executed; compile-level conclusions are reasoned statically and noted where a real build is needed to confirm. Each finding below was independently re-verified against the cited code before inclusion.

---

## 1. Verdict

Diamonds is a **genuinely well-structured demo/portfolio app with a clean module architecture and broad feature surface, but it is not what its documentation claims it to be.** The skeleton is excellent — strict module layering, dependency inversion, a swappable backend, MVVM+StateFlow, 9 hand-written Room migrations. The problem is the gap between the *advertised* "Phase 1–15 complete, production-track" status and what the code actually does when you follow the wires:

- **The offline-first sync engine is dead code.** `SyncManager.queueOperation()` has **zero production call sites**. Every write path hard-fails when offline instead of enqueuing, so the queue, exponential backoff and conflict-resolution machinery process a table that is never populated. The headline feature of the README does not run.
- **The multi-language feature is non-functional.** 169 keys are professionally translated into ES/FR/AR/PT (845 strings, real Arabic), but the UI calls `stringResource` in **exactly one file**; ~350 `Text("…")` string literals are hardcoded English. Switching language changes almost nothing on screen.
- **A `release` build is broken by construction.** The `release` buildType flips `USE_MOCK_BACKEND=false`, routing to a `FirebaseBackendService` whose **14 methods return `Result.Error("not yet implemented")`**, against a **placeholder `google-services.json`**, with `isMinifyEnabled=false`, no `signingConfig`, and `applicationId = com.example.diamonds`.
- **Security posture is pre-production.** The auth token is stored **in plaintext** (while the README advertises "encrypted session storage" — a direct self-contradiction) *(corrected 2026-07-27: the README was fixed in `6f37ca4` (PR #4) and now states plaintext at `README.md:35,108`; the plaintext storage itself is unchanged)*, `allowBackup=true` exposes it, there are **no Firestore Security Rules** in the repo, and payments are marked `SUCCEEDED` with **no payment processor**.

None of these block compilation — to its credit the project **does appear to compile now** (the alarming errors in the `*_build.txt` logs that were committed at review time — since deleted from the repo — were stale Windows runs; every specific error they cited was already fixed). The issue is that "structurally complete" has been mistaken for "working." This review separates the two.

**Bottom line:** strong architecture, honest *debug* experience, but materially overstated completeness. With the mock backend it is a polished prototype. The path to production is longer than the roadmap implies — chiefly: implement the Firebase backend, wire the sync queue (or delete it and correct the docs), externalize strings, encrypt the session, and write Firestore rules.

---

## 2. What's genuinely good (keep doing this)

- **Module boundaries are clean and acyclic.** `:core` and `:common` have zero Android deps; `:core` is framework-free (domain models carry no Room/Android annotations); direction is `:core ← :data ← :ui ← :app` with no cycles — verified across every `build.gradle.kts`.
- **Textbook dependency inversion.** Repository interfaces live in `:core`, are implemented in `:data`, and are bound via Hilt `@Provides` returning the interface, so ViewModels depend on abstractions.
- **Swappable backend.** `IBackendService` + `BackendServiceStub`/`FirebaseBackendService` and `IAuthService` + `Mock`/`Firebase`, selected by `BuildConfig` flags through Hilt — clean and demoable without real Firebase.
- **Room migrations are done properly.** 9 additive, hand-written migrations (v1→v10), no `fallbackToDestructiveMigration`, so upgrades preserve user data.
- **Read-side offline-first is real.** Cache-first reads with online fallback, and network-failure fallback to cache, applied consistently.
- **Coroutine hygiene is mostly sound.** No `GlobalScope`, no `runBlocking`, no `runBlocking`-on-main; ViewModels use `viewModelScope`; Firebase snapshot listeners and the connectivity callback correctly use `callbackFlow { … awaitClose { remove() } }` — **no listener leaks**.
- **Navigation is well-built.** Single shell `NavController` with nested per-tab graphs, correct `popUpTo(findStartDestination)/saveState/restoreState/launchSingleTop`, overlay graphs with `saveState=false`, and defensively-parsed typed nav args.
- **The test suite is better than the project's own audit admits** (see §8) — real repository/ViewModel tests with `MockK`, a `MainDispatcherRule` over `StandardTestDispatcher`, and 17 Compose instrumentation test classes.

---

## 3. Critical findings (P1 — fix before any real-backend / release work)

> Several P1s are the same root issue surfaced from different angles; they are grouped here.

### 3.1 The `release` build routes to an unimplemented Firebase backend
`app/build.gradle.kts:37` sets `USE_MOCK_BACKEND=false` for `release`, but **14 `IBackendService` methods in `FirebaseBackendService.kt:484–524` return `Result.Error("…not yet implemented")`** (the error bodies; line 484 is the first method signature): support tickets, claims/disputes, cancel-with-reason, edit-booking, refunds, help articles, and saved locations. The `Stub` fully implements all of these, so the app looks feature-complete in debug and silently fails for those flows in release. The release also targets a **placeholder `google-services.json`** (Auth/Firestore/FCM cannot initialize).
**Fix:** Keep `USE_MOCK_BACKEND=true` until the backend is real, *or* implement these methods (mirroring the Stub's stores) and supply a real Firebase project. Gate the release variant in CI on backend completeness so it fails loudly, not silently.

### 3.2 User-facing strings are hardcoded — the multi-language feature does not work
> ✅ **Resolved 2026-07-27 (Track B, PRs #7–#17)** — `stringResource` is now used in **46 of 83**
> `:ui` files across **608** references, against 531 strings + 16 plurals key-complete in five
> locales. The paragraph below is the original 2026-06-25 finding, retained for history; see §9.
> The one part still open is its last sentence: **no `HardcodedText` lint check was ever added.**

`stringResource` is used in **1 of 83 UI files** (`LanguageSelectorScreen`). ~350 hardcoded `Text("…")` string literals render English (351 on a single line; ~700 `Text(` call sites in total, but many take variables — e.g. `Text(titleForRoute(...))`), pervasively across every screen. The 845 translated strings are dead resources; selecting Español/Français/العربية changes only the language-picker screen. *(Corollary: `contentDescription` labels are also hardcoded English, so TalkBack stays English in every locale.)*
**Fix:** Replace literals with `stringResource(R.string.*)` mapping to the already-translated keys — largely mechanical since the translations exist. Add a Lint/CI check (`HardcodedText`) to prevent regression.

### 3.3 `RecurringBookingViewModel.getCurrentSession()` suspends forever
`ui/subscription/RecurringBookingViewModel.kt:178–183` collects `getCurrentUserSession()` — a **hot, never-completing DataStore flow** — and uses `return@collect`, which returns only from the lambda for the current emission, **not** from `collect()`. The suspend function therefore never returns, so recurring-booking submit/load hangs.
**Fix:** Replace the whole `collect{}` helper with `.first()` (the idiom every other ViewModel already uses, e.g. `CleanerViewModel.providerId():86`).

### 3.4 `MessageRepository.sendMessage` silently loses messages sent offline
`data/repository/MessageRepository.kt:114–143` writes the message locally and reports `Result.Success` even when the backend send fails/offline, and never queues a retry. The message looks "sent" but never reaches the recipient and is not retried on reconnect — chat data loss.
**Fix:** Enqueue into the sync queue (add an `EntityType.MESSAGE` path) on offline/failure and drain on reconnect, or surface a pending/failed state instead of `Success`.

### 3.5 `createReview` never persists `direction` (breaks review separation)
`FirebaseBackendService.kt:194–211` drops the `direction` field (and `locationTags`) when building the `ReviewDto`. Since three read queries filter on `direction`, every persisted review defaults to `CLIENT_REVIEWS_PROVIDER`, so provider→client reviews are mis-filed against real Firestore. *(README #10 — still true.)*
**Fix:** Add `direction = review.direction` (and `locationTags`) to the DTO.

### 3.6 Security must-fixes before Firebase go-live
- **Plaintext auth token + PII** in a standard DataStore (`PreferencesDataStore.kt:16,20,66`). At review time the README *advertised* "Encrypted user session storage" (at `README.md:36,101` **as the file stood on 2026-06-25** — those line numbers no longer resolve to that text) — a self-contradiction with its own Known-Issue #4. *(Corrected 2026-07-27: the README no longer makes that claim — `6f37ca4` (PR #4) rewrote both lines to state plaintext explicitly, now at `README.md:35,108`. The DataStore is still unencrypted, so the fix below stands.)* **Fix:** wrap with `androidx.security:security-crypto` / Keystore-backed encryption; store the token separately and exclude from backup.
- **No Firestore Security Rules** anywhere in the repo. All access control is client-side query filters; once Firebase is live, any authenticated user can read/write any document. **Fix:** add and deploy `firestore.rules` enforcing per-document ownership before enabling the Firebase backend.
- **Payments fabricated as `SUCCEEDED`** with no PSP (`FirebaseBackendService.kt:267`, `BackendServiceStub.kt:531`). A booking is marked paid with no money moving. **Fix:** integrate a real PSP and derive status only from a server-side webhook; never let the client assert success.

---

## 4. Important findings (P2)

**Data / correctness**
- **Sync queue is dead code** — `SyncManager.queueOperation()` (`SyncManager.kt:37–57`) has no production callers; offline writes are dropped, not queued. Either wire repositories to enqueue write ops on offline/failure, or **delete the queue/backoff/conflict subsystem and correct the README/AUDIT**, which both claim "write operations … sync queue with exponential backoff retry."
- **More hard-stubbed repo methods than the audit admits:** `ClientRepository.getCurrentClient()` (`:85–93`, also `observeCurrentClient` hardcodes the DB key `"current_user"` → always `null` for real users), `ProviderRepository.updateProvider()` (`:120–132`), `PaymentRepository.getPaymentsForProvider()` (`:119`) & `updatePaymentStatus()` (`:132`), `ServiceRepository.updateService()` (`:151`). All compile, all fail at runtime.
- **`searchProvidersByCategory` ignores category, lat/long/radius** (`ProviderRepository.kt:87–118`) — returns the first 20 cached providers regardless; silently breaks discovery.
- **`getReviewsForBooking` never hits the backend** (`ReviewRepository.kt:87–98`) — returns `null` if the review is only server-side, allowing duplicate reviews. Contrast the correct `getReviewForBookingByDirection`.
- **Firebase `createBooking` uses a placeholder `price=0.0` / 60-min duration** (`FirebaseBackendService.kt:125–148`) — the Stub resolves price/duration from the seeded service (`BackendServiceStub.kt:326–349`); the Firebase path does not, so real bookings record `totalPrice = 0`. *(`cleaningType`/`locationType` are not in `CreateBookingRequest`, so neither path carries them — not a Firebase-specific loss.)*
- **Captive-portal false positive** — `ConnectivityObserver.isOnline()` checks `NET_CAPABILITY_INTERNET` but not `NET_CAPABILITY_VALIDATED` (`:57–71`). On hotel/airport WiFi, writes attempt and fail confusingly. *(README #12 — still true.)*
- **Room `exportSchema=true` but no `schemaLocation`** (`AppDatabase.kt:59`) — no schema JSONs, so the 9 migrations cannot be validated by `MigrationTestHelper`. A migration typo would crash at runtime untested.

**Remote / backend**
- **FCM token saved locally but never registered server-side** (`DiamondsFcmService.kt:82–92`) — nothing reads `observeFcmToken()` to upload it, so targeted push can never be delivered.
- **`FirebaseAuthService.signup` creates an Auth user but no Firestore profile doc** (`:47–77`) — `getClient/getProvider(uid)` then throw "not found" immediately after signup, and the chosen role is lost.
- **`searchProviders` is an unfiltered collection scan** (`:76–85`) and **`getConversationsForUser` runs two unbounded queries merged in memory** (`:350–358`) — cost/latency blockers at scale. *(README #13 — still true.)*

**Architecture / DI / state**
- **UI layer depends on concrete `:data` classes**, not `:core` abstractions — `ConnectivityObserver` in `BaseViewModel.kt:6,16` (and 16+ ViewModels), plus direct `LocationRepository`/`PreferencesDataStore` imports in `MapViewModel`/`LanguageViewModel`. Couples `:ui` to data internals; hurts testability. **Fix:** introduce a connectivity interface in `:core`/`:common`.
- **Test/prod DI parity gap** — `FakeRepositoryModule` replaces the entire `RepositoryModule` but **omits `ISavedLocationRepository`** (provided in `Modules.kt:250`). Any instrumentation test touching `SavedLocationsViewModel` fails the Hilt graph with a missing binding.
- **One-off events modelled as sticky `StateFlow` booleans** (`loginSuccess`, `bookingSuccess`, `paymentSuccess`, `isSuccess`…). `RecurringBookingSetupScreen.kt:58–59` never clears `isSuccess`; the `LaunchedEffect` is keyed on `isSuccess` so it won't re-fire on a plain recomposition, but it re-fires on screen re-entry / configuration change (the composable is recreated while the VM flag is still `true`). **Fix:** use `SharedFlow`/`Channel` for navigation/result signals.
- **`observe`-loaders leak collectors** — `ChatViewModel.loadConversations()` (`:71–93`) and `NotificationViewModel.loadNotifications()` nest an infinite `observeX().collect` inside a never-completing `getCurrentUserSession().collect`, re-launched on every call. **Fix:** `flatMapLatest(session) → stateIn(WhileSubscribed)`.

**Compose / UX / a11y**
- **Emoji used as Material icons and interactive controls** — bottom-nav tab "icons" (`BottomTab.kt:22–39`), the chat button, `⏸ Pause`/`▶ Resume` (`SubscriptionManagementScreen.kt:190,194`), every empty/error state. No `contentDescription`, font/OS-dependent (tofu risk), not themable. **Fix:** use `androidx.compose.material.icons.*` wrapped in `Icon(contentDescription=…)`.
- **Zero screens use `collectAsStateWithLifecycle`** despite `lifecycle-runtime-compose` being on the classpath — 87 `collectAsState()` call sites across 44 files keep collecting Room/Firestore/poll flows while backgrounded (battery/CPU). **Fix:** swap to the lifecycle-aware collector.

**Security (release-prep)**
- `android:allowBackup="true"` with empty backup rules (`AndroidManifest.xml:15`) → the plaintext token is in `adb backup`. Set `false` or exclude the DataStore.
- `autoVerify="true"` on a custom `diamonds://` scheme (`:43`) is a no-op (only valid for http App Links with assetlinks.json); any app can register the same scheme and intercept links. Treat deep-linked IDs as untrusted.
- `ACCESS_BACKGROUND_LOCATION` (`:9`) declared without justification or runtime rationale — foreground suffices; triggers extra Play review.

**Build/release**
- `applicationId = com.example.diamonds` (`:17`); **no `signingConfig`** (release APK unsigned); **no image-loading library (Coil)** anywhere — remote images can't render; empty `MAPS_API_KEY` (`gradle.properties:25`) → blank map; **`:core` annotates `@Serializable` but never applies the `kotlin-serialization` plugin** (`core/build.gradle.kts`) → `DomainModel.serializer()` fails to compile if ever used (latent).

**Testing**
- **`SyncManager` (333 LOC), both Workers, `PreferencesDataStore`, `ConnectivityObserver`, and ~6 repositories have zero unit tests** — the highest-risk, most stateful code is unverified. **Fix:** test the sync loop/backoff/conflict paths and `doWork()` success/retry/failure with the WorkManager test harness.

---

## 5. Minor findings (P3)

- `isMinifyEnabled=false` + empty `proguard-rules.pro` (acceptable today, but write keep rules *before* enabling minify so Hilt/Room/Firebase/serialization reflection doesn't break).
- No injected `DispatcherProvider` — the entire data layer inherits the caller's dispatcher (fine for Room/DataStore/Firebase await, brittle for any future CPU/IO work, hard to test).
- `DiamondsFcmService` `serviceScope` and `DiamondsApplication.applicationScope` are never cancelled (minor scope leaks; cancel in `onDestroy`).
- `DiamondsApplication` manually constructs `ConnectivitySyncTrigger` (`:48–49`) while the Hilt-provided `@Singleton` (`Modules.kt:112`) is dead code — it *works* (same singletons), but inject it instead. `FirestoreBookingListener` is similarly provided but never injected.
- No domain/use-case layer — multi-repository orchestration leaks into ViewModels.
- `:common` declares Compose/Material3/DataStore deps it never uses; god-objects (`BackendServiceStub` 1,131 lines, aggregated `Mappers`/`Daos`/`Entities`/`Modules`).
- Hand-rolled JSON for `imageUrls`/`locationTags` (`ReviewRepository.kt:179`) doesn't escape embedded quotes/backslashes.
- `composeOptions kotlinCompilerExtensionVersion = "1.5.0"` lags Compose 1.6.0 (audit says use `1.5.8`); currency hardcoded `"$"` + `String.format` without explicit `Locale`; `org.gradle.parallel` commented out; stale deps (Kotlin 1.9 / Compose 1.6 / AGP 8.5).
- The boilerplate `ExampleUnitTest`/`ExampleInstrumentedTest` have been removed; `Turbine` is a declared dependency but **never used** (VM tests assert only final `StateFlow.value`, not the loading→loaded transition); a few instrumentation tests are effectively un-failable due to OR-ed lenient `waitUntil` conditions.

---

## 6. Corrections to the project's own audit (`AUDIT_REPORT.md` / README)

The existing self-audit is good, but parts are now **stale or inaccurate** — worth fixing so the docs stay trustworthy:

| Claim in repo docs | Reality in current source |
|---|---|
| README #9 / AUDIT: "runtime locale switching never wired (`setApplicationLocales` not called)" | **Inaccurate** — it *is* wired in `LanguageSelectorScreen.kt:87`. (The real bug is hardcoded strings, §3.2.) |
| README #18 / AUDIT: "`BookingRepositoryTest` is only a no-op `assert(true)`" | **Fixed** — it now has 6 real assertion-bearing `@Test` methods (+10 in `BookingRepositoryFullTest`). |
| AUDIT: "estimated coverage ~15–20%" | **Understated** — 49 test files vs 124 prod files, with substantive repository/VM/instrumentation tests. Real gaps are specific (sync/workers/DataStore), not breadth. |
| README #13: "`getReviewsForProvider` full collection scan" | **Partially inaccurate** — that method *is* filtered (`:213–220`); `searchProviders`/`getConversationsForUser` are the real scans. |
| README #15: deep-link "http intercept" | **Inaccurate** — there is no http intent-filter; the real risk is custom-scheme hijack. |
| AUDIT: "`BaseViewModel` centralises error/loading for all VMs" | **Incomplete** — ~6 support VMs extend plain `ViewModel` and re-implement their own error/loading. |
| README features: "Encrypted user session storage / Encrypted preferences" | **False / contradicts Known-Issue #4** — the token is plaintext. *(Corrected 2026-07-27 in `6f37ca4` (PR #4): `README.md:35,108` now say plaintext. The code defect remains — see §9.1.)* |

The reviewers also **confirmed as a non-issue** one candidate finding (WorkManager default-initializer not removed): harmless at the declared versions because `Configuration.Provider` is implemented and `onDemand` init isn't required here.

---

## 7. Build state (important context)

The `final_build.txt` / `build_out.txt` / `test_build.txt` logs committed at review time (since deleted — see §9) showed `:ui:compileDebugKotlin FAILED` — but these are **stale runs from a Windows machine**, and **every specific error cited across the captured logs is already fixed** in current source: `ChatScreen` imports `heightIn` (`:11`, used `:159`) — the error in `final_build.txt`/`build_out.txt`/`test_build.txt`; `SubscriptionManagementScreen` no longer references `Icons.*.Pause/PlayArrow` (`ui_build2.txt`); and the `ReviewRepository` serialization type-mismatch (`build_phase14.txt`) is gone — it now builds JSON by hand rather than via `Json.encodeToString`. The static correctness sweep found **no P0 will-not-compile defects**, and version-sensitive APIs (PullToRefresh on Material3 1.2.0, AutoMirrored icons on icons-core 1.6.0, experimental `@OptIn` gating) line up with the declared dependency set.
**Caveat:** there is no Android SDK in the review environment, so a real `./gradlew assembleDebug` was **not** run — these compile conclusions are high-but-not-certain. **Recommendation:** add CI that actually builds + runs `allUnitTests` so a green/red signal exists and stale logs stop being the only evidence.

---

## 8. Recommended action plan (sequenced)

**Phase A — Make the docs true (cheap, high trust value)**
1. Decide the offline-write story: either wire `queueOperation()` into write repos, or delete the dead sync/backoff/conflict code. Update README/AUDIT to match.
2. Fix the two outright bugs: `RecurringBookingViewModel.getCurrentSession()` (§3.3) and `MessageRepository.sendMessage` offline loss (§3.4).
3. Correct the stale/inaccurate doc claims in §6 (especially the "encrypted storage" contradiction).
4. Add CI: build + `allUnitTests` on every push; add the `HardcodedText` lint as a warning baseline.

**Phase B — Make it actually multilingual & accessible**
5. Externalize the ~350 hardcoded `Text` literals to `stringResource` (and `contentDescription`); replace emoji "icons" with Material icons.

**Phase C — Before flipping `USE_MOCK_BACKEND=false`**
6. Implement the 14 `FirebaseBackendService` stubs + the repo stubs (`getCurrentClient`, `updateProvider`, payment/service methods); persist `direction` in `createReview`; populate price/duration in Firebase `createBooking`; create the profile doc on signup; register the FCM token server-side.
7. Encrypt the session store; set `allowBackup=false`/exclude DataStore; write & deploy Firestore Security Rules; integrate a real PSP.
8. Real `google-services.json`, real `applicationId`, `signingConfig`, keep rules + `isMinifyEnabled=true`.

**Phase D — Hardening**
9. Test `SyncManager`/Workers/`PreferencesDataStore`/`ConnectivityObserver`; use Turbine to assert state transitions; export Room schemas + migration tests.
10. Introduce a `:core` connectivity interface + `DispatcherProvider`; switch to `collectAsStateWithLifecycle`; fix `ConnectivityObserver` validated-capability check; cancel app/service scopes.

---

## 9. Addendum — 2026-07-27 re-verification (`develop` @ `4892952`)

**Re-verified by:** Tech Lead — same method as the original pass (static re-read of every cited file, this time at `develop` @ `4892952`).
**Date:** 2026-07-27
**Scope at re-verification:** the same 5 modules, now **124 production Kotlin files / 25,598 production LOC**, **25 ViewModels**, Room **v10** with 9 hand-written migrations, **26 JVM unit-test files carrying 300 `@Test` methods**, and **23 files under `app/src/androidTest`**.

**What changed.** Roadmap Track **A** (make the docs true, fix the two outright bugs, add CI) and Track **B** (externalize strings) are **done**. Track **D** is **partial**. §7's open question is settled: `.github/workflows/ci.yml` now runs `./gradlew assembleDebug allUnitTests` under JDK 17 on every PR to `develop` and every push to `develop`, so a real green/red signal exists and the stale `*_build.txt` logs have been deleted from the repo.

**What this review got wrong in the other direction.** §3.1 framed the release build as *partly* implemented — 14 stubbed methods against otherwise-working Firestore code. That framing was too generous. Every Firestore **read** in `FirebaseBackendService` fails at runtime, for a reason no document in this repo has recorded until now (§9.4). Flipping `USE_MOCK_BACKEND=false` today does not produce a partly-working app; it produces a **write-only** one.

**Bottom line for this addendum:** Track **C is entirely untouched and is now the single gate on production.** Nothing in §3.1, §3.5, §3.6 or the backend half of §4 has moved.

### 9.1 §3 (P1) — finding-by-finding status

| § | Finding | Status at 2026-07-27 |
|---|---|---|
| 3.1 | `release` routes to an unimplemented Firebase backend | **STILL OPEN — verbatim.** All 14 `Result.Error("…not yet implemented")` bodies remain at `FirebaseBackendService.kt:484–524`; `app/google-services.json` is still the placeholder (`project_number` `"000000000000"`, `api_key` `"placeholder-key-for-testing"`). See also §9.4, which makes this strictly worse. |
| 3.2 | Hardcoded UI strings / non-functional multi-language | **LARGELY SUPERSEDED** — the externalization is done (PRs #7–#17); the regression guard is not. Detail below. |
| 3.3 | `RecurringBookingViewModel.getCurrentSession()` suspends forever | **FIXED** in `ae1d33c` (PR #2). `RecurringBookingViewModel.kt:193` now uses `authRepository.getCurrentUserSession().first()`, with a comment recording why `collect{}`/`return@collect` was wrong. |
| 3.4 | `MessageRepository.sendMessage` silently loses offline messages | **FIXED** in `ae1d33c` (PR #2). `MessageRepository.kt:131–150` returns `Result.Error(OfflineException(…))` when offline and propagates a backend `Result.Error` instead of reporting `Success`. *(The message is still not queued for retry — that is the §4 sync-queue decision, not this bug.)* |
| 3.5 | `createReview` never persists `direction` | **STILL OPEN — verbatim.** `direction` and `locationTags` are still dropped when building the `ReviewDto`. |
| 3.6 | Security must-fixes before Firebase go-live | **STILL OPEN — verbatim on the code side**, one doc-side clause corrected. Detail below. |

**§3.2 in detail.** The claim "`stringResource` is used in 1 of 83 UI files / ~350 hardcoded `Text("…")` literals" is **no longer true and should not be cited again**. Current state: `stringResource` is used in **46 of the 83 `.kt` files under `ui/src/main`, across 608 call sites**. `ui/src/main/res/values/strings.xml` holds **531 `<string>` + 16 `<plurals>`**, and `values-fr`, `values-es`, `values-pt` and `values-ar` are key-complete against it. **~67 residual `Text("…")` literals** remain, almost all non-translatable glyphs (emoji, `"$"`, `"›"`). Every `contentDescription` in `:ui` now resolves to a localized `cd_*` key — zero hardcoded literals remain, so the TalkBack corollary in §3.2 is also resolved. **What is still open** is the second half of the §3.2 fix: there is **no Lint rule and no lint baseline for hardcoded strings anywhere in the repo**, and CI does not run `lint` at all (§9.5.6), so nothing prevents regression.

**§3.6 in detail.** The self-contradiction called out in §3.6 and §6 is gone on the *documentation* side: `README.md:35` and `README.md:108` now state plainly that the auth token **and** profile are stored in plaintext, corrected in `6f37ca4` (PR #4). The README no longer advertises "Encrypted user session storage" — treat the §3.6/§6 wording about that claim as **historical**. The *code* defect is untouched: `PreferencesDataStore.kt:16` is still an ordinary unencrypted DataStore, there are still **zero** Firestore Security Rules in the repo (no `firestore.rules`, no `firebase.json`, no `.firebaserc`), payments still fabricate `status = "SUCCEEDED"` client-side at `FirebaseBackendService.kt:267`, and `android:allowBackup="true"` still ships with empty backup-rule files.

### 9.2 §4 (P2) — finding-by-finding status

| Finding (§4) | Status at 2026-07-27 |
|---|---|
| Sync queue is dead code | **STILL OPEN — the decision, not the docs.** `SyncManager.kt:37` remains the only production definition of `queueOperation()`; the only other occurrences repo-wide are the interface declaration (`core/…/Repositories.kt:147`) and the androidTest fake (`FakeRepositories.kt:451`). README/AUDIT no longer overstate it (corrected in `cb45abc`, PR #6), so this is now purely the wire-it-or-delete-it code decision — **the last Track A item open**. |
| Hard-stubbed repo methods (`getCurrentClient`, `updateProvider`, payment/service methods) | **STILL OPEN.** `ClientRepository.kt:85–88` still returns `Result.Error(Exception("Not implemented"))` behind a `// TODO`. |
| `searchProvidersByCategory` ignores category/lat/long/radius | **STILL OPEN.** `searchProviders` likewise ignores `latitude`/`longitude`/`radius` entirely. |
| `getReviewsForBooking` never hits the backend | **STILL OPEN, and slightly worse than described.** `ReviewRepository.kt:87–97` checks the cache, returns `Result.Success(null)` when offline, and then returns `Result.Success(null)` again — there is no backend call on *any* path. |
| Firebase `createBooking` uses placeholder price/duration | **STILL OPEN** — `totalPrice = 0.0`, `estimatedDuration = 60`. |
| `ConnectivityObserver` captive-portal false positive | **STILL OPEN.** `ConnectivityObserver.kt:60` still checks only `NET_CAPABILITY_INTERNET`, never `NET_CAPABILITY_VALIDATED`. |
| Room `exportSchema=true` with no `schemaLocation` | **STILL OPEN**, and now demonstrably costly — see §9.5.5. |
| FCM token never registered server-side | **STILL OPEN.** `observeFcmToken()` has zero callers. |
| `FirebaseAuthService.signup` creates no Firestore profile doc | **STILL OPEN**, and it also discards the `role` argument. |
| `searchProviders` / `getConversationsForUser` unbounded queries | **STILL OPEN.** |
| `:ui` depends on concrete `:data` `ConnectivityObserver` | **STILL OPEN.** `BaseViewModel.kt:6,16` still imports and takes `com.example.diamonds.data.connectivity.ConnectivityObserver`; no connectivity abstraction exists in `:core`/`:common`. |
| Test/prod DI parity gap (`FakeRepositoryModule`) | **STILL OPEN** — re-confirmed with exact numbers in §9.5.2. |
| One-off events modelled as sticky `StateFlow` booleans | **STILL OPEN.** `RecurringBookingSetupScreen.kt:60–62` still keys a `LaunchedEffect` on `state.isSuccess` with nothing clearing the flag. |
| `observe`-loaders leak collectors | **STILL OPEN.** `ChatViewModel.loadConversations()` (`:71–…`) still nests `observeConversationsForUser().collect` inside a never-completing `getCurrentUserSession().collect`. |
| Emoji used as Material icons | **STILL OPEN** — the ~67 residual `Text("…")` literals from §9.1 are largely these glyphs. |
| Zero `collectAsStateWithLifecycle` | **STILL OPEN, unchanged:** 0 uses versus **87 `collectAsState()` call sites across 44 files**. |
| `allowBackup="true"`, `autoVerify` on a custom scheme, `ACCESS_BACKGROUND_LOCATION` | **STILL OPEN** — and the permission point is broader than stated; see §9.5.8. |
| Build/release (`applicationId`, no `signingConfig`, no Coil, empty `MAPS_API_KEY`, `:core` `@Serializable` without the plugin) | **STILL OPEN, all re-verified.** `applicationId` is still `com.example.diamonds` (Play rejects `com.example.*`); there is no `signingConfig` anywhere; no `coil` dependency exists in any build file or the version catalog; `core/build.gradle.kts` applies only `java-library` + `kotlin.jvm` while `DomainModels.kt:11` and others annotate `@Serializable` (`:data` *does* apply `org.jetbrains.kotlin.plugin.serialization`, `data/build.gradle.kts:6`). |
| Zero tests for `SyncManager`, Workers, `PreferencesDataStore`, `ConnectivityObserver`, ~6 repositories | **PARTIALLY FIXED.** Track D added `SyncManagerTest.kt` (**21 `@Test` methods**) and `LocationRepositoryTest.kt` (**8**), and removed the boilerplate `Example*Test` files; PRs #19–#21 fixed three real `SyncManager` correctness bugs found while writing those tests (a backend `Result.Error` being marked `SYNCED`, and two non-exhaustive dispatch paths). **`SyncWorker`, `RecurringBookingWorker`, `PreferencesDataStore` and `ConnectivityObserver` still have zero tests.** |

### 9.3 §5 (P3) and §6 — status

Every §5 item was re-checked and **all remain open**, unchanged in substance: `isMinifyEnabled=false` with the untouched empty `proguard-rules.pro` template; no injected `DispatcherProvider`; `DiamondsFcmService.serviceScope` (`:36`) and `DiamondsApplication.applicationScope` (`:32`) still never cancelled; `DiamondsApplication.kt:48` still hand-constructs `ConnectivitySyncTrigger` while the Hilt provider at `Modules.kt:114–118` stays dead; no domain/use-case layer; `:common` still declares unused deps; hand-rolled JSON in `ReviewRepository`; stale toolchain versions. One §5 item moved: the boilerplate `Example*Test` files are gone (Track D), but **Turbine is still declared in both `data/build.gradle.kts` and `ui/build.gradle.kts` with 0 uses** — VM tests still assert only the final `StateFlow.value`.

§6's corrections table is **still accurate and still worth keeping**, with one row now historical: *"README features: Encrypted user session storage"* was corrected in `6f37ca4` (PR #4) and the README now says plaintext (`README.md:35,108`). The other §6 retractions (locale switching *is* wired; `BookingRepositoryTest` is real; the ~15–20% coverage estimate is understated; `getReviewsForProvider` *is* filtered) hold at `4892952`.

### 9.4 NEW — P1, blocking: every Firestore *read* fails, because no DTO has a no-arg constructor

This is the single most consequential finding of the re-verification, and it is not recorded in any existing document.

**Mechanism, in four steps:**

1. `FirebaseBackendService` deserializes exclusively through Firestore's reflective object mapper — `DocumentSnapshot.toObject(T::class.java)` and `QuerySnapshot.toObjects(T::class.java)`. That mapper instantiates the target type via a **public no-argument constructor** and then populates fields. There is no other supported instantiation path for Java-class-based mapping.
2. Kotlin emits a synthetic no-arg constructor for a `data class` only when **every** primary-constructor parameter has a default value. **All 15 `*Dto` types in `data/src/main/java/com/example/diamonds/data/remote/backend/IBackendService.kt` have at least one parameter without a default** (15 of the file's 25 `data class`es are `*Dto` types; the other 10 are `*Request` payloads, which are only ever written, never read back via `toObject`) — `ClientDto` (`:113`) requires `id`, `name`, `email`, `phoneNumber`, `createdAt`, `updatedAt`; `BookingDto` (`:162`) requires `id`, `clientId`, `providerId`, `serviceId`, `status`, `scheduledDate`, `scheduledTime`, `estimatedDuration`, `totalPrice`, `address`. So **no DTO in the file has a no-arg constructor**, and the mapper cannot construct any of them.
3. `FirebaseBackendService.kt` calls `toObject`/`toObjects` **26 times** — that is every read path in the class (`getClient:59`, `getProvider:72`, and so on).
4. Every one of those calls sits inside `firestoreCall { … }` (`FirebaseBackendService.kt:531–537`), a blanket `try { Result.Success(block()) } catch (e: Exception) { Result.Error(e) }`. The mapper's runtime exception is therefore **swallowed and surfaced as a generic `Result.Error`** — no crash, and no log line either (the codebase has zero `Log.*` calls by design). The failure is indistinguishable from a network error.

**Writes are unaffected.** `set(dto)` / `add(dto)` serialize *out* of an existing object via getters and never need to construct one — `updateClient` (`:63–66`) writes its input and returns it without a round-trip.

**Net effect: with `USE_MOCK_BACKEND=false` the app successfully writes documents to Firestore and can read none of them back.** Combined with §3.1 (14 stubbed methods) and the placeholder `google-services.json`, the release variant is further from working than this review originally judged. Note that the `@Serializable` annotations on these DTOs are irrelevant here — kotlinx.serialization is not what Firestore's mapper uses.

**Fix (pick one):** give every DTO parameter a default (`val id: String = ""`, …) so Kotlin emits the no-arg constructor — cheapest, but it makes every field silently optional; **or** stop using the reflective mapper and map `snapshot.data` explicitly; **or** add explicit no-arg secondary constructors with `@PropertyName` annotations. Whichever is chosen, it needs an emulator- or Firestore-emulator-backed test: a JVM unit test cannot catch this, and today **nothing in the test suite references `FirebaseBackendService` at all**.

### 9.5 Other new findings (2026-07-27)

**9.5.1 — A shipped instrumentation test is broken by the Track B externalization.** `app/src/androidTest/…/LoginScreenTest.kt:153` asserts `onNodeWithText("Demo accounts  (tap to fill)", substring = true)` — **two** spaces — while the string the screen now renders, `R.string.demo_accounts_hint` (`ui/src/main/res/values/strings.xml:47`), is `Demo accounts (tap to fill)` with **one**. The assertion cannot match; the test fails on any device run. It went unnoticed because CI never compiles or runs `androidTest` (§9.5.6).

**9.5.2 — The androidTest Hilt graph is incomplete** (§4 flagged this; re-confirmed with exact numbers). `app/src/androidTest/java/com/example/diamonds/di/FakeRepositoryModule.kt` declares **13 `@Provides`** and is annotated `@TestInstallIn(… replaces = [RepositoryModule::class])` (`:28–30`) — it replaces the production module **wholesale** — yet it provides no `ISavedLocationRepository`, which production `Modules.kt` does provide. Any instrumentation test that reaches a saved-locations screen fails Hilt graph construction with a missing binding. Replacing a module wholesale means the fake set must be kept exhaustive by hand; nothing enforces that today.

**9.5.3 — `SavedLocationRepository` reports remote failure as success.** `SavedLocationRepository.kt:32` maps `is Result.Error -> Result.Success(emptyList())` and `:45` maps `is Result.Error -> Result.Success(location)`. The second is **exactly the silent-data-loss class of defect that §3.4 identified and Track A fixed in `MessageRepository`**: the location is written to Room, the remote upsert fails, and the UI is told the save succeeded. The first turns a backend outage into "you have no saved locations", indistinguishable from an empty account. `deleteSavedLocation` (`:50–54`) discards the backend result entirely and returns `Result.Success(Unit)` unconditionally. None of the three paths has a test.

**9.5.4 — 37 `as? Result.Success` sites collapse errors into `null` with no user-visible error.** Across **10 ViewModel files** (`CleanerViewModel`, `CleanerProfileViewModel`, `BookingViewModel`, `ReviewViewModel`, `ClientReviewViewModel`, `PaymentViewModel`, `CompanyViewModel`, `CompanyEarningsViewModel`, `MapViewModel`, `HelpCenterViewModel`), a `Result` is downcast with `as? Result.Success` and the `Error`/`Loading` arms are discarded. This directly contradicts the three-arm `Result` convention the project documents and that `ClientRepository` models: a failed load renders as an empty list or a blank field, with no message and no retry affordance. Worth treating as one cleanup rather than 37 individual bugs.

**9.5.5 — Room schemas are never exported, so all 9 migrations are unverified.** `AppDatabase.kt:59` sets `exportSchema = true`, but **no `room.schemaLocation` annotation-processor argument is configured in any build file** and there is **no `data/schemas/` directory**. Consequently `MigrationTestHelper` cannot be used, and the `testImplementation(libs.androidx.room.testing)` dependency declared at `data/build.gradle.kts:87` is **dead weight** — it is on the classpath and used by nothing. A typo in any of `MIGRATION_1_2 … MIGRATION_9_10` would ship undetected and crash on upgrade.

**9.5.6 — CI has a structural blind spot.** `.github/workflows/ci.yml` runs exactly one Gradle invocation: `./gradlew assembleDebug allUnitTests --stacktrace` (`:50`). There is **no `assembleDebugAndroidTest`** (so the 23 instrumentation-test files are never even compiled — which is precisely why §9.5.1 and §9.5.2 were invisible), **no `lint`** (which is why the §3.2 regression guard has nowhere to live), and **no coverage gate**. Separately, the `allUnitTests` aggregate (`build.gradle.kts:19–28`) depends on `:app`, `:common`, `:data` and `:ui` — but `:app` and `:common` have **no `src/test` directory at all**, so two of those four dependencies are no-ops, and **`:core` is excluded entirely** despite being the pure-JVM domain module and the easiest thing in the repo to test. The real unit-test surface CI exercises is `:data` (12 files) + `:ui` (14 files).

**9.5.7 — `PaymentScreen` collects raw card data in the app process.** `PaymentScreen.kt:171` (PAN) and `:218` (CVV) are plain `OutlinedTextField`s — no `PasswordVisualTransformation`, no PSP SDK anywhere in the dependency set — and **`FLAG_SECURE` is set nowhere in the repo (0 occurrences)**, so the unmasked PAN is capturable by screenshot and appears in the recents thumbnail. Together with §3.6's client-fabricated `SUCCEEDED`, this is a PCI-relevant path that must not ship as-is. The fix is not to mask the field: card entry should be delegated to the payment provider's own SDK or hosted field so the PAN never enters this process.

**9.5.8 — Two declared permissions are never requested or used.** `ACCESS_BACKGROUND_LOCATION` (`AndroidManifest.xml:9`) and `POST_NOTIFICATIONS` (`:11`) are declared, but there are **zero references to either constant anywhere in the Kotlin sources** — no runtime permission request exists for either. Background location invites extra Play review for a capability the app never exercises, and an unrequested `POST_NOTIFICATIONS` means the app silently displays no notifications on API 33+ — which quietly compounds §4's "FCM token never registered server-side".

### 9.6 Revised recommended sequence (supersedes §8 where they disagree)

§8's **Phase A** (bar one item) and **Phase B** are complete. What remains, in order:

1. **Close Track A's last item** — decide the offline-write story: wire `queueOperation()` into the write repositories, or delete the queue/backoff/conflict subsystem. The docs no longer overstate it, so this is now purely a code decision, and it is cheap relative to everything below.
2. **Fix the remaining silent-success bugs** — `SavedLocationRepository.kt:32,45` (§9.5.3) and the 37 `as? Result.Success` sites (§9.5.4). These are the surviving instances of the defect class that §3.4 already fixed once.
3. **Close the CI blind spot before adding more code** — add `assembleDebugAndroidTest` (and `connectedDebugAndroidTest` once an emulator runner exists), add `lint`, and add `:core` to `allUnitTests`. Then fix `LoginScreenTest` (§9.5.1) and the `FakeRepositoryModule` binding (§9.5.2), both of which that change surfaces immediately. Land the `HardcodedText` lint baseline here too — it is the unfinished half of §3.2.
4. **Track C, in this order** — it is one indivisible track, and (a) gates everything else in it:
   - **(a)** DTO no-arg constructors (§9.4). Until this is fixed, nothing else in Track C is even observable against a real Firestore.
   - **(b)** The 14 stubbed `FirebaseBackendService` methods (§3.1) and the hard-stubbed repository methods (§4).
   - **(c)** Correctness: `direction` + `locationTags` in `createReview` (§3.5); real price/duration in Firebase `createBooking`; profile doc + role on signup; server-side FCM token registration; real geo filtering in `searchProviders`.
   - **(d)** Security, none of it optional: Firestore Security Rules; a Keystore-backed encrypted session store; `allowBackup=false`; a real PSP owning card entry with a server-derived payment status (§9.5.7).
   - **(e)** Release plumbing: real `google-services.json`, a non-`com.example` `applicationId`, a `signingConfig`, keep rules + `isMinifyEnabled=true`.
5. **Finish Track D** — Room schema export + migration tests (§9.5.5); tests for both Workers, `PreferencesDataStore` and `ConnectivityObserver`; put the already-declared Turbine to work asserting state transitions; introduce `DispatcherProvider` and a `:core` connectivity interface; switch to `collectAsStateWithLifecycle`.

**Re-verified as still good** — nothing here has regressed, and it is worth saying plainly given the length of the list above: the module graph is acyclic and matches the documentation (`:common → :core`; `:data → :core`,`:common`; `:ui → :core`,`:data`,`:common`; `:app → all`), and `:core` remains a pure `java-library` with **zero** Android imports; there is still no `GlobalScope` and no `runBlocking` in any main source set; there are zero `Log.*`/`println` calls anywhere; no secrets have ever been committed; `PendingIntent` uses `FLAG_IMMUTABLE`; cleartext traffic is blocked by the `targetSdk 34` default; and read-side offline-first is genuinely implemented.

---

*This review was produced by reading the source on this PR branch at review time; each finding cites the file/line that substantiates it. Where a real Gradle build (with the Android SDK) would be needed to be certain, that is called out explicitly.*
