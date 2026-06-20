import Foundation
import Combine
import PsyCore

/// Editor mode for the budget editor sheet — mirrors `EditorMode` in BudgetViewModel.kt.
enum BudgetEditorMode { case total, category }

/// Budget screen ViewModel. Mirrors `BudgetViewModel.kt`:
/// a month subject feeds `BudgetEngine`, which produces the total budget, per-category
/// budgets and the categories still available to budget. Editor sub-state (add total /
/// add category / edit / remove) lives here as plain `@Observable` properties.
/// Canonical Combine pattern (ledgers × month → inner CombineLatest3 → engine).
@MainActor @Observable
final class BudgetViewModel {
    private let container: AppContainer
    let calendar: Calendar
    private var cancellables = Set<AnyCancellable>()

    private let monthSubject: CurrentValueSubject<PsyMonth, Never>

    // Cached ledger id so save/remove can run without waiting for the publisher.
    private var cachedLedgerId: Int64?

    // Engine-derived state.
    var monthLabel = ""
    var currency: Currency = .vnd
    var total: TotalBudget?
    var categoryBudgets: [CategoryBudgetItem] = []
    var availableCategories: [PsyCore.Category] = []
    var loading = true

    // Editor sub-state.
    var editorOpen = false
    var editorMode: BudgetEditorMode = .total
    var editorCategoryId: Int64?
    var draftAmountText = ""
    var isEditing = false
    var editingBudget: PsyCore.Budget?

    var canSave: Bool {
        let amount = Int64(draftAmountText.filter { $0.isNumber }) ?? 0
        guard amount > 0 else { return false }
        return editorMode == .total || isEditing || editorCategoryId != nil
    }

    init(container: AppContainer) {
        self.container = container
        var c = Calendar(identifier: .gregorian); c.timeZone = .current
        self.calendar = c
        let now = Date()
        self.monthSubject = CurrentValueSubject(PsyMonth.current(c, now: now))
        self.monthLabel = monthSubject.value.label
        start()
    }

    private func start() {
        let cal = calendar
        let c = container
        let monthPub = monthSubject

        c.ledgerRepo.observeAll()
            .combineLatest(monthPub)
            .map { ledgers, month -> AnyPublisher<(Int64, Currency, PsyMonth, BudgetResult)?, Never> in
                guard let ledger = ledgers.first else { return Just(nil).eraseToAnyPublisher() }
                let currency = Currency.of(ledger.currency)
                let start = month.startMillis(cal)
                let end = month.endMillis(cal)
                return Publishers.CombineLatest3(
                    c.transactionRepo.observeBetween(ledgerId: ledger.id, start: start, end: end),
                    c.budgetRepo.observeAll(ledgerId: ledger.id),
                    c.categoryRepo.observeAll()
                )
                .map { txns, budgets, cats in
                    let result = BudgetEngine.build(monthTransactions: txns, budgets: budgets, categories: cats)
                    return (ledger.id, currency, month, result)
                }
                .eraseToAnyPublisher()
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] payload in
                guard let self else { return }
                if let (ledgerId, currency, month, r) = payload {
                    self.cachedLedgerId = ledgerId
                    self.currency = currency
                    self.monthLabel = month.label
                    self.total = r.total
                    self.categoryBudgets = r.categoryBudgets
                    self.availableCategories = r.availableCategories
                } else {
                    self.total = nil
                    self.categoryBudgets = []
                    self.availableCategories = []
                }
                self.loading = false
            }
            .store(in: &cancellables)
    }

    // MARK: - Month navigation

    func prevMonth() { monthSubject.value = monthSubject.value.adding(-1, calendar) }
    func nextMonth() { monthSubject.value = monthSubject.value.adding(1, calendar) }

    // MARK: - Editor commands

    func startAddTotal() {
        editorMode = .total
        editingBudget = nil
        isEditing = false
        editorCategoryId = nil
        draftAmountText = ""
        editorOpen = true
    }

    func startAddCategory() {
        editorMode = .category
        editingBudget = nil
        isEditing = false
        editorCategoryId = nil
        draftAmountText = ""
        editorOpen = true
    }

    /// Works for both the total budget and a per-category budget.
    func startEdit(_ budget: PsyCore.Budget) {
        editingBudget = budget
        isEditing = true
        editorMode = budget.categoryId == nil ? .total : .category
        editorCategoryId = budget.categoryId
        // For VND (fractionDigits = 0) the stored amountMinor equals the typed integer.
        draftAmountText = String(budget.amountMinor)
        editorOpen = true
    }

    func onAmountChange(_ s: String) {
        draftAmountText = s.filter { $0.isNumber }
    }

    func selectEditorCategory(_ id: Int64) {
        editorCategoryId = id
    }

    func closeEditor() {
        editorOpen = false
        editingBudget = nil
        isEditing = false
        editorCategoryId = nil
        draftAmountText = ""
    }

    func save() {
        // For VND (fractionDigits = 0): typed integer IS the amount in minor units.
        let amount = Int64(draftAmountText.filter { $0.isNumber }) ?? 0
        guard amount > 0 else { return }
        let catId: Int64? = editorMode == .total ? nil : editorCategoryId
        if editorMode == .category && !isEditing && catId == nil { return }
        guard let ledgerId = cachedLedgerId else { return }
        container.budgetRepo.setBudget(ledgerId: ledgerId, categoryId: catId, amountMinor: amount)
        closeEditor()
    }

    func remove() {
        guard let toRemove = editingBudget else { return }
        container.budgetRepo.removeBudget(toRemove)
        closeEditor()
    }
}
