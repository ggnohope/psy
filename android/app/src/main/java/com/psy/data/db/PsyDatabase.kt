package com.psy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.psy.data.db.dao.AccountDao
import com.psy.data.db.dao.BudgetDao
import com.psy.data.db.dao.CategoryDao
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.dao.TransactionDao
import com.psy.data.db.entity.AccountEntity
import com.psy.data.db.entity.BudgetEntity
import com.psy.data.db.entity.CategoryEntity
import com.psy.data.db.entity.LedgerEntity
import com.psy.data.db.entity.TransactionEntity

@Database(
    entities = [LedgerEntity::class, AccountEntity::class, CategoryEntity::class, TransactionEntity::class, BudgetEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class PsyDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
}
