package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(tx: TransactionEntity): Long
    @Delete suspend fun delete(tx: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun getById(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE ledgerId = :ledgerId AND date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(ledgerId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>
}
