# Psy — Design Spec (v1)

**Date:** 2026-06-17
**Status:** Approved (brainstorming)
**Author:** hoalam

## 1. Overview

Psy is a cute, offline-first personal money tracker for Android, inspired by the
"Daak – Budget & Money Tracker" app. v1 targets a learning-grade product that is
good enough to share with family members. A lightweight Go backend provides basic
Google Sign-In and cloud snapshot backup so users do not lose data when changing
devices.

**Goals**
- Learn modern Android (Kotlin/Compose) + Go backend with clean, pragmatic architecture.
- Ship a usable v1 to relatives: record money, see stats, set budgets, back up to cloud.
- A distinctive, friendly "cute" visual identity (Candy Pop direction).

**Non-goals (v1)** — deferred to later phases:
- Multi-person shared ledgers (realtime)
- Voice input, screenshot/OCR input
- Home-screen widgets, shortcuts/automation, Wear OS
- Multi-currency conversion (single main currency only in v1)
- Installments, piggy bank, borrow/lend, refund/reimbursement
- Monetization: ads, Google Play Billing (month/year/lifetime), free-quota gating

> The free/paid split is **not** a v1 feature. v1 builds the core ("free") feature set;
> the free/paid **gating** lands in the later monetization phase. The full Daak clone is
> built progressively, one phase = one spec → plan → build cycle.

## 2. Decisions (locked during brainstorming)

| Topic | Decision |
|---|---|
| App name | **Psy** |
| First platform | Android (Android Studio) |
| Language | **Kotlin** |
| Architecture | **Approach 1** — pragmatic MVVM, single-module, packages `data/domain/ui` |
| Data strategy | **Offline-first**, Room is source of truth |
| Backend | Go (chi) + Postgres, basic |
| Auth | **Google Sign-In** (Credential Manager) → backend verifies Google ID token → issues JWT |
| Sync | **Whole-DB snapshot backup/restore**, last-write-wins (no per-entity delta in v1) |
| HTTP client | **Retrofit** + kotlinx.serialization |
| Visual direction | **Candy Pop** — vibrant pastel gradients, sticker-style, rounded, playful |
| Repo layout | Monorepo at `~/Codes/psy` (`android/`, `backend/`, `docs/`) |

## 3. Tech Stack

**Android**
- Kotlin, Jetpack Compose, Material 3
- MVVM (ViewModel + StateFlow), Coroutines/Flow
- Room (local DB, source of truth)
- Hilt (DI)
- Navigation Compose
- DataStore (settings/preferences)
- Coil (bill photos), Lottie (cute micro-animations)
- Google Sign-In via Credential Manager
- Retrofit + kotlinx.serialization (backend API)
- BiometricPrompt (app lock)

**Backend (Go)**
- chi router
- Postgres (users, snapshot blobs + metadata)
- Google ID token verification → JWT session tokens
- Snapshot blob storage (backup/restore)

**Design system — "Candy Pop"**
- Palette: vibrant pastel gradients (violet→sky, pink accents), white cards, soft shadows
- Shape: large corner radius (16–24dp), pill buttons, circular FAB
- Typography: rounded friendly font (Quicksand / Baloo 2)
- Iconography: hand-drawn / sticker-style category icons (emoji as placeholders early on)
- Motion: Lottie micro-animations (save success, empty states)

## 4. Architecture & Repo Structure

```
~/Codes/psy/
├── android/                       # open this folder in Android Studio
│   └── app/src/main/java/com/psy/
│       ├── data/
│       │   ├── db/                # Room: entities, DAOs, PsyDatabase
│       │   ├── remote/            # Retrofit api + DTOs
│       │   ├── repo/              # Repository implementations
│       │   └── backup/            # SnapshotManager (serialize all tables -> JSON)
│       ├── domain/
│       │   ├── model/             # domain models
│       │   ├── repository/        # repository interfaces
│       │   └── usecase/           # use cases
│       ├── ui/
│       │   ├── theme/             # Candy Pop design system
│       │   ├── navigation/        # Navigation Compose graph
│       │   ├── screens/           # home, addBill, stats, calendar, budget, settings, lock
│       │   └── components/        # shared composables
│       └── di/                    # Hilt modules
├── backend/
│   ├── cmd/server/main.go
│   ├── internal/
│   │   ├── auth/                  # google verify, jwt
│   │   ├── backup/                # handler + store
│   │   ├── db/                    # Postgres, migrations runner
│   │   └── api/                   # router, middleware
│   └── migrations/
└── docs/                          # spec, ADR
```

**Data flow:** Compose UI → ViewModel (StateFlow) → Repository → Room DAO (source of truth).
**Backup flow:** Repository → SnapshotManager (serialize all tables → JSON) → Retrofit ApiClient → Go backend.

## 5. Data Model (Room entities)

- **Ledger**(id, name, icon, currency, createdAt) — multiple books
- **Account**(id, name, type[cash/bank/credit/asset], balance, icon, color) — global wallets/accounts
- **Category**(id, name, icon, color, type[income/expense], parentId?, sortOrder) — supports sub-categories
- **Transaction**(id, ledgerId, type[income/expense/transfer], amount, categoryId, accountId, toAccountId?, note, photoUri?, date, createdAt, updatedAt)
- **Budget**(id, ledgerId, categoryId?[null = total], period[monthly/yearly], amount, startDay)
- **Settings** (DataStore, not Room): theme, pinHash, biometricEnabled, mainCurrency, startDayOfMonth

Notes:
- v1 uses a single `mainCurrency`; the `currency` field on Ledger is stored but conversion is deferred.
- `amount` stored as a minor-unit integer (e.g., cents/đồng) to avoid floating-point errors.

## 6. Features (v1)

1. **Core bookkeeping** — add/edit/delete bill (income/expense/transfer) with category, account,
   date, note, optional photo; multiple ledgers; custom categories and accounts.
2. **Home** — month balance card (income/expense summary), bill list grouped by day, FAB to add.
3. **Statistics & Calendar** — pie chart by category, top-10 items, monthly trend, calendar month
   view with per-day totals.
4. **Budget** — monthly budgets per total or per category; progress (spent vs limit).
5. **Cloud backup/sync** — Google Sign-In; manual + automatic snapshot upload (on app background /
   daily); restore on a new device. Whole-DB last-write-wins; confirm before overwriting local data.
6. **Cute theming + app lock** — Candy Pop theme (light/dark); app lock via PIN + BiometricPrompt.

## 7. Backend API (v1)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/google` | Verify Google ID token, upsert user, return JWT |
| GET | `/backup` | Return latest snapshot blob + metadata for the user |
| GET | `/backup/meta` | Return snapshot metadata only (version, updatedAt, size) |
| POST | `/backup` | Upload a new snapshot blob (replaces latest), bump version |

- Auth middleware validates JWT on `/backup*` routes.
- Snapshot is an opaque JSON blob from the client; server stores it per-user with a monotonically
  increasing `version` and server `updatedAt`. v1 keeps only the latest snapshot per user.

## 8. Sync Strategy (v1)

- **Snapshot** = full export of all Room tables to JSON.
- **Backup**: client serializes DB → uploads blob; server stores as latest, bumps version.
- **Restore**: client downloads blob; before applying, asks user "Replace local data?"; on confirm,
  replaces the local DB transactionally.
- **Granularity**: whole-DB last-write-wins. No per-entity merge in v1. (Per-entity delta sync is a
  later phase.)

## 9. Error Handling

- Offline-first: all operations work locally without network.
- Backup failure: retry with backoff, show non-blocking toast; never block local work.
- Token expiry: silent re-auth via stored Google credential; if it fails, prompt sign-in.
- Restore conflict: explicit user confirmation before overwriting local data.
- DB writes: transactional; restore applies in a single transaction to avoid partial state.

## 10. Testing (TDD)

- Repository / use-case unit tests (JUnit + Turbine for Flow).
- Room DAO tests (Robolectric or instrumented).
- ViewModel tests with fake repositories.
- Backend handler tests (Go `testing` + `httptest`, table-driven).
- Follow `superpowers:test-driven-development` — tests before implementation.

## 11. Suggested Claude Code Skills / Plugins

- **`frontend-design`** (Anthropic official, in `claude-plugins-official`) — visual identity, palette,
  typography for the Candy Pop direction. Enable via `/plugin`. Not installed yet.
- superpowers process skills already available: `test-driven-development`, `writing-plans`,
  `systematic-debugging`, `requesting-code-review`, `verification-before-completion`.

## 12. Roadmap (phases beyond v1)

| Phase | Content | Backend |
|---|---|---|
| v1 (this spec) | Core bookkeeping + stats/calendar + budget + backup + cute theme/lock | basic |
| 3 | Financial tools: piggy bank, installment, borrow/lend, refund, pre-order | mostly local |
| 4 | Advanced input: voice, screenshot/OCR, widgets, shortcuts, Wear OS | local |
| 5 | Multi-person shared ledgers (realtime) | yes |
| 6 | Monetization: Google Play Billing (month/year/lifetime), AdMob, free-quota gating | yes |
| 7 | Personalization+: multi-currency conversion, more themes/app icons | partial |
