# Psy — Backup/Auth Design Spec

**Date:** 2026-06-18
**Status:** Approved (brainstorming)
**Author:** hoalam

## Overview

The final v1 feature: cloud backup & restore of the whole local database, gated by authentication.
Auth has two paths — a **dev login** (email, no OAuth, for testing/dev) and **real Google Sign-In**
(Credential Manager → backend verifies the Google ID token). The Go backend (chi + Postgres, Phase-0
skeleton with `users`/`snapshots` tables) issues a JWT; the Android app serializes all Room tables to a
JSON snapshot and uploads/downloads it. Sync is manual ("Sao lưu ngay"/"Khôi phục") plus optional
auto-backup on app background.

**Testing:** dev login makes the full flow verifiable end-to-end without OAuth — run Postgres (Docker) +
the Go server + the emulator, dev-login, backup, restore. The Go backend gets lightweight handler tests
(correctness-critical). No Android unit tests (user preference) — build + manual.

## Scope (approved)

In: dev login + Google Sign-In; JWT; whole-DB JSON snapshot backup/restore (last-write-wins, single
snapshot per user); manual backup/restore + optional auto-backup on background; a "Sao lưu & đồng bộ"
settings screen. Backend: `/auth/dev`, `/auth/google`, `GET/POST /backup`, `GET /backup/meta`.

Deferred: per-entity delta sync; multi-device conflict UI; snapshot history/versioned restore; PIN/password account; refresh-token rotation.

## Backend (Go)

Config (`internal/config`): add `JWTSecret` (env `JWT_SECRET`, default a dev value), `GoogleClientID`
(env `GOOGLE_CLIENT_ID`, may be empty), `DevLoginEnabled` (env `DEV_LOGIN_ENABLED`, default true in dev).

Deps: `github.com/golang-jwt/jwt/v5`, `google.golang.org/api/idtoken`.

`internal/auth`:
- `IssueJWT(userID int64, secret) (string, error)` — HMAC-HS256, `sub`=userID, exp ~30 days.
- `ParseJWT(token, secret) (userID int64, error)`.
- `Middleware(secret)` — chi middleware: read `Authorization: Bearer <jwt>`, validate, put userID in context (`UserIDFromContext`). 401 on failure.
- Google: `VerifyGoogleIDToken(ctx, idToken, clientID) (sub, email string, error)` via `idtoken.Validate`.

`internal/user`: `UpsertBySub(ctx, pool, sub, email) (userID int64)` — INSERT ... ON CONFLICT(google_sub) DO UPDATE email RETURNING id.

Handlers (`internal/api`):
- `POST /auth/dev` (only if DevLoginEnabled): body `{email, name?}` → sub = `"dev:"+email`, UpsertBySub, IssueJWT → `{token}`. If disabled → 404.
- `POST /auth/google`: body `{idToken}` → VerifyGoogleIDToken (needs GoogleClientID; if empty → 503) → UpsertBySub(sub,email) → IssueJWT → `{token}`.
- Auth-protected group (Middleware):
  - `POST /backup`: body `{blob: string}` (base64 or raw JSON string) → upsert into `snapshots` (user_id PK): `version = old+1`, store blob bytes, `updated_at = now()` → `{version, updatedAt}`.
  - `GET /backup`: latest snapshot → `{version, blob, updatedAt}` or **204** if none.
  - `GET /backup/meta`: `{version, updatedAt, size}` or 204.

`internal/snapshotstore`: `Save(ctx, pool, userID, blob) (version, updatedAt)`, `Get(ctx, pool, userID) (blob, version, updatedAt, found)`.

Tests (Go, httptest, with a DB gated by `TEST_DATABASE_URL` like the existing migrate test, or an in-memory store interface): `/auth/dev` issues a token; `POST /backup` then `GET /backup` round-trips the blob and bumps version; auth middleware rejects missing/invalid tokens.

main.go: load config, mount routes (dev/google/backup), keep migrations.

## Android — network & auth

Deps: `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`,
`com.google.android.libraries.identity.googleid:googleid` (Google Sign-In via Credential Manager).
OkHttp is present via Retrofit.

- `data/remote/AuthApi` (Retrofit): `@POST("auth/dev") suspend fun devLogin(@Body DevLoginRequest): TokenResponse`; `@POST("auth/google") suspend fun googleLogin(@Body GoogleLoginRequest): TokenResponse`. DTOs `DevLoginRequest(email,name?)`, `GoogleLoginRequest(idToken)`, `TokenResponse(token)`.
- `data/remote/BackupApi`: `@POST("backup") suspend fun upload(@Body BackupRequest)`; `@GET("backup") suspend fun download(): Response<BackupResponse>` (handle 204). DTOs `BackupRequest(blob)`, `BackupResponse(version, blob, updatedAt)`.
- `data/auth/AuthTokenStore`: a DataStore (reuse the settings DataStore or a small one) holding `authToken: String?`, `userEmail: String?`, `lastSyncAt: Long?`, `autoBackup: Boolean`.
- Auth OkHttp interceptor: adds `Authorization: Bearer <token>` when present (read synchronously from the store). Provided via the NetworkModule (build an OkHttpClient with the interceptor and give it to Retrofit).
- `domain/repository/AuthRepository` (+impl): `authState: Flow<AuthState(signedIn, email)>`, `signInDev(email)`, `signInGoogle(idToken)`, `signOut()`.
- Google Sign-In helper (`ui/backup/GoogleSignInHelper` or in the screen): use `CredentialManager` + `GetGoogleIdOption` with the web client ID (`R.string.google_web_client_id`, a placeholder the user fills) → obtain the Google ID token → `signInGoogle`.

## Android — snapshot & backup

- `data/backup/SnapshotDto.kt` (`@Serializable`): `version: Int` + lists `ledgers, accounts, categories, transactions, budgets` (each a `@Serializable` DTO mirroring the Room entity fields). 
- `data/backup/SnapshotManager`: `export(): String` — read all rows from all DAOs (a suspend that queries each table once), build SnapshotDto, `Json.encodeToString`. `import(json: String)` — decode, then in a single Room `withTransaction`: clear all 5 tables and insert all rows (preserving ids). Add `deleteAll()`/`insertAll()` DAO methods or `@Query("DELETE FROM ...")` as needed.
- `domain/repository/BackupRepository` (+impl): `backupNow(): Result<Unit>` → export → `backupApi.upload`; on success store `lastSyncAt`. `restore(): Result<Unit>` → `download()` (204 → "no backup"); → SnapshotManager.import. Expose `lastSyncAt: Flow`.
- Auto-backup: in `AppViewModel.onStop` (existing lifecycle hook), if signed in && autoBackup enabled && (debounced since last) → fire `backupNow()` in a background scope. Keep it best-effort (ignore failures).

## UI

- `Routes.BACKUP = "backup"`. `SettingsScreen`: add row "Sao lưu & đồng bộ" (Icons.Default.CloudSync or CloudUpload) → onBackup. `PsyNavHost`: `composable(BACKUP){ BackupScreen(onBack) }`.
- `ui/backup/BackupViewModel` + `BackupScreen`: 
  - Signed out: a "Đăng nhập với Google" button (Credential Manager flow) + a dev-login section (email TextField + "Đăng nhập (dev)" button).
  - Signed in: show email + "Đăng xuất"; "Sao lưu ngay" button (shows progress + result), "Khôi phục" button (confirm dialog "Ghi đè dữ liệu local?"), "Lần sao lưu cuối: <time>" (or "Chưa sao lưu"), a "Tự động sao lưu" Switch.
  - Result/errors via a transient message (snackbar/text).

## What the user does by hand
- For real Google Sign-In: create an **OAuth 2.0 Web client ID** in Google Cloud Console; put it in
  backend env `GOOGLE_CLIENT_ID` and in Android `res/values` `google_web_client_id`.
- Run Postgres (Docker) and `go run ./cmd/server` (with `DEV_LOGIN_ENABLED=true`, `JWT_SECRET=...`).
- The Android emulator reaches the host backend at `http://10.0.2.2:8080/` (already the NetworkModule BASE_URL).

## Error / Edge handling
- Not signed in → backup/restore hidden/disabled; only sign-in shown.
- Restore → confirm dialog before overwriting local data; import runs in a transaction (all-or-nothing).
- `GET /backup` 204 (no snapshot yet) → "Chưa có bản sao lưu".
- Backup/restore network failure → non-blocking error message; no crash; local data untouched on backup failure.
- JWT expired/invalid (401) → clear token, prompt sign-in again.
- Auto-backup is best-effort and throttled (e.g. ≥ a few minutes between auto runs); never blocks UI.
- Google path with empty client ID → the Google button shows an explanatory message ("Chưa cấu hình Google"); dev login still works.

## Testing
- **Go**: handler tests (httptest) — dev login returns a token; backup POST→GET round-trip bumps version & returns the blob; middleware 401s without a valid token. DB-touching tests gated by `TEST_DATABASE_URL` (skip otherwise), consistent with the existing migrate test.
- **Android**: no unit tests. Build + lint.
- **Manual end-to-end** (the real proof): `docker run` Postgres → `DEV_LOGIN_ENABLED=true JWT_SECRET=dev go run ./cmd/server` → emulator app → Sao lưu & đồng bộ → dev-login (an email) → record a transaction → "Sao lưu ngay" → delete/modify data → "Khôi phục" → data returns. Confirm `snapshots` row exists in Postgres.

## File Structure (new/changed)
```
backend/internal/config/config.go (+JWTSecret, GoogleClientID, DevLoginEnabled)
backend/internal/auth/{jwt.go, middleware.go, google.go} (new)
backend/internal/user/user.go (UpsertBySub) (new)
backend/internal/snapshotstore/store.go (new)
backend/internal/api/{router.go(+routes), auth_handlers.go, backup_handlers.go, *_test.go} (new/changed)
backend/cmd/server/main.go (mount routes); backend/go.mod (+jwt, +idtoken)
android: data/remote/{AuthApi,BackupApi + DTOs}, data/auth/AuthTokenStore, data/backup/{SnapshotDto, SnapshotManager}, domain/repository/{AuthRepository,BackupRepository}, data/repo/{AuthRepositoryImpl,BackupRepositoryImpl}, di/NetworkModule(+auth interceptor, +apis), di bindings
android: ui/backup/{BackupScreen, BackupViewModel}, ui/navigation/{Routes(+BACKUP), PsyNavHost, SettingsScreen(+row)}
android: DAOs (+deleteAll/insertAll for import); res/values strings (google_web_client_id placeholder)
android: gradle catalog + app build (+credentials, +googleid)
```
