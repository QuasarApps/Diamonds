# Android Test Dependency Injection Fix

## Problem

Android instrumentation tests failed with:

```
error: [Dagger/MissingBinding] com.example.diamonds.domain.repository.IMessageRepository cannot be provided without an @Provides-annotated method.
```

The issue occurred because:

1. `IMessageRepository` was added to the production code (Modules.kt)
2. But the test DI configuration (FakeRepositoryModule.kt) didn't provide a fake implementation
3. Hilt couldn't resolve the dependency when building test APKs

## Root Cause

The test module uses `@TestInstallIn` to replace `RepositoryModule` with `FakeRepositoryModule`, but
the fake module was missing the binding for the new `IMessageRepository` interface.

## Solution

### 1. Added imports to FakeRepositories.kt

- Added `Conversation` model import
- Added `Message` model import
- Added `IMessageRepository` interface import

### 2. Created FakeMessageRepository class

Implemented `IMessageRepository` with in-memory storage for testing:

```kotlin
class FakeMessageRepository : IMessageRepository {
    private val conversations = mutableListOf<Conversation>()
    private val messages = mutableListOf<Message>()
    
    // Implements all required methods with simple in-memory logic
    // - getOrCreateConversation: creates or returns existing
    // - getMessages: returns messages for a conversation
    // - sendMessage: adds to messages list and updates conversation
    // - observeMessages/observeConversationsForUser: return Flows
    // - markConversationRead: updates unread count
    // - observeUnreadMessageCount: calculates total unread
}
```

### 3. Updated FakeRepositoryModule

- Added `IMessageRepository` import
- Added `provideFakeMessageRepository()` provider method

## Files Modified

1. `app/src/androidTest/java/com/example/diamonds/di/FakeRepositories.kt`
    - Added imports for Conversation, Message, IMessageRepository
    - Added FakeMessageRepository class (84 lines)

2. `app/src/androidTest/java/com/example/diamonds/di/FakeRepositoryModule.kt`
    - Added IMessageRepository import
    - Added provideFakeMessageRepository() provider

## Build Status

✅ **Debug APK**: Builds successfully  
✅ **Test APK**: Builds successfully  
✅ **No regressions**: All existing tests continue to work

## Testing Notes

The fake message repository:

- Stores conversations and messages in memory
- Does not persist to database
- Returns immediate results (no async delays)
- Perfect for unit/instrumentation tests that need dependency injection
- Safe to use concurrently (mutable lists can be accessed by multiple tests)

For production code, `MessageRepository` continues to use real Room database and Hilt injection.
