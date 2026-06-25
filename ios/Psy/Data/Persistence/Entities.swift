import Foundation
import SwiftData

@Model
final class LedgerEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var icon: String
    var currency: String
    var createdAt: Int64
    init(id: Int64, name: String, icon: String, currency: String, createdAt: Int64) {
        self.id = id; self.name = name; self.icon = icon; self.currency = currency; self.createdAt = createdAt
    }
}

@Model
final class AccountEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var type: String
    var icon: String
    var color: Int64
    init(id: Int64, name: String, type: String, icon: String, color: Int64) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color
    }
}

/// PARENT level of the 2-level hierarchy (carries color + type). Mirrors Android CategoryGroup.
@Model
final class CategoryGroupEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var icon: String
    var color: Int64
    var type: String
    var sortOrder: Int
    init(id: Int64, name: String, icon: String, color: Int64, type: String, sortOrder: Int) {
        self.id = id; self.name = name; self.icon = icon; self.color = color; self.type = type; self.sortOrder = sortOrder
    }
}

/// LEAF level. Color + type derive from its parent group (`groupId`).
@Model
final class CategoryEntity {
    @Attribute(.unique) var id: Int64
    var groupId: Int64
    var name: String
    var icon: String
    var sortOrder: Int
    init(id: Int64, groupId: Int64, name: String, icon: String, sortOrder: Int) {
        self.id = id; self.groupId = groupId; self.name = name; self.icon = icon; self.sortOrder = sortOrder
    }
}

@Model
final class TransactionEntity {
    @Attribute(.unique) var id: Int64
    var ledgerId: Int64
    var type: String
    var amountMinor: Int64
    var categoryId: Int64?
    var accountId: Int64
    var toAccountId: Int64?
    var note: String
    var date: Int64
    var createdAt: Int64
    var updatedAt: Int64
    var photoUri: String?
    init(id: Int64, ledgerId: Int64, type: String, amountMinor: Int64, categoryId: Int64?,
         accountId: Int64, toAccountId: Int64?, note: String, date: Int64,
         createdAt: Int64, updatedAt: Int64, photoUri: String?) {
        self.id = id; self.ledgerId = ledgerId; self.type = type; self.amountMinor = amountMinor
        self.categoryId = categoryId; self.accountId = accountId; self.toAccountId = toAccountId
        self.note = note; self.date = date; self.createdAt = createdAt; self.updatedAt = updatedAt; self.photoUri = photoUri
    }
}

@Model
final class BudgetEntity {
    @Attribute(.unique) var id: Int64
    var ledgerId: Int64
    /// References a CategoryGroup (nil = total budget). Mirrors Android.
    var groupId: Int64?
    var amountMinor: Int64
    init(id: Int64, ledgerId: Int64, groupId: Int64?, amountMinor: Int64) {
        self.id = id; self.ledgerId = ledgerId; self.groupId = groupId; self.amountMinor = amountMinor
    }
}
