package com.psy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.psy.data.db.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: LedgerEntity)

    @Query("SELECT * FROM ledgers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LedgerEntity>>
}
