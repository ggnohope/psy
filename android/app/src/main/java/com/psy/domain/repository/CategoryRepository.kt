package com.psy.domain.repository

import com.psy.domain.model.Category
import com.psy.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByType(type: CategoryType): Flow<List<Category>>
    suspend fun count(): Int
    suspend fun upsert(category: Category): Long
    suspend fun delete(category: Category)
}
