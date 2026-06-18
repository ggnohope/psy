package com.psy.data.db.mapper

import com.psy.data.db.entity.*
import com.psy.domain.model.*

fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), icon, color)
fun Account.toEntity() = AccountEntity(id, name, type.name, icon, color)

fun CategoryEntity.toDomain() = Category(id, name, icon, color, CategoryType.valueOf(type), sortOrder)
fun Category.toEntity() = CategoryEntity(id, name, icon, color, type.name, sortOrder)

fun LedgerEntity.toDomain() = Ledger(id, name, icon, currency, createdAt)
fun Ledger.toEntity() = LedgerEntity(id, name, icon, currency, createdAt)

fun TransactionEntity.toDomain() = Transaction(id, ledgerId, TxType.valueOf(type), amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri)
fun Transaction.toEntity() = TransactionEntity(id, ledgerId, type.name, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri)
