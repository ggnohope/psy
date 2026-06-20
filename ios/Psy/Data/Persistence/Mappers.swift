import Foundation
import PsyCore

extension LedgerEntity {
    func toDomain() -> Ledger { Ledger(id: id, name: name, icon: icon, currency: currency, createdAt: createdAt) }
    func apply(_ d: Ledger) { name = d.name; icon = d.icon; currency = d.currency; createdAt = d.createdAt }
    convenience init(from d: Ledger, id: Int64) {
        self.init(id: id, name: d.name, icon: d.icon, currency: d.currency, createdAt: d.createdAt)
    }
}

extension AccountEntity {
    func toDomain() -> Account {
        Account(id: id, name: name, type: AccountType(rawValue: type) ?? .cash, icon: icon, color: color)
    }
    func apply(_ d: Account) { name = d.name; type = d.type.rawValue; icon = d.icon; color = d.color }
    convenience init(from d: Account, id: Int64) {
        self.init(id: id, name: d.name, type: d.type.rawValue, icon: d.icon, color: d.color)
    }
}

extension CategoryEntity {
    func toDomain() -> PsyCore.Category {
        PsyCore.Category(id: id, name: name, icon: icon, color: color,
                 type: CategoryType(rawValue: type) ?? .expense, sortOrder: sortOrder)
    }
    func apply(_ d: PsyCore.Category) { name = d.name; icon = d.icon; color = d.color; type = d.type.rawValue; sortOrder = d.sortOrder }
    convenience init(from d: PsyCore.Category, id: Int64) {
        self.init(id: id, name: d.name, icon: d.icon, color: d.color, type: d.type.rawValue, sortOrder: d.sortOrder)
    }
}

extension TransactionEntity {
    func toDomain() -> Transaction {
        Transaction(id: id, ledgerId: ledgerId, type: TxType(rawValue: type) ?? .expense,
                    amountMinor: amountMinor, categoryId: categoryId, accountId: accountId,
                    toAccountId: toAccountId, note: note, date: date, createdAt: createdAt,
                    updatedAt: updatedAt, photoUri: photoUri)
    }
    func apply(_ d: Transaction) {
        ledgerId = d.ledgerId; type = d.type.rawValue; amountMinor = d.amountMinor
        categoryId = d.categoryId; accountId = d.accountId; toAccountId = d.toAccountId
        note = d.note; date = d.date; createdAt = d.createdAt; updatedAt = d.updatedAt; photoUri = d.photoUri
    }
    convenience init(from d: Transaction, id: Int64) {
        self.init(id: id, ledgerId: d.ledgerId, type: d.type.rawValue, amountMinor: d.amountMinor,
                  categoryId: d.categoryId, accountId: d.accountId, toAccountId: d.toAccountId,
                  note: d.note, date: d.date, createdAt: d.createdAt, updatedAt: d.updatedAt, photoUri: d.photoUri)
    }
}

extension BudgetEntity {
    func toDomain() -> Budget { Budget(id: id, ledgerId: ledgerId, categoryId: categoryId, amountMinor: amountMinor) }
    func apply(_ d: Budget) { ledgerId = d.ledgerId; categoryId = d.categoryId; amountMinor = d.amountMinor }
    convenience init(from d: Budget, id: Int64) {
        self.init(id: id, ledgerId: d.ledgerId, categoryId: d.categoryId, amountMinor: d.amountMinor)
    }
}
