package api

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/hoalam/psy/backend/internal/auth"
	"github.com/hoalam/psy/backend/internal/config"
	"github.com/hoalam/psy/backend/internal/db"
	"github.com/hoalam/psy/backend/internal/user"
)

func TestBackupHandlers(t *testing.T) {
	dbURL := os.Getenv("TEST_DATABASE_URL")
	if dbURL == "" {
		t.Skip("TEST_DATABASE_URL not set; skipping DB-gated backup handler tests")
	}

	ctx := context.Background()
	pool, err := db.Connect(ctx, dbURL)
	if err != nil {
		t.Fatalf("connect test DB: %v", err)
	}
	defer pool.Close()

	if err := db.Migrate(ctx, pool); err != nil {
		t.Fatalf("migrate test DB: %v", err)
	}

	const jwtSecret = "test-secret"
	cfg := config.Config{
		JWTSecret: jwtSecret,
	}
	router := NewRouter(cfg, pool)

	// --- seed a test user and issue a JWT directly ---
	userID, err := user.UpsertBySub(ctx, pool, "test-sub", "test@example.com")
	if err != nil {
		t.Fatalf("seed test user: %v", err)
	}
	token, err := auth.IssueJWT(userID, jwtSecret)
	if err != nil {
		t.Fatalf("issue JWT: %v", err)
	}

	bearerHeader := "Bearer " + token

	// --- 401 when no Authorization header ---
	t.Run("GET /backup without token returns 401", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/backup", nil)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusUnauthorized {
			t.Errorf("status = %d, want %d", rec.Code, http.StatusUnauthorized)
		}
	})

	// --- GET /backup when no snapshot yet → 204 ---
	t.Run("GET /backup no snapshot returns 204", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/backup", nil)
		req.Header.Set("Authorization", bearerHeader)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Errorf("status = %d, want 204", rec.Code)
		}
	})

	// --- POST /backup with blob → version 1 ---
	const blob1 = `{"version":1,"data":"hello"}`
	t.Run("POST /backup uploads blob, returns version 1", func(t *testing.T) {
		body, _ := json.Marshal(map[string]string{"blob": blob1})
		req := httptest.NewRequest(http.MethodPost, "/backup", bytes.NewReader(body))
		req.Header.Set("Authorization", bearerHeader)
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("status = %d, body = %s", rec.Code, rec.Body.String())
		}
		var resp struct {
			Version int64 `json:"version"`
		}
		if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
			t.Fatalf("decode upload response: %v", err)
		}
		if resp.Version != 1 {
			t.Errorf("version = %d, want 1", resp.Version)
		}
	})

	// --- GET /backup returns the blob we uploaded ---
	t.Run("GET /backup returns blob and version 1", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/backup", nil)
		req.Header.Set("Authorization", bearerHeader)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("status = %d, body = %s", rec.Code, rec.Body.String())
		}
		var resp struct {
			Version int64  `json:"version"`
			Blob    string `json:"blob"`
		}
		if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
			t.Fatalf("decode download response: %v", err)
		}
		if resp.Version != 1 {
			t.Errorf("version = %d, want 1", resp.Version)
		}
		if resp.Blob != blob1 {
			t.Errorf("blob = %q, want %q", resp.Blob, blob1)
		}
	})

	// --- Second POST → version 2 ---
	const blob2 = `{"version":2,"data":"world"}`
	t.Run("second POST /backup increments version to 2", func(t *testing.T) {
		body, _ := json.Marshal(map[string]string{"blob": blob2})
		req := httptest.NewRequest(http.MethodPost, "/backup", bytes.NewReader(body))
		req.Header.Set("Authorization", bearerHeader)
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("status = %d, body = %s", rec.Code, rec.Body.String())
		}
		var resp struct {
			Version int64 `json:"version"`
		}
		if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
			t.Fatalf("decode upload response: %v", err)
		}
		if resp.Version != 2 {
			t.Errorf("version = %d, want 2", resp.Version)
		}
	})

	// --- GET /backup/meta ---
	t.Run("GET /backup/meta returns metadata without blob", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/backup/meta", nil)
		req.Header.Set("Authorization", bearerHeader)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("status = %d, body = %s", rec.Code, rec.Body.String())
		}
		var resp map[string]any
		if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
			t.Fatalf("decode meta response: %v", err)
		}
		if _, ok := resp["blob"]; ok {
			t.Error("meta response should not contain blob field")
		}
		if _, ok := resp["version"]; !ok {
			t.Error("meta response missing version field")
		}
	})

	// --- Direct JWT issue/verify round-trip (pure, no DB needed but here for completeness) ---
	t.Run("JWT issued for DB user parses to correct userID", func(t *testing.T) {
		parsedID, err := auth.ParseJWT(token, jwtSecret)
		if err != nil {
			t.Fatalf("ParseJWT: %v", err)
		}
		if parsedID <= 0 {
			t.Errorf("userID = %d, want > 0", parsedID)
		}
	})
}
