package com.psy.data.repo

import com.psy.data.auth.AuthTokenStore
import com.psy.data.backup.SnapshotManager
import com.psy.data.remote.BackupApi
import com.psy.data.remote.dto.BackupRequest
import com.psy.domain.repository.BackupRepository
import com.psy.domain.repository.RestoreOutcome
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val backupApi: BackupApi,
    private val snapshotManager: SnapshotManager,
    private val tokenStore: AuthTokenStore,
) : BackupRepository {

    override val lastSyncAt: Flow<Long?> = tokenStore.lastSyncAtFlow

    override suspend fun backupNow(): Result<Unit> = runCatching {
        val blob = snapshotManager.export()
        backupApi.upload(BackupRequest(blob))
        tokenStore.setLastSyncAt(System.currentTimeMillis())
    }

    override suspend fun restore(): Result<RestoreOutcome> = runCatching {
        val resp = backupApi.download()
        if (resp.code() == 204 || resp.body() == null) {
            RestoreOutcome.NoBackup
        } else {
            snapshotManager.import(resp.body()!!.blob)
            RestoreOutcome.Restored
        }
    }
}
