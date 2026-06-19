package db

import (
	"context"
	"os"
	"sync"
	"testing"
)

// TestMigrateConcurrent guards against the CI regression where parallel test
// packages (or simultaneous server startups) migrate the same database at once.
// `CREATE TABLE IF NOT EXISTS` is not atomic, so without serialization the
// concurrent creates collide in the catalog (SQLSTATE 42P07 / 23505). Migrate
// must serialize via an advisory lock and stay error-free under concurrency.
func TestMigrateConcurrent(t *testing.T) {
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

	// Clean slate so every goroutine actually executes CREATE TABLE.
	if _, err := pool.Exec(ctx, "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"); err != nil {
		t.Fatalf("reset schema: %v", err)
	}

	const n = 8
	var wg sync.WaitGroup
	errs := make([]error, n)
	for i := 0; i < n; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			errs[i] = Migrate(ctx, pool)
		}(i)
	}
	wg.Wait()

	for i, e := range errs {
		if e != nil {
			t.Fatalf("concurrent Migrate #%d failed: %v", i, e)
		}
	}
}
