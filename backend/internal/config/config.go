package config

import "os"

type Config struct {
	Port             string
	DatabaseURL      string
	JWTSecret        string
	GoogleClientID   string
	DevLoginEnabled  bool
}

func Load() Config {
	return Config{
		Port:            getenv("PORT", "8080"),
		DatabaseURL:     getenv("DATABASE_URL", "postgres://psy:psy@localhost:5432/psy?sslmode=disable"),
		JWTSecret:       getenv("JWT_SECRET", "dev-secret-change-me"),
		GoogleClientID:  getenv("GOOGLE_CLIENT_ID", ""),
		DevLoginEnabled: getenvBool("DEV_LOGIN_ENABLED", true),
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getenvBool(key string, fallback bool) bool {
	v := os.Getenv(key)
	switch v {
	case "true":
		return true
	case "false":
		return false
	default:
		return fallback
	}
}
