package snapshotstore

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// historyKeep is how many past snapshot versions to retain per user. The latest lives
// in `snapshots`; the previous historyKeep-1 are recoverable from `snapshot_history`.
const historyKeep = 20

// Save upserts the snapshot for userID, incrementing the version on conflict, and
// appends the new version to snapshot_history (pruned to the last historyKeep) so an
// overwrite is never destructive. Returns the new version and updatedAt timestamp.
func Save(ctx context.Context, pool *pgxpool.Pool, userID int64, blob []byte) (version int64, updatedAt time.Time, err error) {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return 0, time.Time{}, err
	}
	defer tx.Rollback(ctx) //nolint:errcheck // no-op after Commit

	const upsert = `
		INSERT INTO snapshots(user_id, version, blob, updated_at)
		VALUES($1, 1, $2, now())
		ON CONFLICT(user_id) DO UPDATE
			SET version    = snapshots.version + 1,
			    blob       = EXCLUDED.blob,
			    updated_at = now()
		RETURNING version, updated_at`
	if err = tx.QueryRow(ctx, upsert, userID, blob).Scan(&version, &updatedAt); err != nil {
		return 0, time.Time{}, err
	}

	const insertHistory = `INSERT INTO snapshot_history(user_id, version, blob) VALUES($1, $2, $3)`
	if _, err = tx.Exec(ctx, insertHistory, userID, version, blob); err != nil {
		return 0, time.Time{}, err
	}

	const prune = `
		DELETE FROM snapshot_history
		WHERE user_id = $1
		  AND id NOT IN (
		      SELECT id FROM snapshot_history
		      WHERE user_id = $1
		      ORDER BY version DESC
		      LIMIT $2
		  )`
	if _, err = tx.Exec(ctx, prune, userID, historyKeep); err != nil {
		return 0, time.Time{}, err
	}

	if err = tx.Commit(ctx); err != nil {
		return 0, time.Time{}, err
	}
	return version, updatedAt, nil
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
