package com.psy.domain.repository

import com.psy.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    suspend fun count(): Int
    suspend fun upsert(account: Account): Long
}
