import Foundation
import SwiftData

/// Composition root (Hilt-graph analog). Owns the SwiftData container and wires every
/// repository/manager against the shared main ModelContext + DataChangeBus.
@MainActor
final class AppContainer {
    let modelContainer: ModelContainer
    let context: ModelContext
    let bus: DataChangeBus
    let ids: IdAllocator

    let ledgerRepo: LedgerRepository
    let accountRepo: AccountRepository
    let categoryRepo: CategoryRepository
    let transactionRepo: TransactionRepository
    let budgetRepo: BudgetRepository

    let snapshotManager: SnapshotManager
    let seeder: DefaultDataSeeder

    let tokenStore: TokenStore
    let settingsStore: SettingsStore
    let authRepo: AuthRepository
    let backupRepo: BackupRepository

    init(inMemory: Bool = false) {
        modelContainer = ModelContainerFactory.make(inMemory: inMemory)
        context = modelContainer.mainContext
        bus = DataChangeBus()
        ids = IdAllocator(context: context)

        ledgerRepo = LedgerRepository(context: context, bus: bus, ids: ids)
        accountRepo = AccountRepository(context: context, bus: bus, ids: ids)
        categoryRepo = CategoryRepository(context: context, bus: bus, ids: ids)
        transactionRepo = TransactionRepository(context: context, bus: bus, ids: ids)
        budgetRepo = BudgetRepository(context: context, bus: bus, ids: ids)

        snapshotManager = SnapshotManager(context: context, bus: bus)
        seeder = DefaultDataSeeder(ledgerRepo: ledgerRepo, accountRepo: accountRepo, categoryRepo: categoryRepo)

        tokenStore = TokenStore()
        settingsStore = SettingsStore()
        let apiClient = APIClient(baseURLString: BuildConfig.baseURL,
                                  tokenProvider: { [tokenStore] in tokenStore.currentToken })
        authRepo = AuthRepository(api: AuthAPI(client: apiClient), tokenStore: tokenStore)
        backupRepo = BackupRepository(api: BackupAPI(client: apiClient), snapshot: snapshotManager,
                                      tokenStore: tokenStore, seeder: seeder)
    }
}
