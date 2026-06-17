package db

import (
	"context"
	"os"
	"testing"
)

func TestMigrateCreatesTables(t *testing.T) {
	url := os.Getenv("TEST_DATABASE_URL")
	if url == "" {
		t.Skip("set TEST_DATABASE_URL to run migration test")
	}
	ctx := context.Background()

	pool, err := Connect(ctx, url)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	defer pool.Close()

	if err := Migrate(ctx, pool); err != nil {
		t.Fatalf("migrate: %v", err)
	}

	var exists bool
	err = pool.QueryRow(ctx,
		`SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'snapshots')`,
	).Scan(&exists)
	if err != nil {
		t.Fatalf("query: %v", err)
	}
	if !exists {
		t.Fatal("snapshots table was not created")
	}

	err = pool.QueryRow(ctx,
		`SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users')`,
	).Scan(&exists)
	if err != nil {
		t.Fatalf("query users: %v", err)
	}
	if !exists {
		t.Fatal("users table was not created")
	}
}
