# Quick Reference Card

## File Locations Cheat Sheet

```
🏠 Domain Models          → core/domain/model/DomainModels.kt
🗂️ Repository Interfaces  → core/domain/repository/Repositories.kt
📦 Room Entities          → data/local/entity/Entities.kt
🔍 DAOs                   → data/local/dao/Daos.kt
📡 Backend Service        → data/remote/backend/IBackendService.kt
🏪 Repository Impls       → data/repository/*.kt
🔄 Sync Manager           → data/sync/SyncManager.kt
🧠 ViewModel Base         → ui/base/BaseViewModel.kt
🎨 Theme                  → ui/theme/Theme.kt
📱 Screens + ViewModels   → ui/<feature>/*.kt   (see below — there is NO ui/screens/)
🧭 Navigation             → ui/navigation/DiamondsNavHost.kt, Screen.kt, BottomTab.kt
🌍 Strings                → ui/src/main/res/values{,-fr,-es,-pt,-ar}/strings.xml
💉 Hilt DI                → app/di/Modules.kt
📄 AndroidManifest        → app/src/main/AndroidManifest.xml
```

`:ui` is organised **by feature**, not by layer — each package holds its screens next to the
ViewModel that drives them:

```
ui/auth        ui/booking     ui/chat        ui/cleaner     ui/company
ui/customer    ui/map         ui/notification ui/payment    ui/profile
ui/review      ui/settings    ui/subscription ui/support    ui/sync
ui/base        ui/components  ui/navigation  ui/placeholder ui/shell
ui/splash      ui/theme
```

## Common Tasks

### Add New Entity

1. **Define Domain Model**
   ```kotlin
   // core/domain/model/DomainModels.kt
   data class MyEntity(val id: String, ...)
   ```

2. **Create Room Entity**
   ```kotlin
   // data/local/entity/Entities.kt
   @Entity(tableName = "my_entities")
   data class MyEntityEntity(...)
   ```

3. **Create DAO**
   ```kotlin
   // data/local/dao/Daos.kt
   @Dao
   interface MyEntityDao { ... }
   ```

4. **Add DAO to Database**
   ```kotlin
   // AppDatabase.kt
   abstract fun myEntityDao(): MyEntityDao
   ```

5. **Update Mappers**
   ```kotlin
   // data/mapper/Mappers.kt
   fun MyEntityEntity.toDomain() = MyEntity(...)
   fun MyEntity.toEntity() = MyEntityEntity(...)
   ```

### Add New Repository

1. **Add Interface**
   ```kotlin
   // core/domain/repository/Repositories.kt
   interface IMyRepository {
       suspend fun get(): Result<MyEntity>
   }
   ```

2. **Implement Repository**
   ```kotlin
   // data/repository/MyRepository.kt
   class MyRepository(...) : IMyRepository {
       override suspend fun get() = ...
   }
   ```

3. **Add Hilt Binding**
   ```kotlin
   // app/di/Modules.kt
   @Provides
   fun provideMyRepository(...): IMyRepository = MyRepository(...)
   ```

### Create New Screen

1. **ViewModel**
   ```kotlin
   // ui/<feature>/MyViewModel.kt   (existing feature package, or a new one)
   data class MyUiState(val items: List<Item> = emptyList())
   
   @HiltViewModel
   class MyViewModel @Inject constructor(...) : 
       BaseViewModel<MyUiState>(connectivityObserver, MyUiState())
   ```

2. **Screen Composable**
   ```kotlin
   // ui/<feature>/MyScreen.kt
   @Composable
   fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
       val state by viewModel.uiState.collectAsStateWithLifecycle()
       val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
       
       if (!isOnline) OfflineBanner()
       Text(stringResource(R.string.my_screen_title))   // no bare literals
   }
   ```

3. **Strings** — add every user-facing string (and every icon `contentDescription`, as a `cd_*` key)
   to `values/strings.xml` **and** to `values-fr`, `values-es`, `values-pt`, `values-ar`. All five
   locales are key-complete today (531 strings + 16 plurals); keep them that way. Nothing enforces
   this automatically — there is no lint rule or baseline for hardcoded strings.

## Offline-First Checklist

When implementing a feature that involves data:

- [ ] **Read Operations**: Use repository; fall back to the Room cache when the network is unavailable
- [ ] **Write Operations**: Check `connectivityObserver.isOnline()` before allowing
- [ ] **Disabled State**: Disable action buttons with tooltip when offline
- [ ] **Error Messages**: Show "Requires internet" for offline attempts
- [ ] **Never report failure as success**: map `Result.Error` to a visible error, not to an empty
      list or a fabricated `Result.Success`

⚠️ **Offline writes are not queued.** `SyncManager.queueOperation` has zero production callers, so
the sync queue is never populated. A write attempted offline fails fast with `OfflineException` and
is gone. Don't build UI that promises "we'll send this when you're back online", and don't add a
pending-operation badge until the wire-or-delete decision in `ROADMAP.md` Track A is made.

## Testing Checklist

For each repository:

- [ ] Write test for offline read (should return cached)
- [ ] Write test for offline write (should return OfflineException)
- [ ] Write test for online read (should fetch and cache)
- [ ] Write test for online write (should send to backend)

Example:
```kotlin
@Test
fun testOfflineWrite() {
    every { connectivityObserver.isOnline() } returns false
    val result = repository.create(entity)
    assert(result is Result.Error)
    assert(result.exception is OfflineException)
}
```

## Module Dependencies Quick Check

`:core` is the **bottom** of the graph: it depends on nothing and imports no Android APIs.
Everything else depends downward onto it.

```
:core    → nothing (pure java-library, zero Android imports)
:common  → :core
:data    → :core, :common
:ui      → :core, :data, :common
:app     → :ui, :data, :core, :common
```

```
Can :ui import from :data?        ✅ YES
Can :data import from :ui?        ❌ NO
Can :common import from :core?    ✅ YES
Can :core import from :common?    ❌ NO — :core depends on nothing
Can :data import from :core?      ✅ YES
Can :core import from :data?      ❌ NO
Can :core import anything Android? ❌ NO — it's a plain java-library module
```

## Build Commands

Needs **JDK 17** (AGP 8.5.0); the modules compile to JVM 11 bytecode. On Windows use `gradlew.bat`
in place of `./gradlew`.

```bash
# Build the debug app (compiles every module)
./gradlew assembleDebug

# Run all JVM unit tests (root aggregate task)
./gradlew allUnitTests

# One module's unit tests
./gradlew :data:testDebugUnitTest

# Run a specific test class
./gradlew :data:testDebugUnitTest --tests "*BookingRepositoryTest"

# Instrumentation tests (needs an emulator/device; NOT run by CI)
./gradlew :app:connectedDebugAndroidTest

# Build specific module
./gradlew :data:build

# Clean rebuild
./gradlew clean assembleDebug

# Install on device
./gradlew installDebug

# Check dependencies
./gradlew :app:dependencies
```

CI (`.github/workflows/ci.yml`) runs `assembleDebug allUnitTests` on JDK 17 for every PR to
`develop`. It does not run lint, instrumentation tests, or a coverage gate.

## Key Patterns

### Offline-First Read
```kotlin
suspend fun getItem(id: String): Result<Item> {
    // Cache first
    val cached = dao.getById(id)
    if (cached != null) return Result.Success(cached.toDomain())
    
    // Then fetch if online
    if (!connectivityObserver.isOnline())
        return Result.Error(OfflineException())
    
    return backendService.getItem(id)
}
```

### Online-Required Write
```kotlin
suspend fun createItem(item: Item): Result<Item> {
    // Check online requirement
    if (!connectivityObserver.isOnline())
        return Result.Error(OfflineException())
    
    // Send to backend immediately
    return backendService.createItem(item)
}
```

### ViewModel Action
```kotlin
fun createItem(item: Item) {
    if (!isOnline.value) {
        setError("Requires internet connection")
        return
    }
    
    viewModelScope.launch {
        updateState { it.copy(isLoading = true) }
        val result = repository.createItem(item)
        when (result) {
            is Result.Success -> {
                updateState { it.copy(isLoading = false) }
                // Navigate away
            }
            is Result.Error -> {
                setError(result.exception.message)
                updateState { it.copy(isLoading = false) }
            }
            is Result.Loading -> updateState { it.copy(isLoading = true) }
        }
    }
}
```

⚠️ **Handle all three arms.** `Result` is `Success` / `Error` / `Loading`, and an exhaustive `when`
is the convention. In particular, avoid `result as? Result.Success` — it silently collapses
`Result.Error` to `null` and the user sees nothing at all. There are 37 such sites across 10
ViewModel files today; don't add a 38th.

## StateFlow Usage in UI

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    // Collect state with lifecycle awareness
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    // Use state to render UI
    LazyColumn {
        items(state.items) { item ->
            ItemRow(item)
        }
    }
    
    // Show offline banner if needed
    if (!isOnline) {
        OfflineBanner()
    }
    
    // Show error if present
    if (error != null) {
        ErrorDialog(error) { viewModel.clearError() }
    }
}
```

**Reality check**: the shipped screens don't do this yet — there are 87 `collectAsState()` call sites
across 44 files and **zero** uses of `collectAsStateWithLifecycle`. The lifecycle-aware variant above
is the target; migrating the existing call sites is an open Track D item.

## Hilt Scoping

```kotlin
@Singleton          // App lifetime (singletons)
@ActivityScoped     // Activity lifetime (one per activity)
@FragmentScoped     // Fragment lifetime (one per fragment)
// Unscoped         // New instance each time
```

Most repositories use `@Singleton` since they're app-level singletons.

## Common Errors & Solutions

| Error | Cause | Fix |
|-------|-------|-----|
| "Cannot find symbol IMyRepository" | Interface not added to repository file | Add to `core/domain/repository/Repositories.kt` |
| "Unresolved reference: MyRepository" | Repository not bound in Hilt | Add `@Provides` in `app/di/Modules.kt` |
| "Module already bound" | Duplicate Hilt bindings | Check for duplicate providers |
| "ViewModel creation failed" | Missing @Inject or @HiltViewModel | Add `@HiltViewModel` to ViewModel class |
| "Room error: no data" | Entity not added to database | Add to entities list in `@Database` annotation |
| "Flow never emits" | Typo in StateFlow name | Double-check property names match usage |

## Documentation Files

*(No line counts here on purpose — they went stale faster than the content did.)*

| File                        | Purpose                                        | Read it when…                          |
|-----------------------------|------------------------------------------------|----------------------------------------|
| `CLAUDE.md`                 | Conventions an agent must follow                | before writing any code                |
| `README.md`                 | Project overview + known issues                 | onboarding                             |
| `TECH_LEAD_REVIEW.md`       | **Defect facts, cited to file:line**            | you need to know if something is broken |
| `ROADMAP.md`                | 21-phase plan + Track A–E remediation roadmap   | deciding what to work on next          |
| `ARCHITECTURE.md`           | Detailed architecture                           | adding a layer/module                  |
| `DEVELOPMENT.md`            | Dev setup & workflow                            | setting up, or writing a new screen    |
| `AUDIT_REPORT.md`           | Audit findings                                  | reviewing quality/security posture     |
| `IMPLEMENTATION_SUMMARY.md` | What's done, what's left                        | a status snapshot                      |
| `FILE_STRUCTURE.md`         | Directory structure                             | finding where code lives               |
| `QUICK_REFERENCE.md`        | This file                                       | you need an answer in 10 seconds       |

**Precedence when docs disagree** (one hierarchy, stated once — other docs defer here):
`TECH_LEAD_REVIEW.md` wins on **defect facts and file:line citations**; `ROADMAP.md` wins on
**phase and track status**. Anything contradicting current source loses to the source.

## Resources

- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Room Database**: https://developer.android.com/training/data-storage/room
- **Hilt DI**: https://developer.android.com/training/dependency-injection/hilt-android
- **WorkManager**: https://developer.android.com/topic/libraries/architecture/workmanager
- **Kotlin Flow**: https://kotlinlang.org/docs/flow.html
- **Material 3**: https://m3.material.io/

## For Questions

1. **Architecture questions** → Read `ARCHITECTURE.md`
2. **Setup questions** → Read `DEVELOPMENT.md`
3. **File locations** → Read `FILE_STRUCTURE.md`
4. **What's implemented** → Read `IMPLEMENTATION_SUMMARY.md`
5. **Patterns & examples** → Read this file
6. **Next steps** → Read `ROADMAP.md` (Tracks A–E supersede the old "Next Steps")
7. **"Is this actually done?"** → Read `TECH_LEAD_REVIEW.md`
