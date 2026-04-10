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
📱 Screens                → ui/screens/*.kt
💉 Hilt DI                → app/di/Modules.kt
📄 AndroidManifest        → app/src/main/AndroidManifest.xml
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
   // ui/screens/MyUiState.kt
   data class MyUiState(val items: List<Item> = emptyList())
   
   // ui/screens/MyViewModel.kt
   @HiltViewModel
   class MyViewModel @Inject constructor(...) : 
       BaseViewModel<MyUiState>(connectivityObserver, MyUiState())
   ```

2. **Screen Composable**
   ```kotlin
   // ui/screens/MyScreen.kt
   @Composable
   fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
       val state by viewModel.uiState.collectAsStateWithLifecycle()
       val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
       
       if (!isOnline) OfflineBanner()
       // Your UI
   }
   ```

## Offline-First Checklist

When implementing a feature that involves data:

- [ ] **Read Operations**: Use repository, check cache first
- [ ] **Write Operations**: Check `connectivityObserver.isOnline()` before allowing
- [ ] **Disabled State**: Disable action buttons with tooltip when offline
- [ ] **Error Messages**: Show "Requires internet" for offline attempts
- [ ] **Sync Status**: Show pending operation count badge
- [ ] **Cancellation**: Allow users to cancel pending operations

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

```
Can :ui import from :data?        ✅ YES
Can :data import from :ui?        ❌ NO
Can :core import from :common?    ✅ YES
Can :common import from :core?    ❌ NO
Can :data import from :core?      ✅ YES
Can :core import from :data?      ❌ NO
```

## Build Commands

```bash
# Build everything
./gradlew.bat build

# Build specific module
./gradlew.bat :data:build

# Run tests
./gradlew.bat test

# Run specific test
./gradlew.bat :data:test --tests "BookingRepositoryTest"

# Clean rebuild
./gradlew.bat clean build

# Install on device
./gradlew.bat installDebug

# Check dependencies
./gradlew.bat :app:dependencies
```

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
            else -> {}
        }
    }
}
```

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

| File                        | Purpose                  | Length     |
|-----------------------------|--------------------------|------------|
| `README.md`                 | Project overview         | ~100 lines |
| `ARCHITECTURE.md`           | Detailed architecture    | 285 lines  |
| `DEVELOPMENT.md`            | Dev setup & workflow     | 400+ lines |
| `ROADMAP.md`                | 20-phase plan            | ~650 lines |
| `IMPLEMENTATION_SUMMARY.md` | What's done, what's left | ~200 lines |
| `FILE_STRUCTURE.md`         | Directory structure      | ~200 lines |
| `QUICK_REFERENCE.md`        | This file                | ~200 lines |

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
6. **Next steps** → Read `ROADMAP.md`
