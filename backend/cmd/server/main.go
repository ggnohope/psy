package main

import (
	"context"
	"log"
	"net/http"

	"github.com/hoalam/psy/backend/internal/api"
	"github.com/hoalam/psy/backend/internal/config"
	"github.com/hoalam/psy/backend/internal/db"
)

func main() {
	cfg := config.Load()
	ctx := context.Background()

	pool, err := db.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("db connect: %v", err)
	}
	defer pool.Close() // runs only if main returns normally; Fatalf paths skip this

	if err := db.Migrate(ctx, pool); err != nil {
		log.Fatalf("migrate: %v", err)
	}

	r := api.NewRouter(cfg, pool)
	log.Printf("listening on :%s", cfg.Port)
	if err := http.ListenAndServe(":"+cfg.Port, r); err != nil {
		log.Fatalf("serve: %v", err)
	}
}
