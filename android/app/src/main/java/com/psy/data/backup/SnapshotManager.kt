package com.psy.data.backup

import android.util.Log
import androidx.room.withTransaction
import com.psy.data.db.PsyDatabase
import com.psy.data.db.dao.AccountDao
import com.psy.data.db.dao.BudgetDao
import com.psy.data.db.dao.CategoryDao
import com.psy.data.db.dao.CategoryGroupDao
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.dao.TransactionDao
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Tiny probe to read just the snapshot [version] without forcing a full decode.
 * Needed because [CategoryDto] changed shape in v2 — an old v1 JSON would fail
 * to deserialize cleanly into the new shape, so we must branch on the version
 * BEFORE attempting the full decode.
 */
@Serializable
private data class VersionProbe(val version: Int = 1)

class SnapshotManager @Inject constructor(
    private val db: PsyDatabase,
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
    private val categoryGroupDao: CategoryGroupDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val json: Json,
) {

    /** Cheap emptiness check: true when there are no ledgers (hence no usable data). */
    suspend fun isLocalEmpty(): Boolean = ledgerDao.count() == 0

    /**
     * Clear all local data in a single atomic transaction.
     * Children are deleted before parents (consistent with FK semantics).
     */
    suspend fun wipeLocal() {
        db.withTransaction {
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            categoryDao.deleteAll()
            categoryGroupDao.deleteAll()
            accountDao.deleteAll()
            ledgerDao.deleteAll()
        }
    }

    /** Read every table and encode to a JSON string. */
    suspend fun export(): String {
        val snapshot = SnapshotDto(
            ledgers = ledgerDao.getAll().map { it.toDto() },
            accounts = accountDao.getAll().map { it.toDto() },
            categoryGroups = categoryGroupDao.getAll().map { it.toDto() },
            categories = categoryDao.getAll().map { it.toDto() },
            transactions = transactionDao.getAll().map { it.toDto() },
            budgets = budgetDao.getAll().map { it.toDto() },
        )
        return json.encodeToString(SnapshotDto.serializer(), snapshot)
    }

    /**
     * Decode [jsonStr] and replace the entire database content in a single
     * atomic transaction. Children are deleted before parents; parents are
     * inserted before children — consistent with FK semantics even though
     * Room does not currently enforce FKs on this schema.
     *
     * Delete order  : transactions → budgets → categories → categoryGroups → accounts → ledgers
     * Insert order  : ledgers → accounts → categoryGroups → categories → transactions → budgets
     *
     * v1 compatibility: the category shape changed in v2 (groups + leaves). For
     * old v1 snapshots we deliberately DROP categories/categoryGroups/budgets and
     * let [DefaultDataSeeder] re-seed the new sample category tree on next launch.
     * Transactions are still restored; any transaction referencing an old (now
     * dangling) categoryId is tolerated by the UI (renders a "—" fallback).
     */
    suspend fun import(jsonStr: String) {
        val version = runCatching { json.decodeFromString(VersionProbe.serializer(), jsonStr).version }
            .getOrDefault(1)
        val isLegacy = version < 2
        if (isLegacy) {
            Log.w(
                "SnapshotManager",
                "Restoring legacy snapshot v$version: categories/categoryGroups/budgets " +
                    "will be reset to the new sample tree (incompatible v1 category shape)."
            )
            importLegacy(jsonStr)
        } else {
            importV2(jsonStr)
        }
    }

    /** Normal path: full v2 round-trip. */
    private suspend fun importV2(jsonStr: String) {
        val dto = json.decodeFromString<SnapshotDto>(jsonStr)
        db.withTransaction {
            clearAll()
            // Insert parents first
            ledgerDao.insertAll(dto.ledgers.map { it.toEntity() })
            accountDao.insertAll(dto.accounts.map { it.toEntity() })
            categoryGroupDao.insertAll(dto.categoryGroups.map { it.toEntity() })
            categoryDao.insertAll(dto.categories.map { it.toEntity() })
            transactionDao.insertAll(dto.transactions.map { it.toEntity() })
            budgetDao.insertAll(dto.budgets.map { it.toEntity() })
        }
    }

    /**
     * Legacy path: restore only the entities whose shape is still compatible
     * (ledgers, accounts, transactions). Categories/categoryGroups/budgets are
     * left empty; [DefaultDataSeeder] re-seeds the category tree afterwards.
     * We decode only the safe sub-fields to avoid choking on the old CategoryDto.
     */
    private suspend fun importLegacy(jsonStr: String) {
        // Decode into a permissive legacy DTO that ignores the old category/budget shapes.
        val legacy = json.decodeFromString(LegacySnapshotDto.serializer(), jsonStr)
        db.withTransaction {
            clearAll()
            ledgerDao.insertAll(legacy.ledgers.map { it.toEntity() })
            accountDao.insertAll(legacy.accounts.map { it.toEntity() })
            transactionDao.insertAll(legacy.transactions.map { it.toEntity() })
            // categoryGroups / categories / budgets intentionally left empty.
        }
    }

    /** Delete children before parents. */
    private suspend fun clearAll() {
        transactionDao.deleteAll()
        budgetDao.deleteAll()
        categoryDao.deleteAll()
        categoryGroupDao.deleteAll()
        accountDao.deleteAll()
        ledgerDao.deleteAll()
    }
}

/**
 * Minimal view of a legacy (v1) snapshot — only the fields whose shape is
 * unchanged in v2. The old `categories` / `budgets` arrays are omitted on
 * purpose; with [Json.ignoreUnknownKeys] = true they are simply skipped.
 */
@Serializable
private data class LegacySnapshotDto(
    val ledgers: List<LedgerDto>,
    val accounts: List<AccountDto>,
    val transactions: List<TransactionDto>,
)
