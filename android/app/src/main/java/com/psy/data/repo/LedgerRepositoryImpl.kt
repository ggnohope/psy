package com.psy.data.repo

import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.Ledger
import com.psy.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LedgerRepositoryImpl @Inject constructor(
    private val dao: LedgerDao
) : LedgerRepository {
    override fun observeAll(): Flow<List<Ledger>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun firstOrNull(): Ledger? = dao.firstOrNull()?.toDomain()
    override suspend fun upsert(ledger: Ledger): Long = dao.upsert(ledger.toEntity())
}
