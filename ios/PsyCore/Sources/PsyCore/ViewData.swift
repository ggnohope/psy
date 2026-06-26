import Foundation

/// A single transaction row for Home/Calendar lists. For TRANSFER, `categoryIcon` is "🔄",
/// `title` is the source account name and `toAccountName` is the destination (mirrors the VMs).
public struct TxRow: Identifiable, Hashable, Sendable {
    public let id: Int64
    public let title: String          // leaf category name, or source account for transfer
    public let icon: String
    /// Parent group name of the leaf category; empty for TRANSFER. Mirrors Android.
    public let groupName: String
    /// Transaction time-of-day formatted as HH:mm. Mirrors Android.
    public let timeLabel: String
    public let accountName: String
    public let isFund: Bool
    public let toAccountName: String?
    public let type: TxType
    public let amountMinor: Int64
    public let note: String
    public let photoUri: String?
    public init(id: Int64, title: String, icon: String, groupName: String = "", timeLabel: String = "",
                accountName: String, isFund: Bool = false, toAccountName: String?,
                type: TxType, amountMinor: Int64, note: String, photoUri: String?) {
        self.id = id; self.title = title; self.icon = icon
        self.groupName = groupName; self.timeLabel = timeLabel
        self.accountName = accountName; self.isFund = isFund; self.toAccountName = toAccountName
        self.type = type; self.amountMinor = amountMinor
        self.note = note; self.photoUri = photoUri
    }
}

public struct DayGroup: Identifiable, Hashable, Sendable {
    public var id: String { dateLabel }
    public let dateLabel: String
    public let items: [TxRow]
    public init(dateLabel: String, items: [TxRow]) { self.dateLabel = dateLabel; self.items = items }
}

public struct PieSlice: Identifiable, Hashable, Sendable {
    public var id: String { name }
    public let name: String
    public let amountMinor: Int64
    public let color: Int64
    public init(name: String, amountMinor: Int64, color: Int64) { self.name = name; self.amountMinor = amountMinor; self.color = color }
}

/// Leaf row inside a `TopGroup`. `percentInGroup` = leafAmount / groupAmount. Mirrors Android.
public struct TopLeaf: Identifiable, Hashable, Sendable {
    public var id: String { name }
    public let name: String
    public let icon: String
    public let amountMinor: Int64
    public let percentInGroup: Double
    public let count: Int
    public init(name: String, icon: String, amountMinor: Int64, percentInGroup: Double, count: Int) {
        self.name = name; self.icon = icon; self.amountMinor = amountMinor
        self.percentInGroup = percentInGroup; self.count = count
    }
}

/// Top-list group entry, expandable to leaf children. `percentOfTotal` = groupAmount / pieTotal.
/// `color` comes from the fixed pie palette by index (NOT group.color). Mirrors Android.
public struct TopGroup: Identifiable, Hashable, Sendable {
    public var id: Int64 { groupId }
    public let groupId: Int64
    public let name: String
    public let icon: String
    public let color: Int64
    public let amountMinor: Int64
    public let percentOfTotal: Double
    public let count: Int
    public let children: [TopLeaf]
    public init(groupId: Int64, name: String, icon: String, color: Int64, amountMinor: Int64,
                percentOfTotal: Double, count: Int, children: [TopLeaf]) {
        self.groupId = groupId; self.name = name; self.icon = icon; self.color = color
        self.amountMinor = amountMinor; self.percentOfTotal = percentOfTotal
        self.count = count; self.children = children
    }
}

public struct MonthBars: Identifiable, Hashable, Sendable {
    public var id: String { label }
    public let label: String
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public init(label: String, incomeMinor: Int64, expenseMinor: Int64) { self.label = label; self.incomeMinor = incomeMinor; self.expenseMinor = expenseMinor }
}

/// Helper for building TxRow from a Transaction + lookup maps (shared by Home/Calendar engines).
/// `groups` maps leaf parent-group ids → CategoryGroup so each row can surface the group name.
public enum TxRowBuilder {
    /// HH:mm of the tx timestamp (full timestamp, not midnight). Mirrors Android `timeFormatter`.
    static func timeLabel(_ tx: Transaction, calendar: Calendar) -> String {
        let date = Date(timeIntervalSince1970: Double(tx.date) / 1000)
        let comps = calendar.dateComponents([.hour, .minute], from: date)
        return String(format: "%02d:%02d", comps.hour ?? 0, comps.minute ?? 0)
    }

    public static func make(_ tx: Transaction, categories: [Int64: Category], groups: [Int64: CategoryGroup],
                            accounts: [Int64: Account], calendar: Calendar) -> TxRow {
        let acc = accounts[tx.accountId]
        let time = timeLabel(tx, calendar: calendar)
        if tx.type == .transfer {
            let toAcc = tx.toAccountId.flatMap { accounts[$0] }
            return TxRow(id: tx.id, title: acc?.name ?? "—", icon: "🔄", groupName: "", timeLabel: time,
                         accountName: acc?.name ?? "—", toAccountName: toAcc?.name ?? "—",
                         type: tx.type, amountMinor: tx.amountMinor, note: tx.note, photoUri: tx.photoUri)
        } else {
            let leaf = tx.categoryId.flatMap { categories[$0] }
            let group = leaf.flatMap { groups[$0.groupId] }
            return TxRow(id: tx.id, title: leaf?.name ?? "—", icon: leaf?.icon ?? "📦",
                         groupName: group?.name ?? "", timeLabel: time,
                         accountName: acc?.name ?? "—", isFund: acc?.isFund ?? false, toAccountName: nil,
                         type: tx.type, amountMinor: tx.amountMinor, note: tx.note, photoUri: tx.photoUri)
        }
    }
}
