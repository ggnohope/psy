package com.psy.data.seed

import com.psy.domain.model.*
import com.psy.domain.repository.*
import javax.inject.Inject

class DefaultDataSeeder @Inject constructor(
    private val ledgerRepo: LedgerRepository,
    private val accountRepo: AccountRepository,
    private val categoryGroupRepo: CategoryGroupRepository,
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
        if (categoryGroupRepo.count() == 0) {
            val palette = listOf(
                0xFFFF8FD6L, 0xFFA18CFFL, 0xFF7FD8FFL, 0xFFFFB86BL, 0xFF6BCB77L,
                0xFFFF6B6BL, 0xFFB088F9L, 0xFF4D96FFL, 0xFFFF5FA2L, 0xFF22C55EL,
            )
            data class Seed(val name: String, val icon: String, val type: CategoryType, val leaves: List<Pair<String, String>>)
            val seeds = listOf(
                Seed("Ăn uống", "🍜", CategoryType.EXPENSE, listOf("Đi chợ" to "🛒", "Nhà hàng" to "🍽️", "Khác" to "🍴")),
                Seed("Cà phê", "☕", CategoryType.EXPENSE, listOf("Cà phê" to "☕", "Trà sữa" to "🧋", "Khác" to "🥤")),
                Seed("Vận tải", "🚌", CategoryType.EXPENSE, listOf("Grab" to "🛵", "Xăng" to "⛽", "Giữ xe" to "🅿️", "Metro" to "🚇", "Khác" to "🚗")),
                Seed("Mua sắm", "🛍️", CategoryType.EXPENSE, listOf("Quần áo" to "👕", "Đồ dùng" to "🧴", "Khác" to "📦")),
                Seed("Hoá đơn", "🧾", CategoryType.EXPENSE, listOf("Điện nước" to "💡", "Internet" to "🌐", "Khác" to "🧾")),
                Seed("Giải trí", "🎮", CategoryType.EXPENSE, listOf("Khác" to "🎮")),
                Seed("Lương", "💰", CategoryType.INCOME, listOf("Khác" to "💰")),
                Seed("Thưởng", "🎁", CategoryType.INCOME, listOf("Khác" to "🎁")),
            )
            seeds.forEachIndexed { gi, s ->
                val gid = categoryGroupRepo.upsert(
                    CategoryGroup(name = s.name, icon = s.icon, color = palette[gi % palette.size], type = s.type, sortOrder = gi)
                )
                s.leaves.forEachIndexed { li, (ln, lic) ->
                    categoryRepo.upsert(Category(groupId = gid, name = ln, icon = lic, sortOrder = li))
                }
            }
        }
    }
}
