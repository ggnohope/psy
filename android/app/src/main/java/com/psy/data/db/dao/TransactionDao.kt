package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(tx: TransactionEntity): Long
    @Delete suspend fun delete(tx: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun getById(id: Long): TransactionEntity?
    /** Half-open range [start, end): callers pass end = start of the next period (e.g. first ms of next month). */
    @Query("SELECT * FROM transactions WHERE ledgerId = :ledgerId AND date >= :start AND date < :end ORDER BY date DESC, id DESC")
    fun observeBetween(ledgerId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>

    // ── Backup support ──────────────────────────────────────────────────────
    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionEntity>)
}
