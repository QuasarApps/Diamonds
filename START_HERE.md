# START HERE 👋

**Diamonds** - Offline-first home cleaning marketplace app  
**Status**: Phases 1-10 ✅ Complete | **Modules**: 5 | **LOC**: 15,500+ | **Screens**: 38+

---

## Quick Setup

```bash
./gradlew build        # Build
./gradlew test         # Test
./gradlew installDebug # Install
```

---

## 60-Second Overview

- **5 Modules**: core (domain), common (utils), data (DB/sync), ui (screens), app (entry point)
- **Offline-First**: Reads from cache, writes require internet, auto-retry with backoff
- **MVVM + StateFlow**: Reactive state management
- **Hilt DI**: Dependency injection across modules
- **Demo Data**: 5 seeded accounts ready to test

---

## Architecture Layers

```
ViewModel (state) → Repository (offline logic) → Room DB + Backend Service
```

**Read**: Cache first, fetch if online  
**Write**: Internet required only  
**Sync**: Exponential backoff retry (1→2→4→...→60 min)

---

## What's Built (Phases 1-9)

| Phase | Feature               | Status |
|-------|-----------------------|--------|
| 1     | Core Infrastructure   | ✅      |
| 2     | Auth & Profiles       | ✅      |
| 3     | Booking Flow          | ✅      |
| 4     | Provider Dashboards   | ✅      |
| 5     | Reviews & Ratings     | ✅      |
| 6     | Payments              | ✅      |
| 7     | Navigation & App Flow | ✅      |
| 8     | Firebase Integration  | ✅      |
| 9     | Maps & Location       | ✅      |
| 10    | Sync & Offline        | ✅      |

---

## Documentation Map

| File | What | Time |
|------|------|------|
| **README.md** | Tech stack & features | 5 min |
| **ARCHITECTURE.md** | Design & data flow | 20 min |
| **DEVELOPMENT.md** | Adding features | 20 min |
| **QUICK_REFERENCE.md** | Code patterns | 10 min |
| **ROADMAP.md** | 15-phase plan | 10 min |
| **FILE_STRUCTURE.md** | File locations | 5 min |
| **IMPLEMENTATION_SUMMARY.md** | Status & next steps | 10 min |
| **PHASE1_VERIFICATION_CHECKLIST.md** | Completion checklist | 5 min |

**Start**: README.md → ARCHITECTURE.md → DEVELOPMENT.md

---

## Key Concepts

**Offline-First Pattern**:
- Reads: Always hit cache first, return immediately
- Writes: Check online, send to server, update cache
- Sync: Queue failed writes, retry on connectivity change

**Module Dependencies**:
```
:app → :ui, :data, :core
:ui → :data, :core, :common
:data → :core, :common
:core → nothing
:common → nothing
```

**5 Demo Accounts**:
- customer@demo.com (Customer)
- cleaner@demo.com (Independent Cleaner)
- employed@demo.com (Employed Cleaner)
- company@demo.com (Company Manager)
- Plus providers p1-p5 with seeded bookings, reviews, earnings

---

## Common Tasks

**Add a Screen?**
1. Create ViewModel extending BaseViewModel
2. Create Composable @Composable function
3. Wire in AppShell navigation

**Add a Repository?**
1. Add interface in core/domain/repository
2. Implement in data/repository
3. Add Hilt binding in app/di/Modules.kt

**Add a Database Entity?**
1. Create Room entity in data/local/entity
2. Create DAO in data/local/dao
3. Add abstract fun to AppDatabase
4. Update mappers (Entity↔Domain↔DTO)

---

## Next: Read One of These

- **Architects** → ARCHITECTURE.md
- **Developers** → DEVELOPMENT.md  
- **Coders** → QUICK_REFERENCE.md
- **Planners** → ROADMAP.md

---

**Questions?** Check docs | **Ready to code?** Follow patterns | **Need help?** See DOCUMENTATION_INDEX.md
