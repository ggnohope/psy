# Psy — Auth Redesign Design Spec

**Date:** 2026-06-19
**Status:** Approved (brainstorming)
**Author:** hoalam

## Overview

Rework Psy's auth/sync so that: login is **Google-only** (dev-login removed), the app is **gated behind
login at launch**, backup/sync is **fully automatic** (no manual UI), and **Settings only offers Logout
(to switch account)**. Login is required once; the token persists so subsequent launches open straight
into the app and work **offline**. On login with an empty local DB, the latest cloud backup is
auto-restored; on app background, an auto-backup runs; on logout, a final backup runs then local data is
wiped and the user returns to the login screen.

**No Android unit tests** (user preference). Verify by build + lint + manual. Backend keeps lightweight
handler tests. **Google flow testing:** the user sets up the 3 Google prerequisites (Android OAuth client
with SHA-1, test user, a healthy signed-in account on the emulator); the assistant then attempts to drive
the chooser/consent taps (2FA/verify steps, if shown, are the user's).

## Decisions (locked)
- Google-only login; **remove dev-login** (Android UI + AuthRepository.signInDev + AuthApi.devLogin + backend `/auth/dev` + `DevLoginEnabled`).
- **Login gate at launch**; "signed in" = a stored JWT exists (presence check only — no network re-verify → offline OK after first login).
- **Auto-restore when local empty** on login (else seed defaults).
- **Auto-backup on app background** (always when signed in; no toggle).
- **Settings: Logout only** (shows signed-in email + "Đăng xuất"). Remove the "Sao lưu & đồng bộ" screen/row.
- **Logout = final backup (best-effort) → wipe local DB → return to LoginScreen** (with a confirm dialog). Clean account switching.

## Gating (AppRoot)

`AppRoot` evaluates, in order:
1. **Not signed in** → `LoginScreen` (Google-only).
2. Else **locked** (existing PIN/biometric lock) → `LockScreen`.
3. Else → `PsyNavHost` (the app).

`AppViewModel` exposes `isSignedIn: StateFlow<Boolean>` (derived from `AuthTokenStore.tokenFlow != null`).
Login/logout flip it reactively so `AppRoot` recomposes.

## LoginScreen (`ui/auth/LoginScreen.kt`)

A welcome screen: app branding (Psy + a cute illustration/emoji), a short tagline, and a single
**"Đăng nhập với Google"** button. Tapping runs the Credential Manager `GetSignInWithGoogleOption` flow
(the helper, moved to `ui/auth/GoogleSignIn.kt`, shared) → on success `authRepository.signInGoogle(idToken)`
→ then `prepareLocalDataAfterLogin()` (restore-or-seed) → gate opens. Shows a transient error on
offline/Google failure (e.g. "Không có mạng — cần internet để đăng nhập lần đầu"). No dev-login, no email field.

## Auto data lifecycle

- **On login success** → `prepareLocalDataAfterLogin()` (in a repo/use-case): if the local DB is empty
  (e.g. `transactionDao` + `ledgerDao` both empty, or a simpler "ledgers empty" check), then: if the server
  has a snapshot → `BackupRepository.restore()`; else → `DefaultDataSeeder.seedIfEmpty(now)` (so a brand-new
  account gets default ledger/account/categories). If local is NOT empty, do nothing (keep current data).
- **On app background (`AppViewModel.onStop`)** → if signed in → `BackupRepository.backupNow()` best-effort,
  throttled (≥ a few minutes since last sync), on an app scope; never blocks. (Drop the `autoBackup` toggle —
  always on when signed in.)
- **On logout** → `AppViewModel.logout()`: (1) best-effort `backupNow()`; (2) wipe local DB
  (`SnapshotManager.wipeLocal()` = clear all 5 tables in a transaction); (3) `authRepository.signOut()`
  (clear token+email). `isSignedIn` flips false → `AppRoot` shows `LoginScreen`. Next login → empty local →
  restore-or-seed for the new account.

## Settings

- Remove the "Sao lưu & đồng bộ" row and `Routes.BACKUP`/BackupScreen.
- Add an account row/section: show the signed-in email + **"Đăng xuất"** → an `AlertDialog`
  ("Đăng xuất sẽ sao lưu rồi xoá dữ liệu trên máy này. Tiếp tục?") → `AppViewModel.logout()`.

## Removals
- Android: `ui/backup/BackupScreen.kt`, `ui/backup/BackupViewModel.kt`; `AuthApi.devLogin` + `DevLoginRequest`;
  `AuthRepository.signInDev`; the backup row + `Routes.BACKUP`; the `autoBackup` preference usage.
  (Keep `BackupRepository.backupNow/restore` + `SnapshotManager` — now used internally by the auto lifecycle.)
- Backend: `/auth/dev` route + `handleDevLogin`; `DevLoginEnabled` config. Update `backup_handlers_test.go`
  to obtain a token via `auth.IssueJWT(userID, secret)` for a seeded user instead of calling `/auth/dev`.
  Keep `/auth/google`, `/backup*`, `jwt_test.go`.

## Backend (unchanged behavior, minus dev)
`/auth/google` (verify Google ID token → upsert user → JWT), `GET/POST /backup`, `GET /backup/meta`,
auth middleware. Config keeps `JWTSecret`, `GoogleClientID`; drops `DevLoginEnabled`.

## Error / Edge handling
- **First launch offline / backend down** → LoginScreen shows an error; the app can't be entered until a
  successful first login (by design). Subsequent launches (token present) open offline.
- Login success but restore fails (network mid-flow) → fall back to seed defaults; user can still use the
  app; next background auto-backup syncs.
- Auto-backup/restore failures are best-effort and never crash or block; they surface a brief message at most.
- Logout when offline → final backup may fail (best-effort); local is still wiped (data is presumed already
  synced by prior auto-backups) — the confirm dialog wording notes "đã sao lưu lên cloud".
- JWT expired/invalid (401 on a backup call) → clear token → app returns to LoginScreen on next gate eval.
- Wipe + restore/seed run in Room transactions (all-or-nothing).

## Deployment note (surfaced, user's action)
Because login is mandatory and Google verification + backup need the backend, the **Go backend must be
reachable** for first login / sync. It is currently a local dev server (`10.0.2.2:8080` for the emulator).
To share with relatives on real devices, the backend must be **hosted** (and `BASE_URL` pointed at it,
ideally over HTTPS). This is outside this spec's code scope.

## Testing
- Backend: `go build/vet/test` (jwt test + updated backup handler test that issues a JWT directly).
- Android: build + lint (0 errors). No unit tests.
- Manual (assistant drives taps after user does the 3 Google prerequisites): launch → LoginScreen →
  "Đăng nhập với Google" → account chooser → (consent) → app opens; record a transaction → background
  (auto-backup; verify `snapshots` row) → Settings → Đăng xuất → LoginScreen; log back in → data restored.

## File Structure (new/changed)
```
ui/auth/{LoginScreen.kt, GoogleSignIn.kt} (new; GoogleSignIn moved out of BackupScreen)
ui/app/{AppRoot.kt (login gate), AppViewModel.kt (isSignedIn, login restore/seed, onStop always-backup, logout)}
ui/settings/SettingsScreen.kt (remove backup row; add account/logout) ; SettingsViewModel or reuse for email+logout
ui/navigation/{Routes.kt (remove BACKUP), PsyNavHost.kt (remove backup composable)}
data/backup/{BackupRepository(+restoreIfLocalEmpty/prepareLocalDataAfterLogin), SnapshotManager(+wipeLocal, +isLocalEmpty)}
data/remote/{AuthApi (remove devLogin), dto/AuthDtos (remove DevLoginRequest)}
data/repo/AuthRepositoryImpl (remove signInDev) ; domain/repository/AuthRepository (remove signInDev)
REMOVE: ui/backup/BackupScreen.kt, ui/backup/BackupViewModel.kt
backend: internal/api/{router.go (drop /auth/dev), auth_handlers.go (drop handleDevLogin)}, internal/config/config.go (drop DevLoginEnabled), internal/api/backup_handlers_test.go (issue JWT directly)
```
