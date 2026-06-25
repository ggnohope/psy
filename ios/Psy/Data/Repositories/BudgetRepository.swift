import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class BudgetRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll(ledgerId: Int64) -> AnyPublisher<[Budget], Never> {
        bus.subject(.budgets).map { [context] _ in
            let d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.ledgerId == ledgerId })
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    /// Upserts the single total (groupId == nil) or per-group budget, reusing the
    /// existing row's id so there is at most one budget per (ledger, group|total).
    func setBudget(ledgerId: Int64, groupId: Int64?, amountMinor: Int64) {
        let existing = findExisting(ledgerId: ledgerId, groupId: groupId)
        let id = existing?.id ?? ids.nextId(BudgetEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing { existing.amountMinor = amountMinor }
        else { context.insert(BudgetEntity(id: id, ledgerId: ledgerId, groupId: groupId, amountMinor: amountMinor)) }
        try? context.save()
        bus.notify(.budgets)
    }

    func removeBudget(_ budget: Budget) {
        let targetId = budget.id
        var d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.id == targetId })
        d.fetchLimit = 1
        if let existing = (try? context.fetch(d))?.first {
            context.delete(existing); try? context.save(); bus.notify(.budgets)
        }
    }

    private func findExisting(ledgerId: Int64, groupId: Int64?) -> BudgetEntity? {
        var d: FetchDescriptor<BudgetEntity>
        if let groupId {
            d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.ledgerId == ledgerId && $0.groupId == groupId })
        } else {
            d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.ledgerId == ledgerId && $0.groupId == nil })
        }
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
