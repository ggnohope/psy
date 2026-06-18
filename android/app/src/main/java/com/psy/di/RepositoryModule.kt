package com.psy.di

import com.psy.data.repo.*
import com.psy.data.settings.SettingsRepository
import com.psy.data.settings.SettingsRepositoryImpl
import com.psy.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindLedgerRepo(impl: LedgerRepositoryImpl): LedgerRepository
    @Binds @Singleton abstract fun bindAccountRepo(impl: AccountRepositoryImpl): AccountRepository
    @Binds @Singleton abstract fun bindCategoryRepo(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindTransactionRepo(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindBudgetRepo(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindSettingsRepo(impl: SettingsRepositoryImpl): SettingsRepository
}
