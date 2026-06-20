import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class LedgerRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[Ledger], Never> {
        bus.subject(.ledgers).map { [context] _ in
            let d = FetchDescriptor<LedgerEntity>(sortBy: [SortDescriptor(\.createdAt, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func firstOrNull() -> Ledger? {
        var d = FetchDescriptor<LedgerEntity>(sortBy: [SortDescriptor(\.createdAt, order: .forward)])
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first?.toDomain()
    }

    @discardableResult
    func upsert(_ ledger: Ledger) -> Int64 {
        let id = ledger.id != 0 ? ledger.id
            : ids.nextId(LedgerEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(ledger) }
        else { context.insert(LedgerEntity(from: ledger, id: id)) }
        try? context.save()
        bus.notify(.ledgers)
        return id
    }

    private func fetchOne(_ id: Int64) -> LedgerEntity? {
        var d = FetchDescriptor<LedgerEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
