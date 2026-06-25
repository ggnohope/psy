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
            accountRepo.upsert(Account(name = "Tiền mặt", type = AccountType.CASH, icon = "wallet", color = 0xFF1F9D62))
            accountRepo.upsert(Account(name = "Ngân hàng", type = AccountType.BANK, icon = "landmark", color = 0xFF0A7CF6))
        }
        if (categoryGroupRepo.count() == 0) {
            // HostGuardIQ palette (blue/amber/teal/green/red family)
            val palette = listOf(
                0xFF0A7CF6L, 0xFFF59E0BL, 0xFF0BB3B0L, 0xFF1F9D62L, 0xFFE0413AL,
                0xFF3D97F8L, 0xFFFBB43DL, 0xFF19E3E0L,
            )
            data class Seed(val name: String, val icon: String, val type: CategoryType, val leaves: List<Pair<String, String>>)
            val seeds = listOf(
                Seed("Ăn uống", "utensils", CategoryType.EXPENSE, listOf("Đi chợ" to "shopping-cart", "Nhà hàng" to "utensils", "Khác" to "utensils")),
                Seed("Cà phê", "coffee", CategoryType.EXPENSE, listOf("Cà phê" to "coffee", "Trà sữa" to "cup-soda", "Khác" to "cup-soda")),
                Seed("Vận tải", "bus", CategoryType.EXPENSE, listOf("Grab" to "bike", "Xăng" to "fuel", "Giữ xe" to "square-parking", "Metro" to "train-front", "Khác" to "car")),
                Seed("Mua sắm", "shopping-bag", CategoryType.EXPENSE, listOf("Quần áo" to "shirt", "Đồ dùng" to "package", "Khác" to "package")),
                Seed("Hoá đơn", "receipt", CategoryType.EXPENSE, listOf("Điện nước" to "lightbulb", "Internet" to "globe", "Khác" to "receipt")),
                Seed("Giải trí", "gamepad-2", CategoryType.EXPENSE, listOf("Khác" to "gamepad-2")),
                Seed("Lương", "banknote", CategoryType.INCOME, listOf("Khác" to "banknote")),
                Seed("Thưởng", "gift", CategoryType.INCOME, listOf("Khác" to "gift")),
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
