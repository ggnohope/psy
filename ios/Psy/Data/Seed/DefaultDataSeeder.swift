import Foundation
import PsyCore

@MainActor
final class DefaultDataSeeder {
    private let ledgerRepo: LedgerRepository
    private let accountRepo: AccountRepository
    private let categoryRepo: CategoryRepository
    init(ledgerRepo: LedgerRepository, accountRepo: AccountRepository, categoryRepo: CategoryRepository) {
        self.ledgerRepo = ledgerRepo; self.accountRepo = accountRepo; self.categoryRepo = categoryRepo
    }

    func seedIfEmpty(now: Int64) {
        if ledgerRepo.firstOrNull() == nil {
            ledgerRepo.upsert(Ledger(name: "Sổ của tôi", icon: "wallet", currency: "VND", createdAt: now))
        }
        if accountRepo.count() == 0 {
            accountRepo.upsert(Account(name: "Tiền mặt", type: .cash, icon: "💵", color: 0xFF22C55E))
            accountRepo.upsert(Account(name: "Ngân hàng", type: .bank, icon: "🏦", color: 0xFF7FD8FF))
        }
        if categoryRepo.count() == 0 {
            let palette: [Int64] = [
                0xFFFF8FD6, 0xFFA18CFF, 0xFF7FD8FF, 0xFFFFB86B, 0xFF6BCB77,
                0xFFFF6B6B, 0xFFB088F9, 0xFF4D96FF, 0xFFFF5FA2, 0xFF22C55E,
            ]
            let expense: [(String, String)] = [
                ("Ăn uống", "🍜"), ("Di chuyển", "🚌"), ("Mua sắm", "🛍️"), ("Hoá đơn", "🧾"),
                ("Giải trí", "🎮"), ("Sức khoẻ", "💊"), ("Khác", "📦"),
            ]
            let income: [(String, String)] = [("Lương", "💰"), ("Thưởng", "🎁"), ("Khác", "📦")]
            for (i, item) in expense.enumerated() {
                categoryRepo.upsert(PsyCore.Category(name: item.0, icon: item.1, color: palette[i % palette.count], type: .expense, sortOrder: i))
            }
            for (i, item) in income.enumerated() {
                categoryRepo.upsert(PsyCore.Category(name: item.0, icon: item.1, color: palette[i % palette.count], type: .income, sortOrder: i))
            }
        }
    }
}
