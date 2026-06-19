# Psy Auth Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.
> NO Android unit tests (user preference). Backend keeps tests. Verify by build/lint + manual. Spec: `docs/superpowers/specs/2026-06-19-psy-auth-redesign-design.md`.

**Goal:** Google-only login gating the whole app at launch (login once → offline after), automatic backup-on-background + restore-on-empty-login, Settings reduced to Logout (backup → wipe → login). Remove dev-login and the manual backup screen.

**Environments:** Android gradle from `/Users/hoalam/Codes/psy/android` with `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`. Go from `/Users/hoalam/Codes/psy/backend`.

---

## Task 1: Backend — remove dev-login

**Files (backend):**
- `internal/config/config.go`: remove `DevLoginEnabled` field + its env parsing.
- `internal/api/auth_handlers.go`: remove `handleDevLogin` (+ its request struct). Keep `handleGoogleLogin`.
- `internal/api/router.go`: remove the `r.Post("/auth/dev", ...)` route. Keep `/auth/google`, `/health`, the auth group + `/backup*`.
- `internal/api/backup_handlers_test.go`: it previously got a token via `/auth/dev`. Replace that with: seed a user directly (`user.UpsertBySub(ctx, pool, "test-sub", "test@example.com")`) then `auth.IssueJWT(userID, cfg.JWTSecret)` to build the Bearer token. Keep the round-trip assertions (POST /backup → GET /backup → version bump; no-token → 401). Still DB-gated by `TEST_DATABASE_URL`.
- Anything else referencing DevLoginEnabled (main.go shouldn't).

- [ ] Step 1: Apply removals + fix the test.
- [ ] Step 2: `cd backend && go build ./... && go vet ./... && go test ./...` → green (jwt test runs; backup test skips without TEST_DATABASE_URL; run it against a Docker PG if convenient).
- [ ] Step 3: Commit `refactor(backend): remove dev-login; backup test issues JWT directly`.

---

## Task 2: Android data layer — remove dev-login, add login data lifecycle

**Files (android, com.psy):**
- `data/remote/AuthApi.kt`: remove `devLogin`. Keep `googleLogin`.
- `data/remote/dto/AuthDtos.kt`: remove `DevLoginRequest`. Keep `GoogleLoginRequest`, `TokenResponse`.
- `domain/repository/AuthRepository.kt` + `data/repo/AuthRepositoryImpl.kt`: remove `signInDev`. Keep `authState`, `signInGoogle`, `signOut` (signOut just clears token+email).
- `data/db` DAOs: ensure each of the 5 has `getAll()`/`deleteAll()`/`insertAll()` (added in the backup feature) — also confirm a cheap emptiness check is possible. Add to `LedgerDao`: `@Query("SELECT COUNT(*) FROM ledgers") suspend fun count(): Int` if not present.
- `data/backup/SnapshotManager.kt`: add `suspend fun isLocalEmpty(): Boolean` (e.g. `ledgerDao.count() == 0`) and `suspend fun wipeLocal()` (`db.withTransaction { deleteAll on all 5 tables in child→parent order }`). (Keep export/import.)
- `domain/repository/BackupRepository.kt` + impl: add `suspend fun prepareLocalDataAfterLogin()`: 
  ```
  if (snapshotManager.isLocalEmpty()) {
      val resp = backupApi.download()
      if (resp.code() != 204 && resp.body() != null) snapshotManager.import(resp.body()!!.blob)
      else seeder.seedIfEmpty(System.currentTimeMillis())   // inject DefaultDataSeeder
  }
  ```
  Keep `backupNow()`, `restore()`. Inject `DefaultDataSeeder` into the impl.
- `data/auth/AuthTokenStore.kt`: keep token/email/lastSyncAt; the `autoBackup` flag is no longer needed (auto-backup is always-on) — you may remove `autoBackupFlow`/`setAutoBackup` (and their keys) OR leave them unused. Prefer removing to reduce dead code; if removal causes churn in callers, leaving them unused is acceptable — note which.
- `ui/auth/GoogleSignIn.kt` (new): move `launchGoogleSignIn(activity, onSuccess, onError)` here (from BackupScreen, which is being deleted) — `GetSignInWithGoogleOption` flow + the placeholder-clientId guard + Log.e on failure. Keep exact behavior.

- [ ] Step 1: Implement removals + SnapshotManager.wipeLocal/isLocalEmpty + BackupRepository.prepareLocalDataAfterLogin + move GoogleSignIn helper.
- [ ] Step 2: (Will not fully compile until Task 3 deletes BackupScreen references — so compile at the END of Task 3. For Task 2, you MAY temporarily keep BackupScreen compiling by leaving its GoogleSignIn usage importing the moved helper. If a clean compile isn't possible mid-refactor, note it and ensure Task 3 finishes the compile.) Aim: `./gradlew :app:assembleDebug` green; if blocked only by BackupScreen (deleted in Task 3), proceed and let Task 3 green it.
- [ ] Step 3: Commit `refactor(data): remove dev-login; add wipeLocal + restore-or-seed login lifecycle`.

---

## Task 3: Android — LoginScreen, AppRoot gate, AppViewModel, Settings; delete BackupScreen

**Files:**
- `ui/auth/LoginScreen.kt` (new): `@Composable fun LoginScreen(onSignedIn: () -> Unit)` (or it just calls a VM and the gate reacts). Welcome UI (Psy + emoji + tagline) + a single "Đăng nhập với Google" Button → `launchGoogleSignIn(activity, onSuccess = { idToken -> viewModel.signInGoogle(idToken) }, onError = { msg -> show it })`. A transient error text (offline/Google). No dev/email.
- `ui/app/AppViewModel.kt`:
  - `val isSignedIn: StateFlow<Boolean>` = `authTokenStore.tokenFlow.map { it != null }.stateIn(...)` (or via AuthRepository.authState.signedIn).
  - `fun signInGoogle(idToken)`: `viewModelScope.launch { authRepository.signInGoogle(idToken).onSuccess { backupRepository.prepareLocalDataAfterLogin() } ; emit message on failure }`.
  - `onStop(now)`: keep lock-time recording; if signed in → app-scope best-effort `backupRepository.backupNow()` (throttled; drop the autoBackup-toggle condition).
  - `fun logout()`: `appScope.launch { runCatching { backupRepository.backupNow() }; snapshotManager.wipeLocal(); authRepository.signOut() }`. (Inject SnapshotManager or expose wipe via BackupRepository — prefer a `BackupRepository.logoutWipe()` or call snapshotManager directly; keep it clean.)
  - Inject AuthRepository, BackupRepository, SnapshotManager (or a BackupRepository facade), AuthTokenStore.
- `ui/app/AppRoot.kt`: gate order — `val signedIn by vm.isSignedIn.collectAsStateWithLifecycle()`. Inside `PsyTheme(...)`: `when { !signedIn -> LoginScreen(...) ; locked -> LockScreen(...) ; else -> PsyNavHost() }`. (Login gate OUTSIDE the lock gate.)
- `ui/settings/SettingsScreen.kt`: remove the "Sao lưu & đồng bộ" row + `onBackup`. Add an account section: show the signed-in email (from a VM exposing AuthRepository.authState/email) + an "Đăng xuất" row → an `AlertDialog` ("Đăng xuất sẽ sao lưu rồi xoá dữ liệu trên máy này. Tiếp tục?") → on confirm call logout (route it up to AppViewModel via a callback, or a SettingsViewModel that depends on the same repos and triggers the logout-wipe sequence). Simplest: `SettingsScreen(onLogout: () -> Unit)` and wire `onLogout` in PsyNavHost to `appViewModel.logout()` (obtain the AppViewModel via hiltViewModel at the nav-graph activity scope, or pass a lambda from AppRoot down through PsyNavHost). Pick the cleanest wiring that compiles; document it.
- `ui/navigation/Routes.kt`: remove `BACKUP`. `ui/navigation/PsyNavHost.kt`: remove the backup `composable` + `onBackup`; pass `onLogout` to SettingsScreen.
- **DELETE** `ui/backup/BackupScreen.kt` and `ui/backup/BackupViewModel.kt` (`git rm`).

- [ ] Step 1: Implement LoginScreen, AppRoot gate, AppViewModel changes, Settings logout, nav changes; delete BackupScreen/VM.
- [ ] Step 2: `./gradlew :app:assembleDebug :app:lintDebug` → BUILD SUCCESSFUL, 0 lint errors. `:app:testDebugUnitTest` green.
- [ ] Step 3: Commit `feat(ui): Google-only login gate, auto-sync lifecycle, logout in settings; remove backup screen`.

---

## Task 4: Verification gate

- [ ] Step 1: Backend `cd backend && go build ./... && go vet ./... && go test ./...` → green.
- [ ] Step 2: Android `cd android && ./gradlew :app:assembleDebug :app:lintDebug` → SUCCESSFUL, 0 lint errors.
- [ ] Step 3: Manual (assistant drives taps; user has done the 3 Google prerequisites + backend running with GOOGLE_CLIENT_ID + Docker PG):
  1. Launch app → **LoginScreen** appears (NOT Home) — confirms the gate.
  2. Tap "Đăng nhập với Google" → account chooser → pick account → (consent) → app opens to Home. Verify a `users` row with a real `google_sub` in Postgres.
  3. Record a transaction → background the app → reopen: still in (offline, token persisted). Verify a `snapshots` row.
  4. Settings → no "Sao lưu" row; "Đăng xuất" present. Tap → confirm dialog → logout → LoginScreen.
  5. Log back in (same account) → local was wiped → auto-restore → the transaction is back.
  - If a 2FA/verify wall blocks automation, hand that single step to the user.
- [ ] Step 4: `git --no-pager log --oneline && git status -s` clean.

---

## Self-Review Notes
- Spec coverage: backend dev-login removal + test fix (Task 1); android dev-login removal + wipe/restore-or-seed lifecycle + moved GoogleSignIn (Task 2); LoginScreen + AppRoot gate + AppViewModel auto-sync/logout + Settings + BackupScreen deletion (Task 3); verification incl. manual (Task 4). All covered.
- Build reality: Task 2 may not compile standalone (BackupScreen still references removed bits) — Task 3 completes the compile; Task 2's commit may be a WIP that greens at Task 3 (acceptable for a refactor; note it). Alternatively do Tasks 2+3 in one implementer pass if cleaner.
- Type consistency: `AuthRepository` loses `signInDev`; `AuthApi` loses `devLogin`; `BackupRepository` gains `prepareLocalDataAfterLogin`; `SnapshotManager` gains `isLocalEmpty`/`wipeLocal`; `AppViewModel` gains `isSignedIn`/`signInGoogle`/`logout`; `Routes.BACKUP` removed everywhere; `SettingsScreen` gains `onLogout`, loses `onBackup`.
- Backend: `/auth/dev` + `DevLoginEnabled` gone; `backup_handlers_test` issues JWT via `auth.IssueJWT`.
- Logout offline → wipe still proceeds (data presumed synced); dialog wording notes cloud backup.
