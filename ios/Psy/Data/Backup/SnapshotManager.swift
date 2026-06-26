import Foundation
import SwiftData
import PsyCore

@MainActor
final class SnapshotManager {
    private let context: ModelContext
    private let bus: DataChangeBus
    init(context: ModelContext, bus: DataChangeBus) {
        self.context = context; self.bus = bus
    }

    func isLocalEmpty() -> Bool {
        ((try? context.fetchCount(FetchDescriptor<LedgerEntity>())) ?? 0) == 0
    }

    func export() throws -> String {
        let snapshot = SnapshotDTO(
            ledgers: fetchAll(LedgerEntity.self).map { LedgerDTO(id: $0.id, name: $0.name, icon: $0.icon, currency: $0.currency, createdAt: $0.createdAt) },
            accounts: fetchAll(AccountEntity.self).map { AccountDTO(id: $0.id, name: $0.name, type: $0.type, icon: $0.icon, color: $0.color, isFund: $0.isFund) },
            categoryGroups: fetchAll(CategoryGroupEntity.self).map { CategoryGroupDTO(id: $0.id, name: $0.name, icon: $0.icon, color: $0.color, type: $0.type, sortOrder: $0.sortOrder) },
            categories: fetchAll(CategoryEntity.self).map { CategoryDTO(id: $0.id, groupId: $0.groupId, name: $0.name, icon: $0.icon, sortOrder: $0.sortOrder) },
            transactions: fetchAll(TransactionEntity.self).map { TransactionDTO(id: $0.id, ledgerId: $0.ledgerId, type: $0.type, amountMinor: $0.amountMinor, categoryId: $0.categoryId, accountId: $0.accountId, toAccountId: $0.toAccountId, note: $0.note, date: $0.date, createdAt: $0.createdAt, updatedAt: $0.updatedAt, photoUri: $0.photoUri) },
            budgets: fetchAll(BudgetEntity.self).map { BudgetDTO(id: $0.id, ledgerId: $0.ledgerId, groupId: $0.groupId, amountMinor: $0.amountMinor) }
        )
        let data = try JSONEncoder().encode(snapshot)
        return String(decoding: data, as: UTF8.self)
    }

    func importBlob(_ jsonStr: String) throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(jsonStr.utf8))
        deleteAllRows()
        for l in dto.ledgers { context.insert(LedgerEntity(id: l.id, name: l.name, icon: l.icon, currency: l.currency, createdAt: l.createdAt)) }
        for a in dto.accounts { context.insert(AccountEntity(id: a.id, name: a.name, type: a.type, icon: a.icon, color: a.color, isFund: a.isFund)) }
        for g in dto.categoryGroups { context.insert(CategoryGroupEntity(id: g.id, name: g.name, icon: g.icon, color: g.color, type: g.type, sortOrder: g.sortOrder)) }
        for c in dto.categories { context.insert(CategoryEntity(id: c.id, groupId: c.groupId, name: c.name, icon: c.icon, sortOrder: c.sortOrder)) }
        for t in dto.transactions { context.insert(TransactionEntity(id: t.id, ledgerId: t.ledgerId, type: t.type, amountMinor: t.amountMinor, categoryId: t.categoryId, accountId: t.accountId, toAccountId: t.toAccountId, note: t.note, date: t.date, createdAt: t.createdAt, updatedAt: t.updatedAt, photoUri: t.photoUri)) }
        for b in dto.budgets { context.insert(BudgetEntity(id: b.id, ledgerId: b.ledgerId, groupId: b.groupId, amountMinor: b.amountMinor)) }
        try context.save()
        notifyAll()
    }

    func wipeLocal() {
        deleteAllRows()
        try? context.save()
        notifyAll()
    }

    // MARK: - helpers
    private func fetchAll<T: PersistentModel>(_ type: T.Type) -> [T] {
        (try? context.fetch(FetchDescriptor<T>())) ?? []
    }

    private func deleteAllRows() {
        // Children before parents (consistent with FK semantics, though SwiftData has no FKs here).
        try? context.delete(model: TransactionEntity.self)
        try? context.delete(model: BudgetEntity.self)
        try? context.delete(model: CategoryEntity.self)
        try? context.delete(model: CategoryGroupEntity.self)
        try? context.delete(model: AccountEntity.self)
        try? context.delete(model: LedgerEntity.self)
    }

    private func notifyAll() {
        bus.notify(.ledgers); bus.notify(.accounts); bus.notify(.categories)
        bus.notify(.transactions); bus.notify(.budgets)
    }
}
