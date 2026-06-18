package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(category: CategoryEntity): Long
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC") fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
    @Delete suspend fun delete(category: CategoryEntity)

    // ── Backup support ──────────────────────────────────────────────────────
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)
}
