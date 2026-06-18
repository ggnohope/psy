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
}
