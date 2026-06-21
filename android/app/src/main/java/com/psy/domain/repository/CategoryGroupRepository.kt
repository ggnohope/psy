package com.psy.domain.repository

import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryGroupRepository {
    fun observeAll(): Flow<List<CategoryGroup>>
    fun observeByType(type: CategoryType): Flow<List<CategoryGroup>>
    suspend fun count(): Int
    suspend fun upsert(group: CategoryGroup): Long
    suspend fun delete(group: CategoryGroup)
}
