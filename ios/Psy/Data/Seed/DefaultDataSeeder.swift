import Foundation
import PsyCore

@MainActor
final class DefaultDataSeeder {
    private let ledgerRepo: LedgerRepository
    private let accountRepo: AccountRepository
    private let categoryGroupRepo: CategoryGroupRepository
    private let categoryRepo: CategoryRepository
    init(ledgerRepo: LedgerRepository, accountRepo: AccountRepository,
         categoryGroupRepo: CategoryGroupRepository, categoryRepo: CategoryRepository) {
        self.ledgerRepo = ledgerRepo; self.accountRepo = accountRepo
        self.categoryGroupRepo = categoryGroupRepo; self.categoryRepo = categoryRepo
    }

    private struct Seed { let name: String; let icon: String; let type: CategoryType; let leaves: [(String, String)] }

    func seedIfEmpty(now: Int64) {
        if ledgerRepo.firstOrNull() == nil {
            ledgerRepo.upsert(Ledger(name: "Sổ của tôi", icon: "wallet", currency: "VND", createdAt: now))
        }
        if accountRepo.count() == 0 {
            accountRepo.upsert(Account(name: "Tiền mặt", type: .cash, icon: "wallet", color: 0xFF1F9D62))
            accountRepo.upsert(Account(name: "Ngân hàng", type: .bank, icon: "landmark", color: 0xFF0A7CF6))
        }
        if categoryGroupRepo.count() == 0 {
            // HostGuardIQ palette (blue/amber/teal/green/red family)
            let palette: [Int64] = [
                0xFF0A7CF6, 0xFFF59E0B, 0xFF0BB3B0, 0xFF1F9D62, 0xFFE0413A,
                0xFF3D97F8, 0xFFFBB43D, 0xFF19E3E0,
            ]
            let seeds: [Seed] = [
                Seed(name: "Ăn uống", icon: "utensils", type: .expense, leaves: [("Đi chợ", "shopping-cart"), ("Nhà hàng", "utensils"), ("Khác", "utensils")]),
                Seed(name: "Cà phê", icon: "coffee", type: .expense, leaves: [("Cà phê", "coffee"), ("Trà sữa", "cup-soda"), ("Khác", "cup-soda")]),
                Seed(name: "Vận tải", icon: "bus", type: .expense, leaves: [("Grab", "bike"), ("Xăng", "fuel"), ("Giữ xe", "square-parking"), ("Metro", "train-front"), ("Khác", "car")]),
                Seed(name: "Mua sắm", icon: "shopping-bag", type: .expense, leaves: [("Quần áo", "shirt"), ("Đồ dùng", "package"), ("Khác", "package")]),
                Seed(name: "Hoá đơn", icon: "receipt", type: .expense, leaves: [("Điện nước", "lightbulb"), ("Internet", "globe"), ("Khác", "receipt")]),
                Seed(name: "Giải trí", icon: "gamepad-2", type: .expense, leaves: [("Khác", "gamepad-2")]),
                Seed(name: "Lương", icon: "banknote", type: .income, leaves: [("Khác", "banknote")]),
                Seed(name: "Thưởng", icon: "gift", type: .income, leaves: [("Khác", "gift")]),
            ]
            for (gi, s) in seeds.enumerated() {
                let gid = categoryGroupRepo.upsert(
                    CategoryGroup(name: s.name, icon: s.icon, color: palette[gi % palette.count], type: s.type, sortOrder: gi))
                for (li, leaf) in s.leaves.enumerated() {
                    categoryRepo.upsert(PsyCore.Category(groupId: gid, name: leaf.0, icon: leaf.1, sortOrder: li))
                }
            }
        }
    }
}
