# CLAUDE.md

Guidance for Claude Code (and other AI agents) working in this repository.

## Project

**Diamonds** — an Android app for a peer-to-peer home-cleaning marketplace (customers ↔ cleaners/companies). Multi-module, offline-first, MVVM + Jetpack Compose. Package root: `com.example.diamonds`.

## Build, test & CI

JDK 17 is required to run the build (AGP 8.5); the project still targets JVM 11 bytecode.

```bash
./gradlew assembleDebug        # build the debug app (compiles every module)
./gradlew allUnitTests         # run JVM unit tests for :app/:common/:data/:ui (root aggregate task)
./gradlew :data:testDebugUnitTest   # a single module's unit tests
./gradlew installDebug         # install on a device/emulator
```

CI (`.github/workflows/ci.yml`) runs `assembleDebug allUnitTests` on every PR to `develop` and on pushes to `develop`. **Treat a red CI run as blocking.** Instrumentation tests live only in `:app/src/androidTest` and need an emulator (`./gradlew :app:connectedDebugAndroidTest`); they are not part of CI yet.

The debug build needs **no secrets**: it runs on mock/stub backends and the committed `google-services.json` (at `app/google-services.json`) is a placeholder; `MAPS_API_KEY` is read via `project.findProperty(...)` from a Gradle property (`gradle.properties`, `-PMAPS_API_KEY=…`, or `ORG_GRADLE_PROJECT_MAPS_API_KEY`) and defaults to empty.

## Module structure

```
:core    Pure Kotlin/JVM domain — models, Result<T>, repository INTERFACES. No Android deps.
:common  Small shared utilities/extensions.
:data    Room, repositories (impl), sync, connectivity, remote (Firebase + Stub/Mock).
:ui      Jetpack Compose screens, ViewModels, and the navigation host (DiamondsNavHost / AppShell).
:app     Entry point (MainActivity hosts the :ui nav graph), Hilt DI wiring, FCM, instrumentation tests.
```

Dependency direction is strict and acyclic — lower layers never depend on higher ones, and each upper module depends *directly* on the lower modules it uses (not only the adjacent one): `:common` → `:core`; `:data` → `:core`/`:common`; `:ui` → `:data`/`:core`/`:common`; `:app` → `:ui`/`:data`/`:core`/`:common`. **Do not** make `:core` depend on Android, and prefer depending on `:core` interfaces over `:data` implementations.

## Architecture conventions

- **MVVM + StateFlow.** ViewModels usually extend `BaseViewModel<UiState>`; expose read-only `StateFlow` via `asStateFlow()`/`stateIn(...)`; keep `UiState` an immutable data class updated with `copy()`. Launch work in `viewModelScope`.
- **`Result<T>`** (sealed `Success`/`Error`/`Loading`, in `:core`) is the error-handling type. When mapping a backend `Result`, propagate all three arms (`is Result.Loading -> Result.Loading`), matching e.g. `ClientRepository`.
- **Offline-first reads / online writes.** Reads fall back to the local Room cache when offline (or when a network call fails); when online they refresh from the backend — the exact order varies by repo (e.g. `BookingRepository.getClientBookings` is *network-first-when-online* with a cache fallback, whereas `getOrCreateConversation` checks the cache first). Writes require connectivity and report an offline error — most repos return `OfflineException` (see `BookingRepository`), though a few still return a generic `Exception("No internet connection")` (e.g. `SubscriptionRepository`, `MessageRepository.getOrCreateConversation`). *Note:* the `SyncManager` offline-write queue exists but is not currently wired into write repos — see `TECH_LEAD_REVIEW.md` §4 before relying on it.
- **Reading the session:** use `authRepository.getCurrentUserSession().first()` — it's a never-completing DataStore flow (DataStore's `.data` is a *cold* flow that keeps emitting and never completes), so `collect { … }` will suspend forever. Always handle a `null` first emission (logged out / not yet loaded).
- **DI:** Hilt. Repository interfaces (`:core`) are bound to implementations via `@Provides` in `app/src/main/java/com/example/diamonds/di/Modules.kt`. ViewModels are `@HiltViewModel`.
- **Swappable backend** via `BuildConfig` flags in `app/build.gradle.kts`:
  - `USE_MOCK_AUTH` / `USE_MOCK_BACKEND` = `true` in **debug** (use `MockAuthService` / `BackendServiceStub`), `false` in **release** (real Firebase). Several `FirebaseBackendService` methods are still unimplemented — see the review before flipping these.
- **Strings:** user-facing text should use `stringResource(R.string.*)`. Much of the UI currently hardcodes literals (Track B in the roadmap) — don't add new hardcoded strings.

## Working in this repo

- **Branch:** do feature work on a `claude/<topic>` branch off `develop`; open PRs against `develop` (not `main`). Keep PRs small and focused.
- **Reviews:** PRs are reviewed by the **Quasar-Apps** reviewer and the **GitHub Copilot** reviewer. Don't merge until **CI is green and both reviewers are green on the latest commit.** Reply to review feedback *within the review threads*, not as standalone PR comments. Verify any counts/citations against the source before stating them.
- Don't create a PR unless asked.

## Key docs

- `TECH_LEAD_REVIEW.md` — independent architectural review with file:line-cited findings (the current source of truth for known gaps).
- `ROADMAP.md` — the 🧭 Remediation Roadmap (Tracks A–D) supersedes the old "Next Steps".
- `ARCHITECTURE.md` — module/data-flow/repository/ViewModel patterns.
- `README.md` / `AUDIT_REPORT.md` — "Known Issues" list (kept reconciled with reality).
