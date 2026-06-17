package com.psy.domain.repository

import com.psy.domain.model.Ledger
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun observeAll(): Flow<List<Ledger>>
    suspend fun firstOrNull(): Ledger?
    suspend fun upsert(ledger: Ledger): Long
}
