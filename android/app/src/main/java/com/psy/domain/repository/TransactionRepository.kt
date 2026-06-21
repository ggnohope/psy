package com.psy.domain.repository

import com.psy.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeBetween(ledgerId: Long, start: Long, end: Long): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    suspend fun getById(id: Long): Transaction?
    suspend fun upsert(tx: Transaction): Long
    suspend fun delete(tx: Transaction)
}
