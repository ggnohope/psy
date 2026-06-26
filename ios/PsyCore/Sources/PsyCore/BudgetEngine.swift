import Foundation

public struct TotalBudget: Sendable {
    public let budget: Budget
    public let limitMinor: Int64
    public let spentMinor: Int64
    public let percent: Double
}

public struct CategoryBudgetItem: Identifiable, Sendable {
    public var id: Int64 { budget.id }
    public let budget: Budget
    /// The CategoryGroup this budget targets (budgets reference groups, not leaves).
    public let group: CategoryGroup?
    public let limitMinor: Int64
    public let spentMinor: Int64
    public let percent: Double
}

public struct BudgetResult: Sendable {
    public let total: TotalBudget?
    public let categoryBudgets: [CategoryBudgetItem]
    /// EXPENSE groups not yet budgeted (for the picker). Mirrors Android `availableGroups`.
    public let availableGroups: [CategoryGroup]
}

public enum BudgetEngine {
    /// Spent for a per-group budget = sum of EXPENSE tx whose leaf's groupId == budget.groupId.
    /// `groups` should already be the EXPENSE groups (Android queries observeByType(EXPENSE)).
    public static func build(monthTransactions: [Transaction], budgets: [Budget],
                             categories: [Category], groups: [CategoryGroup], accounts: [Account] = []) -> BudgetResult {
        let groupMap = Dictionary(groups.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        // leafId → groupId so each EXPENSE tx can be attributed to its parent group.
        let leafToGroup = Dictionary(categories.map { ($0.id, $0.groupId) }, uniquingKeysWith: { a, _ in a })
        let fundIds = Set(accounts.filter { $0.isFund }.map { $0.id })

        // Spent excludes fund-account txns (consistent with Stats "Ngân sách x").
        let expenseTxns = monthTransactions.filter { $0.type == .expense && !fundIds.contains($0.accountId) }
        let totalSpent = expenseTxns.reduce(Int64(0)) { $0 + $1.amountMinor }

        let totalBudget = budgets.first { $0.groupId == nil }
        let total = totalBudget.map { b -> TotalBudget in
            let pct = b.amountMinor > 0 ? Double(totalSpent) / Double(b.amountMinor) : 0
            return TotalBudget(budget: b, limitMinor: b.amountMinor, spentMinor: totalSpent, percent: pct)
        }

        let categoryBudgets = budgets.filter { $0.groupId != nil }.map { b -> CategoryBudgetItem in
            let groupSpent = expenseTxns
                .filter { $0.categoryId.flatMap { leafToGroup[$0] } == b.groupId }
                .reduce(Int64(0)) { $0 + $1.amountMinor }
            let pct = b.amountMinor > 0 ? Double(groupSpent) / Double(b.amountMinor) : 0
            return CategoryBudgetItem(budget: b, group: groupMap[b.groupId!], limitMinor: b.amountMinor,
                                      spentMinor: groupSpent, percent: pct)
        }.sorted { $0.percent > $1.percent }

        let budgetedGroupIds = Set(budgets.compactMap { $0.groupId })
        let availableGroups = groups.filter { !budgetedGroupIds.contains($0.id) }

        return BudgetResult(total: total, categoryBudgets: categoryBudgets, availableGroups: availableGroups)
    }
}
