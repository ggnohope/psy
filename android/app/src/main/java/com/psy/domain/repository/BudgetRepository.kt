package com.psy.domain.repository

import com.psy.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(ledgerId: Long): Flow<List<Budget>>
    suspend fun setBudget(ledgerId: Long, categoryId: Long?, amountMinor: Long)
    suspend fun removeBudget(budget: Budget)
}
