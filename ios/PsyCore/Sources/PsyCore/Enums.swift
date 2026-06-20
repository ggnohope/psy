// Domain enums. Raw values MUST match Kotlin enum .name for snapshot JSON compatibility.
public enum TxType: String, Codable, Sendable, CaseIterable {
    case income = "INCOME"
    case expense = "EXPENSE"
    case transfer = "TRANSFER"
}

public enum AccountType: String, Codable, Sendable, CaseIterable {
    case cash = "CASH"
    case bank = "BANK"
    case credit = "CREDIT"
    case asset = "ASSET"
}

public enum CategoryType: String, Codable, Sendable, CaseIterable {
    case income = "INCOME"
    case expense = "EXPENSE"
}
