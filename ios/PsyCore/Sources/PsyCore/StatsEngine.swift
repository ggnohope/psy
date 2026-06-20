import Foundation

public struct StatsSummary: Sendable {
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
    public let avgPerDayMinor: Int64
}

public struct TopEntry: Identifiable, Sendable {
    public var id: Int64 { category.id }
    public let category: Category
    public let amountMinor: Int64
    public let percent: Double
}

public struct AccountStat: Identifiable, Sendable {
    public let id: Int64
    public let name: String
    public let icon: String
    public let color: Int64
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
}

public struct StatsResult: Sendable {
    public let summary: StatsSummary
    public let pieMode: TxType
    public let slices: [PieSlice]
    public let top: [TopEntry]
    public let trend: [MonthBars]
    public let accountBreakdown: [AccountStat]
    public let selectedAccountId: Int64?
}

public enum StatsEngine {
    /// Fixed palette: slice colors by index so the chart is always readable (mirrors StatsViewModel.piePalette).
    public static let piePalette: [Int64] = [
        0xFFA18CFF, 0xFF7FD8FF, 0xFFFF8FD6, 0xFFFF5FA2, 0xFF22C55E,
        0xFFFFB86B, 0xFF6BCB77, 0xFF4D96FF, 0xFFFF6B6B, 0xFFB088F9,
    ]

    public static func build(windowTransactions: [Transaction], categories: [Category], accounts: [Account],
                             month: PsyMonth, pieMode: TxType, accountFilter: Int64?,
                             calendar: Calendar, now: Date) -> StatsResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let accMap = Dictionary(accounts.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })

        let monthStart = month.startMillis(calendar)
        let monthEnd = month.endMillis(calendar)

        // ── Account breakdown (ALL accounts, before filtering) ──
        let monthTxnsAll = windowTransactions.filter { $0.date >= monthStart && $0.date < monthEnd }
        var byAccount: [Int64: (inc: Int64, exp: Int64)] = [:]
        for tx in monthTxnsAll {
            switch tx.type {
            case .income: byAccount[tx.accountId, default: (0, 0)].inc += tx.amountMinor
            case .expense: byAccount[tx.accountId, default: (0, 0)].exp += tx.amountMinor
            case .transfer: break
            }
        }
        let accountBreakdown: [AccountStat] = byAccount.compactMap { id, sums in
            guard let acc = accMap[id] else { return nil }
            return AccountStat(id: acc.id, name: acc.name, icon: acc.icon, color: acc.color,
                               incomeMinor: sums.inc, expenseMinor: sums.exp, netMinor: sums.inc - sums.exp)
        }.sorted { ($0.incomeMinor + $0.expenseMinor) > ($1.incomeMinor + $1.expenseMinor) }

        // Drop filter if the account no longer exists.
        let effectiveFilter = accountFilter.flatMap { accMap[$0] != nil ? $0 : nil }
        let filteredWindow = effectiveFilter == nil ? windowTransactions : windowTransactions.filter { $0.accountId == effectiveFilter }
        let monthTxns = filteredWindow.filter { $0.date >= monthStart && $0.date < monthEnd }

        // ── Summary ──
        var income: Int64 = 0, expense: Int64 = 0
        for tx in monthTxns {
            switch tx.type {
            case .income: income += tx.amountMinor
            case .expense: expense += tx.amountMinor
            case .transfer: break
            }
        }
        let currentYM = PsyMonth.current(calendar, now: now)
        let todayDay = calendar.component(.day, from: now)
        let daysToCount = (month == currentYM) ? todayDay : month.lengthOfMonth(calendar)
        let avgPerDay = expense / Int64(max(1, daysToCount))

        // ── Pie slices (by index palette) ──
        let pieTxns = monthTxns.filter { $0.type == pieMode && $0.categoryId != nil }
        var pieByCategory: [Int64: Int64] = [:]
        for tx in pieTxns { pieByCategory[tx.categoryId!, default: 0] += tx.amountMinor }

        let sortedPie = pieByCategory.compactMap { catId, amount -> (name: String, amount: Int64)? in
            guard let cat = catMap[catId] else { return nil }
            return (cat.name, amount)
        }.sorted { $0.amount > $1.amount }
        let slices = sortedPie.enumerated().map { i, e in
            PieSlice(name: e.name, amountMinor: e.amount, color: piePalette[i % piePalette.count])
        }

        // ── Top entries ──
        let pieTotal = slices.reduce(Int64(0)) { $0 + $1.amountMinor }
        let top = pieByCategory.compactMap { catId, amount -> TopEntry? in
            guard let cat = catMap[catId] else { return nil }
            let pct = pieTotal > 0 ? Double(amount) / Double(pieTotal) : 0
            return TopEntry(category: cat, amountMinor: amount, percent: pct)
        }.sorted { $0.amountMinor > $1.amountMinor }.prefix(10).map { $0 }

        // ── Trend (6 months) ──
        let trend: [MonthBars] = (0...5).reversed().map { offset in
            let ym = month.adding(-offset, calendar)
            let s = ym.startMillis(calendar), e = ym.endMillis(calendar)
            var inc: Int64 = 0, exp: Int64 = 0
            for tx in filteredWindow where tx.date >= s && tx.date < e {
                switch tx.type {
                case .income: inc += tx.amountMinor
                case .expense: exp += tx.amountMinor
                case .transfer: break
                }
            }
            return MonthBars(label: ym.shortLabel, incomeMinor: inc, expenseMinor: exp)
        }

        return StatsResult(
            summary: StatsSummary(incomeMinor: income, expenseMinor: expense, netMinor: income - expense, avgPerDayMinor: avgPerDay),
            pieMode: pieMode, slices: slices, top: top, trend: trend,
            accountBreakdown: accountBreakdown, selectedAccountId: effectiveFilter)
    }
}
