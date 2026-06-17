package com.psy.data.repo

import com.psy.data.db.dao.AccountDao
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.Account
import com.psy.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao
) : AccountRepository {
    override fun observeAll(): Flow<List<Account>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun count(): Int = dao.count()
    override suspend fun upsert(account: Account): Long = dao.upsert(account.toEntity())
}
