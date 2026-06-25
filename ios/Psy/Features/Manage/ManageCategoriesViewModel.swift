import Foundation
import Combine
import PsyCore

/// A parent group bundled with its leaf categories. Mirrors Android `GroupWithLeaves`.
struct GroupWithLeaves: Identifiable {
    var id: Int64 { group.id }
    let group: CategoryGroup
    let leaves: [PsyCore.Category]
}

/// 2-level manage-categories VM (mirrors ManageCategoriesViewModel.kt): a type tab drives
/// the group list (each with its leaves); separate group + leaf editor draft state.
@MainActor @Observable
final class ManageCategoriesViewModel {
    private let container: AppContainer
    private var cancellables = Set<AnyCancellable>()
    private let typeSubject = CurrentValueSubject<CategoryType, Never>(.expense)

    var type: CategoryType = .expense
    var groups: [GroupWithLeaves] = []

    // Group editor
    var groupEditorOpen = false
    var editingGroupId: Int64?
    var groupDraftName = ""
    var groupDraftIcon = "package"
    var groupDraftColor: Int64 = 0xFF0A7CF6

    // Leaf editor
    var leafEditorOpen = false
    var editingLeafId: Int64?
    var leafParentGroupId: Int64?
    var leafDraftName = ""
    var leafDraftIcon = "package"

    var canSaveGroup: Bool { !groupDraftName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    var canSaveLeaf: Bool { !leafDraftName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && (editingLeafId != nil || leafParentGroupId != nil) }

    init(container: AppContainer) {
        self.container = container
        start()
    }

    private func start() {
        let c = container
        typeSubject
            .map { type -> AnyPublisher<[GroupWithLeaves], Never> in
                c.categoryGroupRepo.observeByType(type)
                    .combineLatest(c.categoryRepo.observeAll())
                    .map { groups, leaves in
                        groups.map { g in
                            GroupWithLeaves(group: g, leaves: leaves.filter { $0.groupId == g.id })
                        }
                    }
                    .eraseToAnyPublisher()
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] gwl in self?.groups = gwl }
            .store(in: &cancellables)
    }

    // MARK: - Tab

    func selectTab(_ newType: CategoryType) {
        type = newType
        typeSubject.value = newType
    }

    // MARK: - Group editor

    func startAddGroup() {
        editingGroupId = nil; groupDraftName = ""; groupDraftIcon = "package"; groupDraftColor = 0xFF0A7CF6
        groupEditorOpen = true
    }

    func startEditGroup(_ group: CategoryGroup) {
        editingGroupId = group.id; groupDraftName = group.name; groupDraftIcon = group.icon; groupDraftColor = group.color
        groupEditorOpen = true
    }

    func saveGroup() {
        let trimmed = groupDraftName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let isNew = editingGroupId == nil
        let maxSort = groups.map(\.group.sortOrder).max() ?? -1
        let sortOrder = isNew ? maxSort + 1 : (groups.first { $0.id == editingGroupId }?.group.sortOrder ?? maxSort + 1)
        container.categoryGroupRepo.upsert(CategoryGroup(
            id: editingGroupId ?? 0, name: trimmed, icon: groupDraftIcon, color: groupDraftColor,
            type: type, sortOrder: sortOrder))
        groupEditorOpen = false
    }

    func deleteGroup(_ group: CategoryGroup) {
        // Delete leaves first, then the group.
        container.categoryRepo.byGroup(group.id).forEach { container.categoryRepo.delete($0) }
        container.categoryGroupRepo.delete(group)
    }

    // MARK: - Leaf editor

    func startAddLeaf(groupId: Int64) {
        editingLeafId = nil; leafParentGroupId = groupId; leafDraftName = ""; leafDraftIcon = "package"
        leafEditorOpen = true
    }

    func startEditLeaf(_ leaf: PsyCore.Category) {
        editingLeafId = leaf.id; leafParentGroupId = leaf.groupId; leafDraftName = leaf.name; leafDraftIcon = leaf.icon
        leafEditorOpen = true
    }

    func saveLeaf() {
        let trimmed = leafDraftName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let groupId = leafParentGroupId else { return }
        let siblings = container.categoryRepo.byGroup(groupId)
        let isNew = editingLeafId == nil
        let maxSort = siblings.map(\.sortOrder).max() ?? -1
        let sortOrder = isNew ? maxSort + 1 : (siblings.first { $0.id == editingLeafId }?.sortOrder ?? maxSort + 1)
        container.categoryRepo.upsert(PsyCore.Category(
            id: editingLeafId ?? 0, groupId: groupId, name: trimmed, icon: leafDraftIcon, sortOrder: sortOrder))
        leafEditorOpen = false
    }

    func deleteLeaf(_ leaf: PsyCore.Category) {
        container.categoryRepo.delete(leaf)
    }
}
