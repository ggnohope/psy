package api

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/hoalam/psy/backend/internal/auth"
	"github.com/hoalam/psy/backend/internal/snapshotstore"
)

// handleBackupUpload saves the snapshot blob for the authenticated user.
// Request: { "blob": "<json string>" }
// Response: { "version": N, "updatedAt": "<RFC3339>" }
func (h *Handlers) handleBackupUpload(w http.ResponseWriter, r *http.Request) {
	userID, ok := auth.UserID(r.Context())
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]string{"error": "unauthorized"})
		return
	}
	var req struct {
		Blob string `json:"blob"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid request body"})
		return
	}
	version, updatedAt, err := snapshotstore.Save(r.Context(), h.pool, userID, []byte(req.Blob))
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "save snapshot: " + err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"version":   version,
		"updatedAt": updatedAt.UTC().Format(time.RFC3339),
	})
}

// handleBackupDownload returns the latest snapshot blob for the authenticated user.
// Returns 204 (no body) if no snapshot exists.
func (h *Handlers) handleBackupDownload(w http.ResponseWriter, r *http.Request) {
	userID, ok := auth.UserID(r.Context())
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]string{"error": "unauthorized"})
		return
	}
	blob, version, updatedAt, found, err := snapshotstore.Get(r.Context(), h.pool, userID)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "get snapshot: " + err.Error()})
		return
	}
	if !found {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"version":   version,
		"blob":      string(blob),
		"updatedAt": updatedAt.UTC().Format(time.RFC3339),
	})
}

// handleBackupMeta returns snapshot metadata (version, updatedAt, size) without the blob.
// Returns 204 if no snapshot exists.
func (h *Handlers) handleBackupMeta(w http.ResponseWriter, r *http.Request) {
	userID, ok := auth.UserID(r.Context())
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]string{"error": "unauthorized"})
		return
	}
	blob, version, updatedAt, found, err := snapshotstore.Get(r.Context(), h.pool, userID)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "get snapshot meta: " + err.Error()})
		return
	}
	if !found {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"version":   version,
		"updatedAt": updatedAt.UTC().Format(time.RFC3339),
		"size":      len(blob),
	})
}
