package com.psy.data.repo

import com.psy.data.db.dao.BudgetDao
import com.psy.data.db.entity.BudgetEntity
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.Budget
import com.psy.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {
    override fun observeAll(ledgerId: Long): Flow<List<Budget>> =
        dao.observeAll(ledgerId).map { list -> list.map { it.toDomain() } }

    override suspend fun setBudget(ledgerId: Long, groupId: Long?, amountMinor: Long) {
        val existing = if (groupId == null) dao.findTotal(ledgerId) else dao.findByGroup(ledgerId, groupId)
        dao.upsert(BudgetEntity(id = existing?.id ?: 0, ledgerId, groupId, amountMinor))
    }

    override suspend fun removeBudget(budget: Budget) = dao.delete(budget.toEntity())
}
