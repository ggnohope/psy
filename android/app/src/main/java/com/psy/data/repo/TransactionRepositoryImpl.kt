package com.psy.data.repo

import com.psy.data.db.dao.TransactionDao
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.Transaction
import com.psy.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {
    override fun observeBetween(ledgerId: Long, start: Long, end: Long): Flow<List<Transaction>> =
        dao.observeBetween(ledgerId, start, end).map { list -> list.map { it.toDomain() } }
    override suspend fun getById(id: Long): Transaction? = dao.getById(id)?.toDomain()
    override suspend fun upsert(tx: Transaction): Long = dao.upsert(tx.toEntity())
    override suspend fun delete(tx: Transaction) = dao.delete(tx.toEntity())
}
