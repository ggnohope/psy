package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.CategoryGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(group: CategoryGroupEntity): Long
    @Query("SELECT * FROM category_groups WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: String): Flow<List<CategoryGroupEntity>>
    @Query("SELECT * FROM category_groups ORDER BY sortOrder ASC") fun observeAll(): Flow<List<CategoryGroupEntity>>
    @Query("SELECT COUNT(*) FROM category_groups") suspend fun count(): Int
    @Delete suspend fun delete(group: CategoryGroupEntity)

    // ── Backup support ──────────────────────────────────────────────────────
    @Query("SELECT * FROM category_groups")
    suspend fun getAll(): List<CategoryGroupEntity>

    @Query("DELETE FROM category_groups")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryGroupEntity>)
}
