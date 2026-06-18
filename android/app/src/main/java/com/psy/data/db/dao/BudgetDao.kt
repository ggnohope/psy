package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(budget: BudgetEntity): Long
    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId") fun observeAll(ledgerId: Long): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND categoryId IS NULL LIMIT 1") suspend fun findTotal(ledgerId: Long): BudgetEntity?
    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND categoryId = :categoryId LIMIT 1") suspend fun findByCategory(ledgerId: Long, categoryId: Long): BudgetEntity?
    @Delete suspend fun delete(budget: BudgetEntity)

    // ── Backup support ──────────────────────────────────────────────────────
    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BudgetEntity>)
}
