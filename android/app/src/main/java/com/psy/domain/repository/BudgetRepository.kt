package com.psy.domain.repository

import com.psy.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(ledgerId: Long): Flow<List<Budget>>
    suspend fun setBudget(ledgerId: Long, groupId: Long?, amountMinor: Long)
    suspend fun findByGroup(ledgerId: Long, groupId: Long): Budget?
    suspend fun removeBudget(budget: Budget)
}
