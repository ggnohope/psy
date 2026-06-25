import Foundation
import Combine
import PsyCore

/// Ports ManageCategoriesViewModel.kt. A type tab (expense/income) drives observeByType;
/// editor draft state + save/delete. Canonical @MainActor @Observable pattern.
@MainActor @Observable
final class ManageCategoriesViewModel {
    private let container: AppContainer
    private var cancellables = Set<AnyCancellable>()
    private let typeSubject = CurrentValueSubject<CategoryType, Never>(.expense)

    var type: CategoryType = .expense
    // Top-level groups for the selected type. (Leaf editing UI lands with the re-skin.)
    var items: [CategoryGroup] = []

    // Editor draft state
    var editingId: Int64?
    var draftName: String = ""
    var draftIcon: String = "package"
    var draftColor: Int64 = 0xFF0A7CF6

    init(container: AppContainer) {
        self.container = container
        start()
    }

    private func start() {
        let c = container
        typeSubject
            .map { type -> AnyPublisher<[CategoryGroup], Never> in
                c.categoryGroupRepo.observeByType(type)
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] groups in self?.items = groups }
            .store(in: &cancellables)
    }

    // MARK: - Tab

    func selectTab(_ newType: CategoryType) {
        type = newType
        typeSubject.value = newType
    }

    // MARK: - Editor

    func startAdd() {
        editingId = nil
        draftName = ""
        draftIcon = "package"
        draftColor = 0xFF0A7CF6
    }

    func startEdit(_ group: CategoryGroup) {
        editingId = group.id
        draftName = group.name
        draftIcon = group.icon
        draftColor = group.color
    }

    func save() {
        let trimmed = draftName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let isNew = editingId == nil
        let maxSortOrder = items.map(\.sortOrder).max() ?? -1
        let sortOrder = isNew ? maxSortOrder + 1
            : (items.first(where: { $0.id == editingId })?.sortOrder ?? (maxSortOrder + 1))
        let group = CategoryGroup(
            id: editingId ?? 0,
            name: trimmed,
            icon: draftIcon,
            color: draftColor,
            type: type,
            sortOrder: sortOrder
        )
        container.categoryGroupRepo.upsert(group)
    }

    func delete(_ group: CategoryGroup) {
        container.categoryGroupRepo.delete(group)
    }
}
