// Domain enums. Raw values MUST match Kotlin enum .name for snapshot JSON compatibility.
public enum TxType: String, Codable, Sendable, CaseIterable {
    case income = "INCOME"
    case expense = "EXPENSE"
    case transfer = "TRANSFER"
}
