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
    public let category: Category?
    public let limitMinor: Int64
    public let spentMinor: Int64
    public let percent: Double
}

public struct BudgetResult: Sendable {
    public let total: TotalBudget?
    public let categoryBudgets: [CategoryBudgetItem]
    public let availableCategories: [Category]
}

public enum BudgetEngine {
    public static func build(monthTransactions: [Transaction], budgets: [Budget], categories: [Category]) -> BudgetResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let expenseTxns = monthTransactions.filter { $0.type == .expense }
        let totalSpent = expenseTxns.reduce(Int64(0)) { $0 + $1.amountMinor }

        let totalBudget = budgets.first { $0.categoryId == nil }
        let total = totalBudget.map { b -> TotalBudget in
            let pct = b.amountMinor > 0 ? Double(totalSpent) / Double(b.amountMinor) : 0
            return TotalBudget(budget: b, limitMinor: b.amountMinor, spentMinor: totalSpent, percent: pct)
        }

        let categoryBudgets = budgets.filter { $0.categoryId != nil }.compactMap { b -> CategoryBudgetItem in
            let catSpent = expenseTxns.filter { $0.categoryId == b.categoryId }.reduce(Int64(0)) { $0 + $1.amountMinor }
            let pct = b.amountMinor > 0 ? Double(catSpent) / Double(b.amountMinor) : 0
            return CategoryBudgetItem(budget: b, category: catMap[b.categoryId!], limitMinor: b.amountMinor, spentMinor: catSpent, percent: pct)
        }.sorted { $0.percent > $1.percent }

        let budgetedIds = Set(budgets.compactMap { $0.categoryId })
        let available = categories.filter { $0.type == .expense && !budgetedIds.contains($0.id) }

        return BudgetResult(total: total, categoryBudgets: categoryBudgets, availableCategories: available)
    }
}
