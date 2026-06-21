import Foundation

/// Top-level backup snapshot. Field names + null handling must match the Android
/// kotlinx.serialization blob exactly so the shared backend blob round-trips both ways.
public struct SnapshotDTO: Codable, Equatable, Sendable {
    public var version: Int
    public var ledgers: [LedgerDTO]
    public var accounts: [AccountDTO]
    public var categoryGroups: [CategoryGroupDTO]
    public var categories: [CategoryDTO]
    public var transactions: [TransactionDTO]
    public var budgets: [BudgetDTO]

    public init(version: Int = 2, ledgers: [LedgerDTO], accounts: [AccountDTO],
                categoryGroups: [CategoryGroupDTO] = [], categories: [CategoryDTO],
                transactions: [TransactionDTO], budgets: [BudgetDTO]) {
        self.version = version; self.ledgers = ledgers; self.accounts = accounts
        self.categoryGroups = categoryGroups; self.categories = categories
        self.transactions = transactions; self.budgets = budgets
    }

    enum CodingKeys: String, CodingKey {
        case version, ledgers, accounts, categoryGroups, categories, transactions, budgets
    }

    // categoryGroups defaults to [] when decoding a legacy v1 blob that lacks the key.
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        version = try c.decode(Int.self, forKey: .version)
        ledgers = try c.decode([LedgerDTO].self, forKey: .ledgers)
        accounts = try c.decode([AccountDTO].self, forKey: .accounts)
        categoryGroups = try c.decodeIfPresent([CategoryGroupDTO].self, forKey: .categoryGroups) ?? []
        categories = try c.decode([CategoryDTO].self, forKey: .categories)
        transactions = try c.decode([TransactionDTO].self, forKey: .transactions)
        budgets = try c.decode([BudgetDTO].self, forKey: .budgets)
    }
}

public struct LedgerDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var currency: String
    public var createdAt: Int64
    public init(id: Int64, name: String, icon: String, currency: String, createdAt: Int64) {
        self.id = id; self.name = name; self.icon = icon; self.currency = currency; self.createdAt = createdAt
    }
}

public struct AccountDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var type: String
    public var icon: String
    public var color: Int64
    public init(id: Int64, name: String, type: String, icon: String, color: Int64) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color
    }
}

public struct CategoryGroupDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var color: Int64
    public var type: String
    public var sortOrder: Int
    public init(id: Int64, name: String, icon: String, color: Int64, type: String, sortOrder: Int) {
        self.id = id; self.name = name; self.icon = icon; self.color = color; self.type = type; self.sortOrder = sortOrder
    }
}

/// LEAF DTO. Reshaped for the hierarchy migration: carries `groupId`, drops `color`/`type`.
public struct CategoryDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var groupId: Int64
    public var name: String
    public var icon: String
    public var sortOrder: Int
    public init(id: Int64, groupId: Int64, name: String, icon: String, sortOrder: Int) {
        self.id = id; self.groupId = groupId; self.name = name; self.icon = icon; self.sortOrder = sortOrder
    }
}

/// Has optionals → custom encode emits explicit `null` to match kotlinx output.
public struct TransactionDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var type: String
    public var amountMinor: Int64
    public var categoryId: Int64?
    public var accountId: Int64
    public var toAccountId: Int64?
    public var note: String
    public var date: Int64
    public var createdAt: Int64
    public var updatedAt: Int64
    public var photoUri: String?

    public init(id: Int64, ledgerId: Int64, type: String, amountMinor: Int64, categoryId: Int64?,
                accountId: Int64, toAccountId: Int64?, note: String, date: Int64,
                createdAt: Int64, updatedAt: Int64, photoUri: String?) {
        self.id = id; self.ledgerId = ledgerId; self.type = type; self.amountMinor = amountMinor
        self.categoryId = categoryId; self.accountId = accountId; self.toAccountId = toAccountId
        self.note = note; self.date = date; self.createdAt = createdAt; self.updatedAt = updatedAt; self.photoUri = photoUri
    }

    enum CodingKeys: String, CodingKey {
        case id, ledgerId, type, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(ledgerId, forKey: .ledgerId)
        try c.encode(type, forKey: .type)
        try c.encode(amountMinor, forKey: .amountMinor)
        try c.encode(categoryId, forKey: .categoryId)        // explicit null when nil
        try c.encode(accountId, forKey: .accountId)
        try c.encode(toAccountId, forKey: .toAccountId)      // explicit null when nil
        try c.encode(note, forKey: .note)
        try c.encode(date, forKey: .date)
        try c.encode(createdAt, forKey: .createdAt)
        try c.encode(updatedAt, forKey: .updatedAt)
        try c.encode(photoUri, forKey: .photoUri)            // explicit null when nil
    }
}

/// Has optional groupId → custom encode emits explicit `null` to match kotlinx.
public struct BudgetDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var groupId: Int64?
    public var amountMinor: Int64
    public init(id: Int64, ledgerId: Int64, groupId: Int64?, amountMinor: Int64) {
        self.id = id; self.ledgerId = ledgerId; self.groupId = groupId; self.amountMinor = amountMinor
    }

    enum CodingKeys: String, CodingKey { case id, ledgerId, groupId, amountMinor }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(ledgerId, forKey: .ledgerId)
        try c.encode(groupId, forKey: .groupId)              // explicit null when nil
        try c.encode(amountMinor, forKey: .amountMinor)
    }
}
