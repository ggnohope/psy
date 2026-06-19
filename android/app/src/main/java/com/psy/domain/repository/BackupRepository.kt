package com.psy.domain.repository

import kotlinx.coroutines.flow.Flow

sealed interface RestoreOutcome {
    object Restored : RestoreOutcome
    object NoBackup : RestoreOutcome
}

interface BackupRepository {
    val lastSyncAt: Flow<Long?>
    suspend fun backupNow(): Result<Unit>
    suspend fun restore(): Result<RestoreOutcome>

    /**
     * Called right after a successful login. When the local DB is empty, pulls the latest
     * cloud snapshot (restore) or, if the server has none, seeds default data. When the local
     * DB already has data, does nothing (avoids clobbering an existing account's data).
     */
    suspend fun prepareLocalDataAfterLogin()

    /** Clears all local data (used by the logout sequence). */
    suspend fun wipeLocal()
}
