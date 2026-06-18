package com.psy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.psy.data.db.dao.AccountDao
import com.psy.data.db.dao.CategoryDao
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.dao.TransactionDao
import com.psy.data.db.entity.AccountEntity
import com.psy.data.db.entity.CategoryEntity
import com.psy.data.db.entity.LedgerEntity
import com.psy.data.db.entity.TransactionEntity

@Database(
    entities = [LedgerEntity::class, AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class PsyDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}
