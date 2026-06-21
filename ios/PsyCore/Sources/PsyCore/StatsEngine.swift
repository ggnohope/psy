import Foundation

public struct StatsSummary: Sendable {
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
    public let avgPerDayMinor: Int64

    public init(incomeMinor: Int64, expenseMinor: Int64, netMinor: Int64, avgPerDayMinor: Int64) {
        self.incomeMinor = incomeMinor
        self.expenseMinor = expenseMinor
        self.netMinor = netMinor
        self.avgPerDayMinor = avgPerDayMinor
    }
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
    public let top: [TopGroup]
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

    public static func build(windowTransactions: [Transaction], categories: [Category], groups: [CategoryGroup],
                             accounts: [Account], month: PsyMonth, pieMode: TxType, accountFilter: Int64?,
                             calendar: Calendar, now: Date) -> StatsResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let groupMap = Dictionary(groups.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
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

        // ── Pie slices + top groups (2-level) ──
        // Mirrors StatsViewModel: aggregate by GROUP, slice color by palette index,
        // top list = group entries with expandable leaf children.
        let pieTxns = monthTxns.filter { $0.type == pieMode && $0.categoryId != nil }

        // groupId → (leafId → [amounts]); skip txns whose leaf is gone.
        var byGroup: [Int64: [Int64: [Int64]]] = [:]
        for tx in pieTxns {
            guard let leaf = catMap[tx.categoryId!] else { continue }
            byGroup[leaf.groupId, default: [:]][leaf.id, default: []].append(tx.amountMinor)
        }

        // Group totals, sorted desc → drives both pie slices and the top list, so the
        // palette index (=> color) matches between pie and list rows. Drop groups whose
        // CategoryGroup no longer exists.
        struct GroupAgg { let groupId: Int64; let amount: Int64; let leaves: [Int64: [Int64]] }
        let sortedGroups: [GroupAgg] = byGroup.compactMap { gid, leafMap in
            guard groupMap[gid] != nil else { return nil }
            let amount = leafMap.values.reduce(Int64(0)) { $0 + $1.reduce(0, +) }
            return GroupAgg(groupId: gid, amount: amount, leaves: leafMap)
        }.sorted { $0.amount > $1.amount }

        let pieTotal = sortedGroups.reduce(Int64(0)) { $0 + $1.amount }

        let slices = sortedGroups.enumerated().compactMap { i, agg -> PieSlice? in
            guard let group = groupMap[agg.groupId] else { return nil }
            return PieSlice(name: group.name, amountMinor: agg.amount, color: piePalette[i % piePalette.count])
        }

        let top: [TopGroup] = sortedGroups.enumerated().compactMap { i, agg in
            guard let group = groupMap[agg.groupId] else { return nil }
            let color = piePalette[i % piePalette.count]
            let groupAmount = agg.amount
            let groupCount = agg.leaves.values.reduce(0) { $0 + $1.count }
            let children: [TopLeaf] = agg.leaves.compactMap { leafId, amounts in
                guard let leaf = catMap[leafId] else { return nil }
                let leafAmount = amounts.reduce(0, +)
                return TopLeaf(
                    name: leaf.name, icon: leaf.icon, amountMinor: leafAmount,
                    percentInGroup: groupAmount > 0 ? Double(leafAmount) / Double(groupAmount) : 0,
                    count: amounts.count)
            }.sorted { $0.amountMinor > $1.amountMinor }
            return TopGroup(
                groupId: agg.groupId, name: group.name, icon: group.icon, color: color,
                amountMinor: groupAmount,
                percentOfTotal: pieTotal > 0 ? Double(groupAmount) / Double(pieTotal) : 0,
                count: groupCount, children: children)
        }

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
