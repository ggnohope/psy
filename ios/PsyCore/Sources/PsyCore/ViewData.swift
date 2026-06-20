import Foundation

/// A single transaction row for Home/Calendar lists. For TRANSFER, `categoryIcon` is "🔄",
/// `title` is the source account name and `toAccountName` is the destination (mirrors the VMs).
public struct TxRow: Identifiable, Hashable, Sendable {
    public let id: Int64
    public let title: String          // category name, or source account for transfer
    public let icon: String
    public let accountName: String
    public let toAccountName: String?
    public let type: TxType
    public let amountMinor: Int64
    public let note: String
    public let photoUri: String?
    public init(id: Int64, title: String, icon: String, accountName: String, toAccountName: String?,
                type: TxType, amountMinor: Int64, note: String, photoUri: String?) {
        self.id = id; self.title = title; self.icon = icon; self.accountName = accountName
        self.toAccountName = toAccountName; self.type = type; self.amountMinor = amountMinor
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

public struct MonthBars: Identifiable, Hashable, Sendable {
    public var id: String { label }
    public let label: String
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public init(label: String, incomeMinor: Int64, expenseMinor: Int64) { self.label = label; self.incomeMinor = incomeMinor; self.expenseMinor = expenseMinor }
}

/// Helper for building TxRow from a Transaction + lookup maps (shared by Home/Calendar engines).
public enum TxRowBuilder {
    public static func make(_ tx: Transaction, categories: [Int64: Category], accounts: [Int64: Account]) -> TxRow {
        let acc = accounts[tx.accountId]
        if tx.type == .transfer {
            let toAcc = tx.toAccountId.flatMap { accounts[$0] }
            return TxRow(id: tx.id, title: acc?.name ?? "—", icon: "🔄", accountName: acc?.name ?? "—",
                         toAccountName: toAcc?.name ?? "—", type: tx.type, amountMinor: tx.amountMinor,
                         note: tx.note, photoUri: tx.photoUri)
        } else {
            let cat = tx.categoryId.flatMap { categories[$0] }
            return TxRow(id: tx.id, title: cat?.name ?? "—", icon: cat?.icon ?? "📦", accountName: acc?.name ?? "—",
                         toAccountName: nil, type: tx.type, amountMinor: tx.amountMinor,
                         note: tx.note, photoUri: tx.photoUri)
        }
    }
}
