package com.psy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.entity.LedgerEntity

@Database(entities = [LedgerEntity::class], version = 1, exportSchema = false)
abstract class PsyDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
}
