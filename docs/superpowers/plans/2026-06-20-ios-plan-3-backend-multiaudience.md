# iOS Port — Plan 3: Backend Multi-Audience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Let the Go backend accept Google ID tokens from MORE than one OAuth client (the existing web client used by Android + the new iOS client), so iOS login validates end-to-end. Fully backward-compatible.

**Architecture:** Add `GoogleClientIDs []string` to config, parsed from a new `GOOGLE_CLIENT_IDS` CSV env var with fallback to the existing `GOOGLE_CLIENT_ID`. `VerifyGoogleIDToken` tries each audience until one validates. Handler uses the list.

**Tech Stack:** Go, chi, google.golang.org/api/idtoken.

**Reference:** `backend/internal/config/config.go`, `backend/internal/auth/google.go`, `backend/internal/api/auth_handlers.go`. Spec §7.

**Prerequisites:** none (independent of iOS). Verify with `cd backend && go build ./... && go vet ./... && go test ./internal/config/... ./internal/auth/...` (these packages need no Postgres).

---

## Task 1: Config — parse multiple client IDs (TDD)

**Files:**
- Create: `backend/internal/config/config_test.go`
- Modify: `backend/internal/config/config.go`

- [ ] **Step 1: Write failing test**

`backend/internal/config/config_test.go`:
```go
package config

import (
	"reflect"
	"testing"
)

func TestParseClientIDs(t *testing.T) {
	cases := []struct {
		name   string
		csv    string
		single string
		want   []string
	}{
		{"csv wins, trims spaces", " web , ios ", "legacy", []string{"web", "ios"}},
		{"fallback to single when csv empty", "", "legacy", []string{"legacy"}},
		{"single csv value", "only", "", []string{"only"}},
		{"both empty -> nil", "", "", nil},
		{"csv with empty parts dropped", "a,,b,", "x", []string{"a", "b"}},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			got := parseClientIDs(c.csv, c.single)
			if !reflect.DeepEqual(got, c.want) {
				t.Fatalf("parseClientIDs(%q,%q) = %#v, want %#v", c.csv, c.single, got, c.want)
			}
		})
	}
}
```

- [ ] **Step 2: Run, confirm fail**

Run: `cd backend && go test ./internal/config/...`
Expected: FAIL — `undefined: parseClientIDs`.

- [ ] **Step 3: Implement**

Modify `backend/internal/config/config.go` to:
```go
package config

import (
	"os"
	"strings"
)

type Config struct {
	Port            string
	DatabaseURL     string
	JWTSecret       string
	GoogleClientID  string   // legacy single (kept for compatibility)
	GoogleClientIDs []string // accepted audiences for Google ID tokens
}

func Load() Config {
	single := getenv("GOOGLE_CLIENT_ID", "")
	return Config{
		Port:            getenv("PORT", "8080"),
		DatabaseURL:     getenv("DATABASE_URL", "postgres://psy:psy@localhost:5432/psy?sslmode=disable"),
		JWTSecret:       getenv("JWT_SECRET", "dev-secret-change-me"),
		GoogleClientID:  single,
		GoogleClientIDs: parseClientIDs(getenv("GOOGLE_CLIENT_IDS", ""), single),
	}
}

// parseClientIDs prefers the comma-separated csv (trimming blanks); if it yields
// nothing, it falls back to the single legacy value. Returns nil when both empty.
func parseClientIDs(csv, single string) []string {
	if csv != "" {
		parts := strings.Split(csv, ",")
		out := make([]string, 0, len(parts))
		for _, p := range parts {
			if t := strings.TrimSpace(p); t != "" {
				out = append(out, t)
			}
		}
		if len(out) > 0 {
			return out
		}
	}
	if single != "" {
		return []string{single}
	}
	return nil
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
```

- [ ] **Step 4: Run, confirm pass**

Run: `cd backend && go test ./internal/config/...`
Expected: PASS (5 subtests).

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add backend/internal/config/config.go backend/internal/config/config_test.go
git commit -m "feat(backend): parse GOOGLE_CLIENT_IDS (CSV) with legacy fallback"
```

---

## Task 2: Verify token against multiple audiences

**Files:**
- Modify: `backend/internal/auth/google.go`

- [ ] **Step 1: Change signature to accept a slice and loop**

Replace `backend/internal/auth/google.go` with:
```go
package auth

import (
	"context"
	"fmt"

	"google.golang.org/api/idtoken"
)

// VerifyGoogleIDToken validates the given Google ID token against any of the accepted
// clientIDs (audiences) and returns the subject (Google user ID) and email. It succeeds
// as soon as one audience validates; if none do, it returns the last validation error.
func VerifyGoogleIDToken(ctx context.Context, idTokenStr string, clientIDs []string) (sub, email string, err error) {
	if len(clientIDs) == 0 {
		return "", "", fmt.Errorf("no google client ids configured")
	}
	var lastErr error
	for _, clientID := range clientIDs {
		payload, e := idtoken.Validate(ctx, idTokenStr, clientID)
		if e != nil {
			lastErr = e
			continue
		}
		sub = payload.Subject
		if sub == "" {
			return "", "", fmt.Errorf("empty subject in token payload")
		}
		emailVal, _ := payload.Claims["email"].(string)
		return sub, emailVal, nil
	}
	return "", "", fmt.Errorf("idtoken validate (all %d audiences failed): %w", len(clientIDs), lastErr)
}
```

- [ ] **Step 2: Verify build**

Run: `cd backend && go build ./...`
Expected: FAIL to build (the handler still calls the old 3-arg form) — that's fixed in Task 3. It is OK for this step to show the handler compile error; do NOT change the handler here. (If you prefer a clean build per task, you may do Task 3 immediately after; commit them together is also acceptable. To keep tasks atomic, commit this now even though `go build ./...` is red until Task 3.)

- [ ] **Step 3: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add backend/internal/auth/google.go
git commit -m "feat(backend): VerifyGoogleIDToken accepts multiple audiences"
```

---

## Task 3: Handler uses the audience list

**Files:**
- Modify: `backend/internal/api/auth_handlers.go`

- [ ] **Step 1: Use `GoogleClientIDs`**

In `backend/internal/api/auth_handlers.go`, change the "not configured" guard and the verify call:
- Replace `if h.cfg.GoogleClientID == "" {` with:
```go
	if len(h.cfg.GoogleClientIDs) == 0 {
```
- Replace `sub, email, err := auth.VerifyGoogleIDToken(r.Context(), req.IDToken, h.cfg.GoogleClientID)` with:
```go
	sub, email, err := auth.VerifyGoogleIDToken(r.Context(), req.IDToken, h.cfg.GoogleClientIDs)
```

- [ ] **Step 2: Verify build + vet + tests (no-DB packages)**

Run:
```bash
cd backend && go build ./... && go vet ./... && go test ./internal/config/... ./internal/auth/...
```
Expected: build clean, vet clean, tests PASS.

- [ ] **Step 3: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add backend/internal/api/auth_handlers.go
git commit -m "feat(backend): google login validates against all configured audiences"
```

---

## Task 4: Document the new env var

**Files:**
- Modify: `docs/CICD.md`

- [ ] **Step 1: Add a note**

In `docs/CICD.md`, find the section listing the EC2 `.env` / backend secrets and add this line (keep the surrounding bilingual style):
```markdown
- `GOOGLE_CLIENT_IDS` (tùy chọn): danh sách audience Google ID token được chấp nhận, phân tách bằng dấu phẩy — ví dụ `WEB_CLIENT_ID,IOS_CLIENT_ID`. Cho phép cùng backend phục vụ cả Android (web client) và iOS (iOS client). Nếu để trống, fallback về `GOOGLE_CLIENT_ID` (tương thích ngược).
```
If `docs/CICD.md` has no obvious env/secrets list, add the line under a clear heading near where `GOOGLE_CLIENT_ID` or other env vars are mentioned (search the file for `GOOGLE_CLIENT_ID`). If the term does not appear at all, append a short `## Google audiences` subsection with the line above.

- [ ] **Step 2: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add docs/CICD.md
git commit -m "docs(backend): document GOOGLE_CLIENT_IDS multi-audience env var"
```

---

## Self-Review (completed during authoring)

**Spec coverage (§7):** config CSV+fallback (Task 1), multi-audience validate (Task 2), handler wiring (Task 3), docs/env (Task 4). The EC2 `.env` value itself is a manual deploy step (already in the user's manual-work guide), not a code change.

**No-DB verification:** `internal/config` and `internal/auth` tests need no Postgres, so the new logic is fully verifiable locally. Full `go test ./...` (incl. `internal/api` backup tests that may need a DB) is exercised by the existing `backend-ci` GitHub Action with its Postgres service.

**Placeholder scan:** none. Task 2 Step 2's intentional red build (signature change before handler update) is called out explicitly, not a placeholder.

**Type consistency:** `VerifyGoogleIDToken(ctx, token, clientIDs []string)` signature matches its only caller in Task 3. `Config.GoogleClientIDs` used in both config and handler. `parseClientIDs` name consistent between impl and test.

---

## Execution Handoff

Plan 3 complete and independent. After this, **Plan 4 (Offline features)** is the main remaining work (depends on Plans 1+2).
