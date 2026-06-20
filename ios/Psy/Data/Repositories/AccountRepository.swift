import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class AccountRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[Account], Never> {
        bus.subject(.accounts).map { [context] _ in
            let d = FetchDescriptor<AccountEntity>(sortBy: [SortDescriptor(\.id, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func count() -> Int { (try? context.fetchCount(FetchDescriptor<AccountEntity>())) ?? 0 }

    @discardableResult
    func upsert(_ account: Account) -> Int64 {
        let id = account.id != 0 ? account.id
            : ids.nextId(AccountEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(account) }
        else { context.insert(AccountEntity(from: account, id: id)) }
        try? context.save()
        bus.notify(.accounts)
        return id
    }

    private func fetchOne(_ id: Int64) -> AccountEntity? {
        var d = FetchDescriptor<AccountEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
