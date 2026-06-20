import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class TransactionRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    /// Half-open range [start, end): callers pass end = start of the next period.
    func observeBetween(ledgerId: Int64, start: Int64, end: Int64) -> AnyPublisher<[Transaction], Never> {
        bus.subject(.transactions).map { [context] _ in
            let d = FetchDescriptor<TransactionEntity>(
                predicate: #Predicate { $0.ledgerId == ledgerId && $0.date >= start && $0.date < end },
                sortBy: [SortDescriptor(\.date, order: .reverse), SortDescriptor(\.id, order: .reverse)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func getById(_ id: Int64) -> Transaction? { fetchOne(id)?.toDomain() }

    @discardableResult
    func upsert(_ tx: Transaction) -> Int64 {
        let id = tx.id != 0 ? tx.id
            : ids.nextId(TransactionEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(tx) }
        else { context.insert(TransactionEntity(from: tx, id: id)) }
        try? context.save()
        bus.notify(.transactions)
        return id
    }

    func delete(_ tx: Transaction) {
        if let existing = fetchOne(tx.id) { context.delete(existing); try? context.save(); bus.notify(.transactions) }
    }

    private func fetchOne(_ id: Int64) -> TransactionEntity? {
        var d = FetchDescriptor<TransactionEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
