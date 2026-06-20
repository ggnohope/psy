package api

import (
	"encoding/json"
	"net/http"

	"github.com/hoalam/psy/backend/internal/auth"
	"github.com/hoalam/psy/backend/internal/user"
)

// handleGoogleLogin validates a Google ID token and returns a JWT.
// Returns 503 when GOOGLE_CLIENT_ID is not configured.
func (h *Handlers) handleGoogleLogin(w http.ResponseWriter, r *http.Request) {
	if len(h.cfg.GoogleClientIDs) == 0 {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"error": "google login not configured"})
		return
	}
	var req struct {
		IDToken string `json:"idToken"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.IDToken == "" {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "idToken required"})
		return
	}
	sub, email, err := auth.VerifyGoogleIDToken(r.Context(), req.IDToken, h.cfg.GoogleClientIDs)
	if err != nil {
		writeJSON(w, http.StatusUnauthorized, map[string]string{"error": "invalid google token: " + err.Error()})
		return
	}
	userID, err := user.UpsertBySub(r.Context(), h.pool, sub, email)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "upsert user: " + err.Error()})
		return
	}
	token, err := auth.IssueJWT(userID, h.cfg.JWTSecret)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "issue jwt: " + err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"token": token})
}

// writeJSON sets Content-Type, status, and encodes v as JSON.
func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
