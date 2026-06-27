import Foundation
import Combine
import PsyCore

/// Ports ManageAccountsViewModel.kt. Subscribes to accountRepo.observeAll() and exposes
/// editor draft state + save/delete. Canonical @MainActor @Observable pattern.
@MainActor @Observable
final class ManageAccountsViewModel {
    private let container: AppContainer
    private var cancellables = Set<AnyCancellable>()

    var items: [Account] = []

    // Editor draft state
    var editingId: Int64?
    var draftName: String = ""
    var draftType: AccountType = .cash
    var draftIcon: String = "wallet"
    var draftColor: Int64 = 0xFF1F9D62
    var draftIsFund: Bool = false

    init(container: AppContainer) {
        self.container = container
        start()
    }

    private func start() {
        container.accountRepo.observeAll()
            .receive(on: RunLoop.main)
            .sink { [weak self] accounts in self?.items = accounts }
            .store(in: &cancellables)
    }

    // MARK: - Editor

    func startAdd() {
        editingId = nil
        draftName = ""
        draftType = .cash
        draftIcon = "wallet"
        draftColor = 0xFF1F9D62
        draftIsFund = false
    }

    func startEdit(_ account: Account) {
        editingId = account.id
        draftName = account.name
        draftType = account.type
        draftIcon = account.icon
        draftColor = account.color
        draftIsFund = account.isFund
    }

    func save() {
        let trimmed = draftName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let account = Account(
            id: editingId ?? 0,
            name: trimmed,
            type: draftType,
            icon: draftIcon,
            color: draftColor,
            isFund: draftIsFund
        )
        container.accountRepo.upsert(account)
    }
    // NOTE: accounts have no delete — mirrors Android (AccountRepository exposes no delete;
    // ManageAccountsScreen.kt has no delete UI for accounts).
}
