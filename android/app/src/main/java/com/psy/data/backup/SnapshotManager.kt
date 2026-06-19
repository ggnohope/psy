package com.psy.data.backup

import androidx.room.withTransaction
import com.psy.data.db.PsyDatabase
import com.psy.data.db.dao.AccountDao
import com.psy.data.db.dao.BudgetDao
import com.psy.data.db.dao.CategoryDao
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.dao.TransactionDao
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SnapshotManager @Inject constructor(
    private val db: PsyDatabase,
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
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
            accountDao.deleteAll()
            ledgerDao.deleteAll()
        }
    }

    /** Read every table and encode to a JSON string. */
    suspend fun export(): String {
        val snapshot = SnapshotDto(
            ledgers = ledgerDao.getAll().map { it.toDto() },
            accounts = accountDao.getAll().map { it.toDto() },
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
     * Delete order  : transactions → budgets → categories → accounts → ledgers
     * Insert order  : ledgers → accounts → categories → transactions → budgets
     */
    suspend fun import(jsonStr: String) {
        val dto = json.decodeFromString<SnapshotDto>(jsonStr)
        db.withTransaction {
            // Delete children first
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()
            ledgerDao.deleteAll()

            // Insert parents first
            ledgerDao.insertAll(dto.ledgers.map { it.toEntity() })
            accountDao.insertAll(dto.accounts.map { it.toEntity() })
            categoryDao.insertAll(dto.categories.map { it.toEntity() })
            transactionDao.insertAll(dto.transactions.map { it.toEntity() })
            budgetDao.insertAll(dto.budgets.map { it.toEntity() })
        }
    }
}
