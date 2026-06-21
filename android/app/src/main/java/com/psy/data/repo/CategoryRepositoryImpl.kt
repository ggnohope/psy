package com.psy.data.repo

import com.psy.data.db.dao.CategoryDao
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.Category
import com.psy.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeByGroup(groupId: Long): Flow<List<Category>> =
        dao.observeByGroup(groupId).map { l -> l.map { it.toDomain() } }
    override suspend fun count(): Int = dao.count()
    override suspend fun countByGroup(groupId: Long): Int = dao.countByGroup(groupId)
    override suspend fun upsert(category: Category): Long = dao.upsert(category.toEntity())
    override suspend fun delete(category: Category) = dao.delete(category.toEntity())
}
