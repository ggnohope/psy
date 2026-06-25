import Foundation
import Combine
import SwiftData
import PsyCore

/// PARENT-level category repository (2-level hierarchy). Mirrors Android CategoryGroupRepository.
@MainActor
final class CategoryGroupRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[CategoryGroup], Never> {
        bus.subject(.categories).map { [context] _ in
            let d = FetchDescriptor<CategoryGroupEntity>(sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func observeByType(_ type: CategoryType) -> AnyPublisher<[CategoryGroup], Never> {
        let raw = type.rawValue
        return bus.subject(.categories).map { [context] _ in
            let d = FetchDescriptor<CategoryGroupEntity>(
                predicate: #Predicate { $0.type == raw },
                sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func byType(_ type: CategoryType) -> [CategoryGroup] {
        let raw = type.rawValue
        let d = FetchDescriptor<CategoryGroupEntity>(
            predicate: #Predicate { $0.type == raw },
            sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }

    func all() -> [CategoryGroup] {
        let d = FetchDescriptor<CategoryGroupEntity>(sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }

    func count() -> Int { (try? context.fetchCount(FetchDescriptor<CategoryGroupEntity>())) ?? 0 }

    @discardableResult
    func upsert(_ group: CategoryGroup) -> Int64 {
        let id = group.id != 0 ? group.id
            : ids.nextId(CategoryGroupEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(group) }
        else { context.insert(CategoryGroupEntity(from: group, id: id)) }
        try? context.save()
        bus.notify(.categories)
        return id
    }

    func delete(_ group: CategoryGroup) {
        if let existing = fetchOne(group.id) { context.delete(existing); try? context.save(); bus.notify(.categories) }
    }

    private func fetchOne(_ id: Int64) -> CategoryGroupEntity? {
        var d = FetchDescriptor<CategoryGroupEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
