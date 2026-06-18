# Psy Backup/Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.
> NO Android unit tests (user preference). The **Go backend gets lightweight handler tests** (correctness-critical). Verify Android by build + manual; verify backend by `go test` + a manual end-to-end. Spec: `docs/superpowers/specs/2026-06-18-psy-backup-auth-design.md`.

**Goal:** Auth (dev login + Google Sign-In → JWT) + whole-DB JSON snapshot backup/restore (manual + auto), via the Go backend and a "Sao lưu & đồng bộ" settings screen.

**Environments:** Android gradle from `/Users/hoalam/Codes/psy/android` with `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`. Go from `/Users/hoalam/Codes/psy/backend`.

---

## Task 1: Backend — auth (JWT, dev, google) + snapshot store + handlers + tests

**Files (backend):**
- `internal/config/config.go`: add `JWTSecret` (env `JWT_SECRET`, default `"dev-secret-change-me"`), `GoogleClientID` (env `GOOGLE_CLIENT_ID`, default ""), `DevLoginEnabled` (env `DEV_LOGIN_ENABLED`, default `"true"` → bool).
- `go.mod`: `go get github.com/golang-jwt/jwt/v5` and `go get google.golang.org/api/idtoken`.
- `internal/auth/jwt.go`: `func IssueJWT(userID int64, secret string) (string, error)` (HS256, claims `sub`=userID as string, `exp`=now+30d); `func ParseJWT(token, secret string) (int64, error)`.
- `internal/auth/middleware.go`: `type ctxKey`; `func Middleware(secret string) func(http.Handler) http.Handler` reading `Authorization: Bearer <t>`, ParseJWT, `context.WithValue(userIDKey, id)`, else 401 JSON; `func UserID(ctx) (int64, bool)`.
- `internal/auth/google.go`: `func VerifyGoogleIDToken(ctx, idToken, clientID string) (sub, email string, err error)` using `idtoken.Validate(ctx, idToken, clientID)` then read `payload.Subject` + `payload.Claims["email"]`.
- `internal/user/user.go`: `func UpsertBySub(ctx, pool *pgxpool.Pool, sub, email string) (int64, error)` → `INSERT INTO users(google_sub,email) VALUES($1,$2) ON CONFLICT(google_sub) DO UPDATE SET email=EXCLUDED.email RETURNING id`.
- `internal/snapshotstore/store.go`: `Save(ctx, pool, userID int64, blob []byte) (version int64, updatedAt time.Time, err error)` (UPSERT into snapshots: version = COALESCE(old,0)+1; `INSERT ... ON CONFLICT(user_id) DO UPDATE SET version=snapshots.version+1, blob=EXCLUDED.blob, updated_at=now() RETURNING version, updated_at`; for a brand-new row version=1); `Get(ctx, pool, userID) (blob []byte, version int64, updatedAt time.Time, found bool, err error)`.
- `internal/api/auth_handlers.go`: `handleDevLogin` (if !DevLoginEnabled → 404; decode {email,name}; sub="dev:"+email; UpsertBySub; IssueJWT; `{token}`), `handleGoogleLogin` (if GoogleClientID=="" → 503 {error}; decode {idToken}; VerifyGoogleIDToken; UpsertBySub; IssueJWT; `{token}`). These need access to config + pool — pass them into a handlers struct or closures.
- `internal/api/backup_handlers.go`: `handleBackupUpload` (UserID from ctx; decode {blob string} (the JSON snapshot as a string); `snapshotstore.Save(userID, []byte(blob))`; `{version, updatedAt}`), `handleBackupDownload` (Get; if !found → 204; else `{version, blob:string(blob), updatedAt}`), `handleBackupMeta` (Get; 204 or `{version, updatedAt, size}`).
- `internal/api/router.go`: change `NewRouter()` → `NewRouter(cfg config.Config, pool *pgxpool.Pool) chi.Router` (keep `/health`); mount `POST /auth/dev`, `POST /auth/google`; a sub-router `r.Group` with `auth.Middleware(cfg.JWTSecret)` for `POST /backup`, `GET /backup`, `GET /backup/meta`. (Update the existing `/health` test that calls `NewRouter()` — give it a nil-safe or test config + nil pool, OR keep a `NewRouter()` no-arg that builds a default; simplest: update `health_test.go` to call `NewRouter(config.Load(), nil)` since /health doesn't touch the pool.)
- `cmd/server/main.go`: pass `cfg, pool` into `NewRouter`.

**Tests (backend, httptest):** `internal/api/auth_handlers_test.go` — dev login returns a non-empty token (use a real pool only if `TEST_DATABASE_URL` set, else skip the DB-touching part; for token issuance you can test `auth.IssueJWT`/`ParseJWT` round-trip in `internal/auth/jwt_test.go` WITHOUT a DB — DO include that one, it's pure). `internal/api/backup_handlers_test.go` — gated by `TEST_DATABASE_URL`: dev-login → POST /backup → GET /backup round-trips blob & version; and a no-token request → 401. Skip cleanly when no DB.

- [ ] Step 1: Implement config, deps, auth, user, snapshotstore, handlers, router, main wiring; add the pure `jwt_test.go` and DB-gated handler tests.
- [ ] Step 2: `cd /Users/hoalam/Codes/psy/backend && go build ./... && go vet ./... && go test ./...` → green (jwt test runs; DB tests skip without TEST_DATABASE_URL).
- [ ] Step 3: Commit `feat(backend): auth (dev+google JWT) + snapshot backup endpoints`.

---

## Task 2: Android — network client + auth repository

**Files (android, package com.psy):**
- Catalog + app build: add `androidx.credentials:credentials` + `androidx.credentials:credentials-play-services-auth` (stable, e.g. 1.3.0) and `com.google.android.libraries.identity.googleid:googleid` (e.g. 1.1.1).
- `data/remote/dto/AuthDtos.kt`: `@Serializable data class DevLoginRequest(email, name: String? = null)`, `GoogleLoginRequest(idToken)`, `TokenResponse(token)`.
- `data/remote/dto/BackupDtos.kt`: `BackupRequest(blob)`, `BackupResponse(version: Int, blob, updatedAt: String)`.
- `data/remote/AuthApi.kt`: `@POST("auth/dev") suspend fun devLogin(@Body DevLoginRequest): TokenResponse`; `@POST("auth/google") suspend fun googleLogin(@Body GoogleLoginRequest): TokenResponse`.
- `data/remote/BackupApi.kt`: `@POST("backup") suspend fun upload(@Body BackupRequest)`; `@GET("backup") suspend fun download(): retrofit2.Response<BackupResponse>` (so 204 → body null).
- `data/auth/AuthTokenStore.kt`: a small Preferences DataStore wrapper (reuse the existing settings DataStore instance via Hilt, or a new `auth` datastore) exposing `tokenFlow: Flow<String?>`, `emailFlow`, `lastSyncAtFlow: Flow<Long?>`, `autoBackupFlow: Flow<Boolean>`, and suspend setters `setAuth(token,email)`, `clearAuth()`, `setLastSyncAt(t)`, `setAutoBackup(b)`. Also a synchronous `currentToken(): String?` (runBlocking on first()) for the interceptor, OR keep an in-memory `@Volatile` cache updated from the flow.
- `di/NetworkModule.kt` (modify): build an `OkHttpClient` with an `Interceptor` that adds `Authorization: Bearer <token>` when a token exists (read from AuthTokenStore's cached value); pass that client to the `Retrofit.Builder().client(...)`. Provide `AuthApi` and `BackupApi` from the Retrofit instance.
- `domain/repository/AuthRepository.kt` (+`data/repo/AuthRepositoryImpl.kt`): `val authState: Flow<AuthState>` (`data class AuthState(signedIn: Boolean, email: String?)` derived from tokenFlow/emailFlow); `suspend fun signInDev(email: String): Result<Unit>` (authApi.devLogin → store token+email); `suspend fun signInGoogle(idToken: String): Result<Unit>`; `suspend fun signOut()`. Hilt @Binds.

- [ ] Step 1: Implement deps, DTOs, APIs, token store, interceptor wiring, AuthRepository.
- [ ] Step 2: `./gradlew :app:assembleDebug` → green; `:app:testDebugUnitTest` → green.
- [ ] Step 3: Commit `feat(data): auth APIs, token store, bearer interceptor, AuthRepository`.

---

## Task 3: Android — snapshot serialization + backup repository

**Files:**
- `data/backup/SnapshotDto.kt`: `@Serializable data class SnapshotDto(version: Int = 1, ledgers, accounts, categories, transactions, budgets)` where each is a `List<...Dto>`; define a `@Serializable` DTO per entity mirroring its fields (ids included). (Map from/to the Room entities.)
- DAOs: add `@Query("DELETE FROM <table>") suspend fun deleteAll()` and a bulk `@Insert suspend fun insertAll(list)` to each of LedgerDao, AccountDao, CategoryDao, TransactionDao, BudgetDao (or reuse upsert in a loop). Also add a `suspend fun getAll(): List<Entity>` (non-Flow snapshot read) to each.
- `data/backup/SnapshotManager.kt`: `@Inject constructor(db: PsyDatabase, all DAOs, json: Json)`. `suspend fun export(): String` — read all tables (getAll), map entities → DTOs, build SnapshotDto, `json.encodeToString`. `suspend fun import(jsonStr: String)` — decode; `db.withTransaction { deleteAll on all 5 (respect FK order: transactions/budgets first, then categories/accounts/ledgers — or disable FK; there are no enforced Room FKs currently so order is free); insertAll mapped entities }`.
- `domain/repository/BackupRepository.kt` (+impl): `val lastSyncAt: Flow<Long?>`; `suspend fun backupNow(): Result<Unit>` (export → `backupApi.upload(BackupRequest(blob))` → on success `tokenStore.setLastSyncAt(now)`); `suspend fun restore(): Result<RestoreOutcome>` (`download()`; if code 204 → `RestoreOutcome.NoBackup`; else import(body.blob) → `RestoreOutcome.Restored`). Hilt @Binds. (Pass `now` in from the caller or use System.currentTimeMillis in the impl method body — acceptable.)

- [ ] Step 1: Implement SnapshotDto + DAO additions + SnapshotManager + BackupRepository.
- [ ] Step 2: `./gradlew :app:assembleDebug` → green; `:app:testDebugUnitTest` → green.
- [ ] Step 3: Commit `feat(data): whole-DB snapshot serialization + backup repository`.

---

## Task 4: Android — Backup screen, Google Sign-In, auto-backup

**Files:**
- `res/values/strings.xml`: add `<string name="google_web_client_id">REPLACE_WITH_OAUTH_WEB_CLIENT_ID</string>` (placeholder; user fills).
- `Routes.kt`: add `BACKUP = "backup"`. `SettingsScreen.kt`: add `onBackup: () -> Unit = {}` + a row "Sao lưu & đồng bộ" (Icons.Default.CloudSync). `PsyNavHost.kt`: pass `onBackup`, add `composable(Routes.BACKUP){ BackupScreen(onBack = { nav.popBackStack() }) }`.
- `ui/backup/BackupViewModel.kt`: `@HiltViewModel`, inject AuthRepository, BackupRepository. Expose `authState`, `lastSyncAt`, `autoBackup` (from token store via repo or store), and a transient `message`/`busy` state. Functions: `signInDev(email)`, `signInGoogle(idToken)`, `signOut()`, `backupNow()`, `restore()` (returns outcome → message), `setAutoBackup(b)`.
- `ui/backup/BackupScreen.kt`: Scaffold + TopAppBar("Sao lưu & đồng bộ", back). 
  - Signed out: "Đăng nhập với Google" button → launches Credential Manager Google flow (see helper); a divider "hoặc"; a dev-login row: email OutlinedTextField + "Đăng nhập (dev)" button → signInDev.
  - Signed in: show email + "Đăng xuất"; "Sao lưu ngay" button (busy spinner); "Khôi phục" button → AlertDialog confirm "Ghi đè dữ liệu trên máy?" → restore; "Lần sao lưu cuối: <formatted or Chưa sao lưu>"; "Tự động sao lưu" Switch.
  - Show transient messages (e.g. "Đã sao lưu", "Đã khôi phục", "Chưa có bản sao lưu", error).
- Google Sign-In: a helper using `androidx.credentials.CredentialManager` + `com.google.android.libraries.identity.googleid.GetGoogleIdOption` (serverClientId = getString(R.string.google_web_client_id)) → `GetCredentialRequest` → on result extract `GoogleIdTokenCredential.idToken` → `viewModel.signInGoogle(idToken)`. If the client id is the placeholder / flow fails → show "Chưa cấu hình Google" message (catch the exception). Use the Activity context (LocalContext as Activity).
- Auto-backup: in `AppViewModel.onStop(...)` (existing), inject BackupRepository + AuthTokenStore; if signed in && autoBackup && (now - lastSyncAt) > throttle (e.g. 5 min) → launch `backupRepository.backupNow()` on an app-scope coroutine (best-effort, ignore failures). (Inject what's needed into AppViewModel.)

- [ ] Step 1: Implement strings, routes/row, BackupViewModel, BackupScreen, Google Sign-In helper, auto-backup hook.
- [ ] Step 2: `./gradlew :app:assembleDebug :app:lintDebug` → green.
- [ ] Step 3: Commit `feat(ui): backup & sync screen (dev + Google login, backup/restore, auto-backup)`.

---

## Task 5: Verification gate (Go tests + Android build/lint + manual end-to-end)

- [ ] Step 1: Backend: `cd backend && go build ./... && go vet ./... && go test ./...` → green (jwt round-trip passes; DB-gated tests skip or, if you start a Docker Postgres + set TEST_DATABASE_URL, run).
- [ ] Step 2: Android: `cd android && ./gradlew :app:assembleDebug :app:lintDebug` → 0 lint errors.
- [ ] Step 3: **Manual end-to-end** (dev login; no OAuth needed):
  1. `docker run --rm -d --name psy-pg -e POSTGRES_USER=psy -e POSTGRES_PASSWORD=psy -e POSTGRES_DB=psy -p 5432:5432 postgres:16` (wait for ready).
  2. `cd backend && DEV_LOGIN_ENABLED=true JWT_SECRET=dev go run ./cmd/server` (migrations run; listens :8080).
  3. Emulator app → Cài đặt → Sao lưu & đồng bộ → dev-login with an email → signed in.
  4. Ensure some transactions exist → "Sao lưu ngay" → success; verify a `snapshots` row exists (`docker exec psy-pg psql -U psy -d psy -c "select user_id, version, length(blob) from snapshots;"`).
  5. Delete/modify a transaction → "Khôi phục" → confirm → data returns to the backed-up state.
  6. Tear down: stop server, `docker stop psy-pg`.
- [ ] Step 4: `git --no-pager log --oneline && git status -s` clean.

---

## Self-Review Notes
- Spec coverage: backend auth+JWT+google+snapshot endpoints+tests (Task 1); android network+auth (Task 2); snapshot+backup repo (Task 3); UI+Google Sign-In+auto-backup (Task 4); verification incl. manual end-to-end (Task 5). All covered.
- Tests: Go handler/jwt tests (allowed); no Android unit tests.
- Type consistency: `TokenResponse(token)`/`BackupResponse(version,blob,updatedAt)` shared by APIs + repos; `NewRouter(cfg,pool)` signature change updates the existing health_test call; SnapshotDto mirrors the 5 entities; `Routes.BACKUP` shared by SettingsScreen + NavHost; AppViewModel.onStop gains best-effort auto-backup.
- The existing `/health` test must be updated for the new `NewRouter(cfg, pool)` signature (Task 1 notes this).
- Dev login is gated by `DEV_LOGIN_ENABLED`; Google path needs `GOOGLE_CLIENT_ID` (user-provided) — without it returns 503 and the UI shows "Chưa cấu hình Google", dev login still works.
