import Foundation
import SwiftData

enum ModelContainerFactory {
    static let schema = Schema([
        LedgerEntity.self, AccountEntity.self, CategoryEntity.self,
        TransactionEntity.self, BudgetEntity.self,
    ])

    static func make(inMemory: Bool = false) -> ModelContainer {
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: inMemory)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
    }
}
