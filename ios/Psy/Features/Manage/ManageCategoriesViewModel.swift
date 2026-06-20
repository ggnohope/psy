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
    var items: [PsyCore.Category] = []

    // Editor draft state
    var editingId: Int64?
    var draftName: String = ""
    var draftIcon: String = "📦"
    var draftColor: Int64 = 0xFFA18CFF

    init(container: AppContainer) {
        self.container = container
        start()
    }

    private func start() {
        let c = container
        typeSubject
            .map { type -> AnyPublisher<[PsyCore.Category], Never> in
                c.categoryRepo.observeByType(type)
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] cats in self?.items = cats }
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
        draftIcon = "📦"
        draftColor = 0xFFA18CFF
    }

    func startEdit(_ category: PsyCore.Category) {
        editingId = category.id
        draftName = category.name
        draftIcon = category.icon
        draftColor = category.color
    }

    func save() {
        let trimmed = draftName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let isNew = editingId == nil
        let maxSortOrder = items.map(\.sortOrder).max() ?? -1
        let sortOrder: Int
        if isNew {
            sortOrder = maxSortOrder + 1
        } else {
            sortOrder = items.first(where: { $0.id == editingId })?.sortOrder ?? (maxSortOrder + 1)
        }
        let category = PsyCore.Category(
            id: editingId ?? 0,
            name: trimmed,
            icon: draftIcon,
            color: draftColor,
            type: type,
            sortOrder: sortOrder
        )
        container.categoryRepo.upsert(category)
    }

    func delete(_ category: PsyCore.Category) {
        container.categoryRepo.delete(category)
    }
}
