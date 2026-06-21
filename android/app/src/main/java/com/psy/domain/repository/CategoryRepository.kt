package com.psy.domain.repository

import com.psy.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByGroup(groupId: Long): Flow<List<Category>>
    suspend fun count(): Int
    suspend fun countByGroup(groupId: Long): Int
    suspend fun upsert(category: Category): Long
    suspend fun delete(category: Category)
}
