package com.psy.data.backup

import com.psy.data.db.entity.AccountEntity
import com.psy.data.db.entity.BudgetEntity
import com.psy.data.db.entity.CategoryEntity
import com.psy.data.db.entity.CategoryGroupEntity
import com.psy.data.db.entity.LedgerEntity
import com.psy.data.db.entity.TransactionEntity
import kotlinx.serialization.Serializable

// ─── Top-level snapshot ────────────────────────────────────────────────────

@Serializable
data class SnapshotDto(
    val version: Int = 3,
    val ledgers: List<LedgerDto>,
    val accounts: List<AccountDto>,
    val categoryGroups: List<CategoryGroupDto> = emptyList(),
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val budgets: List<BudgetDto>,
)

// ─── Per-entity DTOs (mirror every field, nullable where entity is) ─────────

@Serializable
data class LedgerDto(
    val id: Long,
    val name: String,
    val icon: String,
    val currency: String,
    val createdAt: Long,
)

@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val type: String,
    val icon: String,
    val color: Long,
    val isFund: Boolean = false,
)

@Serializable
data class CategoryGroupDto(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    val sortOrder: Int,
)

@Serializable
data class CategoryDto(
    val id: Long,
    val groupId: Long,
    val name: String,
    val icon: String,
    val sortOrder: Int,
)

@Serializable
data class TransactionDto(
    val id: Long,
    val ledgerId: Long,
    val type: String,
    val amountMinor: Long,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long?,
    val note: String,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val photoUri: String?,
)

@Serializable
data class BudgetDto(
    val id: Long,
    val ledgerId: Long,
    val groupId: Long?,
    val amountMinor: Long,
)

// ─── Entity ↔ Dto mapping helpers ──────────────────────────────────────────

fun LedgerEntity.toDto() = LedgerDto(id, name, icon, currency, createdAt)
fun LedgerDto.toEntity() = LedgerEntity(id, name, icon, currency, createdAt)

fun AccountEntity.toDto() = AccountDto(id, name, type, icon, color, isFund)
fun AccountDto.toEntity() = AccountEntity(id, name, type, icon, color, isFund)

fun CategoryGroupEntity.toDto() = CategoryGroupDto(id, name, icon, color, type, sortOrder)
fun CategoryGroupDto.toEntity() = CategoryGroupEntity(id, name, icon, color, type, sortOrder)

fun CategoryEntity.toDto() = CategoryDto(id, groupId, name, icon, sortOrder)
fun CategoryDto.toEntity() = CategoryEntity(id, groupId, name, icon, sortOrder)

fun TransactionEntity.toDto() =
    TransactionDto(id, ledgerId, type, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri)

fun TransactionDto.toEntity() =
    TransactionEntity(id, ledgerId, type, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri)

fun BudgetEntity.toDto() = BudgetDto(id, ledgerId, groupId, amountMinor)
fun BudgetDto.toEntity() = BudgetEntity(id, ledgerId, groupId, amountMinor)
