package api

import (
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/hoalam/psy/backend/internal/auth"
	"github.com/hoalam/psy/backend/internal/config"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Handlers holds dependencies shared across HTTP handlers.
type Handlers struct {
	cfg  config.Config
	pool *pgxpool.Pool
}

// NewRouter builds the HTTP router with all routes mounted.
// pool may be nil in tests that only exercise routes not requiring DB access.
func NewRouter(cfg config.Config, pool *pgxpool.Pool) chi.Router {
	h := &Handlers{cfg: cfg, pool: pool}

	r := chi.NewRouter()
	r.Use(middleware.Recoverer)

	r.Get("/health", handleHealth)

	// Auth routes (no JWT required)
	r.Post("/auth/dev", h.handleDevLogin)
	r.Post("/auth/google", h.handleGoogleLogin)

	// Backup routes (JWT required)
	r.Group(func(r chi.Router) {
		r.Use(auth.Middleware(cfg.JWTSecret))
		r.Post("/backup", h.handleBackupUpload)
		r.Get("/backup", h.handleBackupDownload)
		r.Get("/backup/meta", h.handleBackupMeta)
	})

	return r
}
