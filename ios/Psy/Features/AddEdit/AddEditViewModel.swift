import Foundation
import SwiftUI
import PsyCore

@MainActor @Observable
final class AddEditViewModel {
    private let container: AppContainer
    private let txId: Int64          // 0 = new
    private var originalCreatedAt: Int64 = 0
    private var ledgerId: Int64 = 0

    var isEdit: Bool { txId != 0 }
    var type: TxType = .expense
    var amountText: String = ""
    // 2-level category hierarchy (mirrors Android AddEdit).
    var groups: [CategoryGroup] = []        // parent groups for the current type
    var leaves: [PsyCore.Category] = []     // leaves of the selected group
    var selectedGroupId: Int64?
    var accounts: [Account] = []
    var selectedCategoryId: Int64?

    /// The currently selected parent group + leaf, for the breadcrumb.
    var selectedGroup: CategoryGroup? { groups.first { $0.id == selectedGroupId } }
    var selectedLeaf: PsyCore.Category? { leaves.first { $0.id == selectedCategoryId } }
    var selectedAccountId: Int64?
    var toAccountId: Int64?
    var date: Date = Date()
    var note: String = ""
    var currency: Currency = .vnd
    var photoUri: String?
    var photoErrorMessage: String?

    var canSave: Bool {
        AddEditLogic.canSave(amountText: amountText, type: type, categoryId: selectedCategoryId,
                             accountId: selectedAccountId, toAccountId: toAccountId)
    }

    init(container: AppContainer, txId: Int64) {
        self.container = container
        self.txId = txId
        load()
    }

    private func load() {
        let ledger = container.ledgerRepo.firstOrNull()
        ledgerId = ledger?.id ?? 0
        currency = ledger.map { Currency.of($0.currency) } ?? .vnd
        accounts = container.accountRepo.all()
        selectedAccountId = accounts.first?.id

        if isEdit, let tx = container.transactionRepo.getById(txId) {
            type = tx.type
            amountText = AddEditLogic.typedString(amountMinor: tx.amountMinor, fractionDigits: currency.fractionDigits)
            if tx.type == .transfer { selectedAccountId = tx.accountId; toAccountId = tx.toAccountId; selectedCategoryId = nil }
            else { selectedCategoryId = tx.categoryId; selectedAccountId = tx.accountId }
            date = Date(timeIntervalSince1970: Double(tx.date) / 1000)
            note = tx.note
            photoUri = tx.photoUri
            originalCreatedAt = tx.createdAt
        }
        reloadGroups(preserveSelection: isEdit)
    }

    /// Loads parent groups for the current type, picks a group, then loads its leaves.
    func reloadGroups(preserveSelection: Bool) {
        guard type != .transfer else { groups = []; leaves = []; selectedGroupId = nil; selectedCategoryId = nil; return }
        let target: CategoryType = (type == .income) ? .income : .expense
        groups = container.categoryGroupRepo.byType(target)
        if preserveSelection, let cid = selectedCategoryId,
           let leaf = container.categoryRepo.all().first(where: { $0.id == cid }) {
            selectedGroupId = leaf.groupId
        } else {
            selectedGroupId = groups.first?.id
        }
        loadLeaves(autoSelectFirst: !(preserveSelection && selectedCategoryId != nil))
    }

    private func loadLeaves(autoSelectFirst: Bool) {
        guard let gid = selectedGroupId else { leaves = []; selectedCategoryId = nil; return }
        leaves = container.categoryRepo.byGroup(gid)
        if autoSelectFirst || !leaves.contains(where: { $0.id == selectedCategoryId }) {
            selectedCategoryId = leaves.first?.id
        }
    }

    /// Picking a parent group auto-selects its first leaf (mirrors Android).
    func selectGroup(_ id: Int64) {
        selectedGroupId = id
        leaves = container.categoryRepo.byGroup(id)
        selectedCategoryId = leaves.first?.id
    }

    func selectCategory(_ id: Int64) { selectedCategoryId = id }

    func onTypeChange(_ newType: TxType) {
        type = newType
        if newType == .transfer { groups = []; leaves = []; selectedGroupId = nil; selectedCategoryId = nil }
        else { toAccountId = nil; reloadGroups(preserveSelection: false) }
    }

    func attachPhoto(data: Data) {
        photoErrorMessage = nil
        do {
            let name = "img_\(Int(Date().timeIntervalSince1970 * 1000))"
            photoUri = try PhotoStorage.save(data: data, name: name)
        } catch { photoErrorMessage = "Không thể đính kèm ảnh: \(error.localizedDescription)" }
    }

    func removePhoto() {
        if let p = photoUri { PhotoStorage.delete(path: p) }
        photoUri = nil
    }

    func save(onDone: () -> Void) {
        guard canSave else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let amountMinor = AddEditLogic.amountMinor(typed: amountText, fractionDigits: currency.fractionDigits)
        let dateMillis = Int64(date.timeIntervalSince1970 * 1000)
        let tx: PsyCore.Transaction
        if type == .transfer {
            tx = PsyCore.Transaction(id: txId, ledgerId: ledgerId, type: .transfer, amountMinor: amountMinor,
                             categoryId: nil, accountId: selectedAccountId!, toAccountId: toAccountId,
                             note: note, date: dateMillis, createdAt: isEdit ? originalCreatedAt : now, updatedAt: now, photoUri: photoUri)
        } else {
            tx = PsyCore.Transaction(id: txId, ledgerId: ledgerId, type: type, amountMinor: amountMinor,
                             categoryId: selectedCategoryId, accountId: selectedAccountId!, toAccountId: nil,
                             note: note, date: dateMillis, createdAt: isEdit ? originalCreatedAt : now, updatedAt: now, photoUri: photoUri)
        }
        container.transactionRepo.upsert(tx)
        onDone()
    }

    func delete(onDone: () -> Void) {
        guard isEdit, let tx = container.transactionRepo.getById(txId) else { return }
        container.transactionRepo.delete(tx)
        onDone()
    }
}
