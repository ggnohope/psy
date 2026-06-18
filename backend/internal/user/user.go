package user

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

// UpsertBySub inserts a new user identified by google_sub, or updates the email
// if a user with that sub already exists. Returns the user's ID.
func UpsertBySub(ctx context.Context, pool *pgxpool.Pool, sub, email string) (int64, error) {
	const q = `
		INSERT INTO users(google_sub, email)
		VALUES($1, $2)
		ON CONFLICT(google_sub) DO UPDATE SET email = EXCLUDED.email
		RETURNING id`
	var id int64
	err := pool.QueryRow(ctx, q, sub, email).Scan(&id)
	return id, err
}
