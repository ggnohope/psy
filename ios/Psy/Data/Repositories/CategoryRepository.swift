import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class CategoryRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[PsyCore.Category], Never> {
        bus.subject(.categories).map { [context] _ in
            let d = FetchDescriptor<CategoryEntity>(sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func observeByType(_ type: CategoryType) -> AnyPublisher<[PsyCore.Category], Never> {
        let raw = type.rawValue
        return bus.subject(.categories).map { [context] _ in
            let d = FetchDescriptor<CategoryEntity>(
                predicate: #Predicate { $0.type == raw },
                sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func byType(_ type: CategoryType) -> [PsyCore.Category] {
        let raw = type.rawValue
        let d = FetchDescriptor<CategoryEntity>(
            predicate: #Predicate { $0.type == raw },
            sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }

    func all() -> [PsyCore.Category] {
        let d = FetchDescriptor<CategoryEntity>(sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }

    func count() -> Int { (try? context.fetchCount(FetchDescriptor<CategoryEntity>())) ?? 0 }

    @discardableResult
    func upsert(_ category: PsyCore.Category) -> Int64 {
        let id = category.id != 0 ? category.id
            : ids.nextId(CategoryEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(category) }
        else { context.insert(CategoryEntity(from: category, id: id)) }
        try? context.save()
        bus.notify(.categories)
        return id
    }

    func delete(_ category: PsyCore.Category) {
        if let existing = fetchOne(category.id) { context.delete(existing); try? context.save(); bus.notify(.categories) }
    }

    private func fetchOne(_ id: Int64) -> CategoryEntity? {
        var d = FetchDescriptor<CategoryEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
