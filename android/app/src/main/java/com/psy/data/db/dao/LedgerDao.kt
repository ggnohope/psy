package com.psy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.psy.data.db.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ledger: LedgerEntity): Long

    @Query("SELECT * FROM ledgers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers ORDER BY createdAt ASC LIMIT 1")
    suspend fun firstOrNull(): LedgerEntity?

    @Query("SELECT COUNT(*) FROM ledgers")
    suspend fun count(): Int

    // ── Backup support ──────────────────────────────────────────────────────
    @Query("SELECT * FROM ledgers")
    suspend fun getAll(): List<LedgerEntity>

    @Query("DELETE FROM ledgers")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LedgerEntity>)
}
