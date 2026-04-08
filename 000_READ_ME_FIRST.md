# Diamonds - Home Cleaning Marketplace

**Status**: Phases 1-7 ✅ Complete (47% of 15-phase roadmap)  
**Production LOC**: 11,000+ | **Screens**: 30+ | **ViewModels**: 12 | **Demo Accounts**: 5

---

## Quick Start

```bash
./gradlew build        # Build project
./gradlew test         # Run tests  
./gradlew installDebug # Install on device
```

---

## What's Implemented

| Phase | Feature | Status |
|-------|---------|--------|
| 1 | Architecture (5 modules, DI, DB, Repos) | ✅ |
| 2 | Auth & Profiles (Login, Signup, Session) | ✅ |
| 3 | Booking (Search, Details, History) | ✅ |
| 4 | Provider Dashboards (Cleaner, Company) | ✅ |
| 5 | Reviews & Ratings | ✅ |
| 6 | Payments (Card, History) | ✅ |
| 7 | Navigation & App Flow (Deep Links, Transitions, Dialogs) | ✅ |
| 8-15 | Firebase, Maps, Testing, Release | ⏳ |

---

## Architecture

**5 Modules**:
- `:core` - Domain models & repository interfaces
- `:common` - Shared utilities
- `:data` - Room DB, repos, sync queue, connectivity
- `:ui` - 30+ Compose screens, 12 ViewModels  
- `:app` - Entry point, Hilt DI

**Key Patterns**:
- Offline-first (cache reads, online writes, exponential backoff)
- MVVM + StateFlow
- Backend abstraction (Firebase/REST swappable)
- Type-safe Result<T> error handling
- 5 demo accounts with seeded data

---

## Documentation

| File | Purpose |
|------|---------|
| **START_HERE.md** | Quick onboarding (NEW DEVS START HERE) |
| **README.md** | Tech stack & overview |
| **ARCHITECTURE.md** | Design decisions & patterns |
| **DEVELOPMENT.md** | Dev setup & feature guide |
| **ROADMAP.md** | 15-phase plan |
| **QUICK_REFERENCE.md** | Code patterns & common tasks |
| **FILE_STRUCTURE.md** | Directory layout |
| **IMPLEMENTATION_SUMMARY.md** | Current status & next steps |
| **PHASE1_VERIFICATION_CHECKLIST.md** | Feature checklist |

**→ Start with START_HERE.md**
