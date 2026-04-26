# Diamonds - Home Cleaning Marketplace

**Status**: Phases 1–15 ✅ Complete (75% of 20-phase roadmap)  
**Production LOC**: 17,000+ | **Screens**: 41+ | **ViewModels**: 16 | **Demo Accounts**: 5  
**Last Audit**: April 26, 2026 —
see [Known Issues in README.md](README.md#known-issues--audit-findings)

---

## ⚠️ Top Blockers Before Firebase Go-Live

1. Add **Coil** image loading library (`io.coil-kt:coil-compose`) — images non-functional without it
2. Change **`applicationId`** from `com.example.diamonds` to a real package
3. Enable **`isMinifyEnabled = true`** and write `proguard-rules.pro`
4. **Encrypt the DataStore** session token
5. Write **Firestore Security Rules** and add to repo
6. Implement the **14 unimplemented `FirebaseBackendService` methods**
7. Fix **`getCurrentClient()`** — injects real user ID, currently returns `Result.Error`

---

## Quick Start

```bash
./gradlew build        # Build project
./gradlew test         # Run tests  
./gradlew installDebug # Install on device
```

---

## What's Implemented

| Phase | Feature                                                   | Status |
|-------|-----------------------------------------------------------|--------|
| 1     | Architecture (5 modules, DI, DB, Repos)                   | ✅      |
| 2     | Auth & Profiles (Login, Signup, Session)                  | ✅      |
| 3     | Booking (Search, Details, History)                        | ✅      |
| 4     | Provider Dashboards (Cleaner, Company)                    | ✅      |
| 5     | Reviews & Ratings                                         | ✅      |
| 6     | Payments (Card, History)                                  | ✅      |
| 7     | Navigation & App Flow (Deep Links, Transitions, Dialogs)  | ✅      |
| 8     | Firebase Integration (Auth, Firestore, FCM)               | ✅      |
| 9     | Maps & Location (Google Maps, Tracking)                   | ✅      |
| 10    | Sync & Offline Features (SyncWorker, Conflict Resolution) | ✅      |
| 11    | In-App Chat System                                        | ✅      |
| 12    | Subscription & Recurring Bookings                         | ✅      |
| 13    | Multi-Language Support                                    | ✅ ⚠️   |
| 14    | Reverse Reviews (Cleaner Reviews Customer)                | ✅ ⚠️   |
| 15    | Help, Support & Claims System                             | ✅ ⚠️   |
| 16    | Detailed Cleaning & Location Options                      | ⏳      |
| 17    | Error Handling & Analytics                                | ⏳      |
| 18    | Testing & Optimization                                    | ⏳      |
| 19    | Release Preparation & CI/CD                               | ⏳      |
| 20–21 | Beta Testing & Launch                                     | ⏳      |

> ⚠️ = Phase is structurally complete but has known implementation gaps (see README.md audit
> findings)

---

## Architecture

**5 Modules**:
- `:core` - Domain models & repository interfaces
- `:common` - Shared utilities
- `:data` - Room DB, repos, sync queue, connectivity
- `:ui` - 38+ Compose screens, 15 ViewModels
- `:app` - Entry point, Hilt DI

**Key Patterns**:
- Offline-first (cache reads, online writes, exponential backoff)
- MVVM + StateFlow
- Backend abstraction (Firebase/REST swappable)
- Type-safe Result<T> error handling
- 5 demo accounts with seeded data

---

## Documentation

| File                                 | Purpose                                |
|--------------------------------------|----------------------------------------|
| **START_HERE.md**                    | Quick onboarding (NEW DEVS START HERE) |
| **README.md**                        | Tech stack, overview & known issues    |
| **AUDIT_REPORT.md**                  | Full audit findings (April 26, 2026)   |
| **ARCHITECTURE.md**                  | Design decisions & patterns            |
| **DEVELOPMENT.md**                   | Dev setup & feature guide              |
| **ROADMAP.md**                       | 21-phase plan                          |
| **QUICK_REFERENCE.md**               | Code patterns & common tasks           |
| **FILE_STRUCTURE.md**                | Directory layout                       |
| **IMPLEMENTATION_SUMMARY.md**        | Current status & next steps            |
| **PHASE1_VERIFICATION_CHECKLIST.md** | Feature checklist                      |

**→ Start with START_HERE.md**
