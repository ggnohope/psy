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
            // Distinct color per category so chart slices and list dots are visually distinguishable.
            val palette = listOf(
                0xFFFF8FD6L, 0xFFA18CFFL, 0xFF7FD8FFL, 0xFFFFB86BL, 0xFF6BCB77L,
                0xFFFF6B6BL, 0xFFB088F9L, 0xFF4D96FFL, 0xFFFF5FA2L, 0xFF22C55EL,
            )
            val expense = listOf("Ăn uống" to "🍜", "Di chuyển" to "🚌", "Mua sắm" to "🛍️", "Hoá đơn" to "🧾", "Giải trí" to "🎮", "Sức khoẻ" to "💊", "Khác" to "📦")
            val income = listOf("Lương" to "💰", "Thưởng" to "🎁", "Khác" to "📦")
            expense.forEachIndexed { i, (n, ic) -> categoryRepo.upsert(Category(name = n, icon = ic, color = palette[i % palette.size], type = CategoryType.EXPENSE, sortOrder = i)) }
            income.forEachIndexed { i, (n, ic) -> categoryRepo.upsert(Category(name = n, icon = ic, color = palette[i % palette.size], type = CategoryType.INCOME, sortOrder = i)) }
        }
    }
}
