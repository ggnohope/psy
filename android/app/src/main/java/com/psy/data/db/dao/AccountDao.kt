package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(account: AccountEntity): Long
    @Query("SELECT * FROM accounts ORDER BY id ASC") fun observeAll(): Flow<List<AccountEntity>>
    @Query("SELECT COUNT(*) FROM accounts") suspend fun count(): Int

    // ── Backup support ──────────────────────────────────────────────────────
    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AccountEntity>)
}
