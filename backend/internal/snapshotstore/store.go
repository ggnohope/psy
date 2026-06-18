package snapshotstore

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Save upserts the snapshot for userID, incrementing the version on conflict.
// Returns the new version and updatedAt timestamp.
func Save(ctx context.Context, pool *pgxpool.Pool, userID int64, blob []byte) (version int64, updatedAt time.Time, err error) {
	const q = `
		INSERT INTO snapshots(user_id, version, blob, updated_at)
		VALUES($1, 1, $2, now())
		ON CONFLICT(user_id) DO UPDATE
			SET version    = snapshots.version + 1,
			    blob       = EXCLUDED.blob,
			    updated_at = now()
		RETURNING version, updated_at`
	err = pool.QueryRow(ctx, q, userID, blob).Scan(&version, &updatedAt)
	return
}

// Get retrieves the snapshot for userID. If no snapshot exists, found is false.
func Get(ctx context.Context, pool *pgxpool.Pool, userID int64) (blob []byte, version int64, updatedAt time.Time, found bool, err error) {
	const q = `SELECT blob, version, updated_at FROM snapshots WHERE user_id = $1`
	err = pool.QueryRow(ctx, q, userID).Scan(&blob, &version, &updatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, 0, time.Time{}, false, nil
	}
	if err != nil {
		return nil, 0, time.Time{}, false, err
	}
	return blob, version, updatedAt, true, nil
}
