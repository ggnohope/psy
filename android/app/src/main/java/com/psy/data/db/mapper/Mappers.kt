package com.psy.data.db.mapper

import com.psy.data.db.entity.*
import com.psy.domain.model.*

fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), icon, color)
fun Account.toEntity() = AccountEntity(id, name, type.name, icon, color)

fun CategoryGroupEntity.toDomain() = CategoryGroup(id, name, icon, color, CategoryType.valueOf(type), sortOrder)
fun CategoryGroup.toEntity() = CategoryGroupEntity(id, name, icon, color, type.name, sortOrder)

fun CategoryEntity.toDomain() = Category(id, groupId, name, icon, sortOrder)
fun Category.toEntity() = CategoryEntity(id, groupId, name, icon, sortOrder)

fun LedgerEntity.toDomain() = Ledger(id, name, icon, currency, createdAt)
fun Ledger.toEntity() = LedgerEntity(id, name, icon, currency, createdAt)

fun TransactionEntity.toDomain() = Transaction(id, ledgerId, TxType.valueOf(type), amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri)
fun Transaction.toEntity() = TransactionEntity(id, ledgerId, type.name, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri)

fun BudgetEntity.toDomain() = Budget(id, ledgerId, groupId, amountMinor)
fun Budget.toEntity() = BudgetEntity(id, ledgerId, groupId, amountMinor)
