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
