public struct Ledger: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var currency: String
    public var createdAt: Int64
    public init(id: Int64 = 0, name: String, icon: String, currency: String, createdAt: Int64) {
        self.id = id; self.name = name; self.icon = icon
        self.currency = currency; self.createdAt = createdAt
    }
}

public struct Account: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var type: AccountType
    public var icon: String
    public var color: Int64 // ARGB packed
    public init(id: Int64 = 0, name: String, type: AccountType, icon: String, color: Int64) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color
    }
}

/// PARENT level of the 2-level category hierarchy. Carries the color + type
/// that leaves derive from. Mirrors Android `CategoryGroup`.
public struct CategoryGroup: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var color: Int64 // ARGB packed
    public var type: CategoryType
    public var sortOrder: Int
    public init(id: Int64 = 0, name: String, icon: String, color: Int64, type: CategoryType, sortOrder: Int) {
        self.id = id; self.name = name; self.icon = icon
        self.color = color; self.type = type; self.sortOrder = sortOrder
    }
}

/// LEAF level. Color + type are derived from its parent group (`groupId`).
/// Mirrors Android `Category` (color/type dropped in the hierarchy migration).
public struct Category: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var groupId: Int64
    public var name: String
    public var icon: String
    public var sortOrder: Int
    public init(id: Int64 = 0, groupId: Int64, name: String, icon: String, sortOrder: Int) {
        self.id = id; self.groupId = groupId; self.name = name
        self.icon = icon; self.sortOrder = sortOrder
    }
}

public struct Transaction: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var type: TxType
    public var amountMinor: Int64
    public var categoryId: Int64?
    public var accountId: Int64
    public var toAccountId: Int64?
    public var note: String
    public var date: Int64        // epoch millis of the bill's day
    public var createdAt: Int64
    public var updatedAt: Int64
    public var photoUri: String?
    public init(id: Int64 = 0, ledgerId: Int64, type: TxType, amountMinor: Int64,
                categoryId: Int64? = nil, accountId: Int64, toAccountId: Int64? = nil,
                note: String, date: Int64, createdAt: Int64, updatedAt: Int64, photoUri: String? = nil) {
        self.id = id; self.ledgerId = ledgerId; self.type = type; self.amountMinor = amountMinor
        self.categoryId = categoryId; self.accountId = accountId; self.toAccountId = toAccountId
        self.note = note; self.date = date; self.createdAt = createdAt
        self.updatedAt = updatedAt; self.photoUri = photoUri
    }
}

public struct Budget: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    /// References a `CategoryGroup` (nil = total/uncategorised budget). Mirrors Android.
    public var groupId: Int64?
    public var amountMinor: Int64
    public init(id: Int64 = 0, ledgerId: Int64, groupId: Int64?, amountMinor: Int64) {
        self.id = id; self.ledgerId = ledgerId; self.groupId = groupId; self.amountMinor = amountMinor
    }
}
