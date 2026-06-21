package com.psy.data.repo

import com.psy.data.db.dao.CategoryGroupDao
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import com.psy.domain.repository.CategoryGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryGroupRepositoryImpl @Inject constructor(
    private val dao: CategoryGroupDao
) : CategoryGroupRepository {
    override fun observeAll(): Flow<List<CategoryGroup>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeByType(type: CategoryType): Flow<List<CategoryGroup>> =
        dao.observeByType(type.name).map { l -> l.map { it.toDomain() } }
    override suspend fun count(): Int = dao.count()
    override suspend fun upsert(group: CategoryGroup): Long = dao.upsert(group.toEntity())
    override suspend fun delete(group: CategoryGroup) = dao.delete(group.toEntity())
}
