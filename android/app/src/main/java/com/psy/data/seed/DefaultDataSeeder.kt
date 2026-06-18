package com.psy.data.seed

import com.psy.domain.model.*
import com.psy.domain.repository.*
import javax.inject.Inject

class DefaultDataSeeder @Inject constructor(
    private val ledgerRepo: LedgerRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
) {
    suspend fun seedIfEmpty(now: Long) {
        if (ledgerRepo.firstOrNull() == null) {
            ledgerRepo.upsert(Ledger(name = "Sổ của tôi", icon = "wallet", currency = "VND", createdAt = now))
        }
        if (accountRepo.count() == 0) {
            accountRepo.upsert(Account(name = "Tiền mặt", type = AccountType.CASH, icon = "💵", color = 0xFF22C55E))
            accountRepo.upsert(Account(name = "Ngân hàng", type = AccountType.BANK, icon = "🏦", color = 0xFF7FD8FF))
        }
        if (categoryRepo.count() == 0) {
            val expense = listOf("Ăn uống" to "🍜", "Di chuyển" to "🚌", "Mua sắm" to "🛍️", "Hoá đơn" to "🧾", "Giải trí" to "🎮", "Sức khoẻ" to "💊", "Khác" to "📦")
            val income = listOf("Lương" to "💰", "Thưởng" to "🎁", "Khác" to "📦")
            expense.forEachIndexed { i, (n, ic) -> categoryRepo.upsert(Category(name = n, icon = ic, color = 0xFFFF8FD6, type = CategoryType.EXPENSE, sortOrder = i)) }
            income.forEachIndexed { i, (n, ic) -> categoryRepo.upsert(Category(name = n, icon = ic, color = 0xFF7FD8FF, type = CategoryType.INCOME, sortOrder = i)) }
        }
    }
}
