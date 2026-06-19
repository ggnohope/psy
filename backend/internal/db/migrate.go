package db

import (
	"context"
	"embed"
	"fmt"
	"sort"

	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed migrations/*.sql
var migrationFS embed.FS

// Migrate applies all embedded migration files in lexical order.
func Migrate(ctx context.Context, pool *pgxpool.Pool) error {
	entries, err := migrationFS.ReadDir("migrations")
	if err != nil {
		return err
	}
	names := make([]string, 0, len(entries))
	for _, e := range entries {
		names = append(names, e.Name())
	}
	sort.Strings(names)

	// Run on one dedicated connection guarded by a session-level advisory lock so
	// concurrent migrators (parallel test packages sharing a DB, or simultaneous
	// server startups) serialize instead of racing. `CREATE TABLE IF NOT EXISTS`
	// is NOT atomic: two sessions can both pass the existence check and then
	// collide in the system catalog (SQLSTATE 42P07 / 23505 on the implicit row
	// type). The lock makes the second migrator wait, then see the tables already
	// exist and no-op.
	conn, err := pool.Acquire(ctx)
	if err != nil {
		return fmt.Errorf("acquire connection: %w", err)
	}
	defer conn.Release()

	const migrateLockKey int64 = 4927 // arbitrary constant shared by all migrators
	if _, err := conn.Exec(ctx, "SELECT pg_advisory_lock($1)", migrateLockKey); err != nil {
		return fmt.Errorf("acquire migrate lock: %w", err)
	}
	defer func() {
		// Best-effort unlock on a fresh context so it runs even if ctx is cancelled.
		_, _ = conn.Exec(context.Background(), "SELECT pg_advisory_unlock($1)", migrateLockKey)
	}()

	for _, name := range names {
		sqlBytes, err := migrationFS.ReadFile("migrations/" + name)
		if err != nil {
			return fmt.Errorf("read migration %s: %w", name, err)
		}
		if _, err := conn.Exec(ctx, string(sqlBytes)); err != nil {
			return fmt.Errorf("migration %s: %w", name, err)
		}
	}
	return nil
}
