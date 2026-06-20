import Foundation
import SwiftData

/// Allocates monotonically increasing Int64 ids per entity table, mirroring Room's
/// autoincrement. Single-user UI app → all access is on the main actor, no races.
@MainActor
final class IdAllocator {
    private let context: ModelContext
    init(context: ModelContext) { self.context = context }

    func nextId<T: PersistentModel>(_ type: T.Type, idKeyPath: KeyPath<T, Int64>,
                                    sortDescending: SortDescriptor<T>) -> Int64 {
        var d = FetchDescriptor<T>(sortBy: [sortDescending])
        d.fetchLimit = 1
        guard let last: T = (try? context.fetch(d))?.first else { return 1 }
        return last[keyPath: idKeyPath] + 1
    }
}
