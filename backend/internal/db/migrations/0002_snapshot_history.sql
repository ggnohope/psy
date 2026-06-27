-- Append-only history of every uploaded snapshot version.
-- Safety net: the `snapshots` table keeps only the latest blob (UPSERT overwrites in
-- place), so a buggy client that uploads an empty/seed snapshot used to destroy the
-- previous good backup irrecoverably. This table retains the last N versions per user
-- so any overwrite stays recoverable. See snapshotstore.Save.
CREATE TABLE IF NOT EXISTS snapshot_history (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    version     BIGINT NOT NULL,
    blob        BYTEA NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS snapshot_history_user_version_idx
    ON snapshot_history (user_id, version DESC);
