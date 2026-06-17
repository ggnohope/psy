package com.psy.di

import android.content.Context
import androidx.room.Room
import com.psy.data.db.PsyDatabase
import com.psy.data.db.dao.AccountDao
import com.psy.data.db.dao.CategoryDao
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PsyDatabase =
        Room.databaseBuilder(context, PsyDatabase::class.java, "psy.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideLedgerDao(db: PsyDatabase): LedgerDao = db.ledgerDao()
    @Provides fun provideAccountDao(db: PsyDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: PsyDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTransactionDao(db: PsyDatabase): TransactionDao = db.transactionDao()
}
