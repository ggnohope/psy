# Psy Core Bookkeeping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **NO UNIT TESTS** in this plan (user preference — too token-expensive). Verify each task by compiling (`./gradlew :app:assembleDebug`) and, at the end, running the app manually. Two-stage review (spec + quality) still applies per task.

**Goal:** A working income/expense recording loop: open the app → Home shows this month's balance and transactions grouped by day → tap + to add an income/expense (amount, category, account, date, note) → save → it appears on Home. Tap a transaction to edit or delete. Data persists in Room, seeded on first run with a default ledger, a cash account, and default categories.

**Architecture:** Builds on the Phase 0 skeleton (single-module Kotlin, MVVM, Hilt, Room, Compose, package `com.psy` with `data/domain/ui/di`). Adds Account/Category/Transaction to the data layer, domain models + repositories, a first-run seeder, Navigation Compose, and two screens (Home, Add/Edit) with Hilt ViewModels.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Room, Hilt, Navigation Compose, Coroutines/Flow. Amounts are `Long` minor units formatted via `com.psy.domain.util.Money`.

**Environment for every gradle command:**
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` then `cd /Users/hoalam/Codes/psy/android && ./gradlew ...`

**Deferred to follow-up plans (do NOT build here):** transfer-type transactions, photo attachment, ledger/account/category management screens, multi-ledger switching, search, statistics/charts, calendar, budget, backup/auth, theming/lock.

---

## File Structure

```
android/app/src/main/java/com/psy/
├── domain/
│   ├── model/        Ledger.kt, Account.kt, Category.kt, Transaction.kt, enums (TxType, AccountType, CategoryType), Currency.kt
│   └── repository/   LedgerRepository, AccountRepository, CategoryRepository, TransactionRepository (interfaces)
├── data/
│   ├── db/
│   │   ├── entity/   AccountEntity.kt, CategoryEntity.kt, TransactionEntity.kt (LedgerEntity exists)
│   │   ├── dao/      AccountDao.kt, CategoryDao.kt, TransactionDao.kt (LedgerDao exists)
│   │   ├── PsyDatabase.kt   (extend: add entities, bump version)
│   │   └── mapper/   Mappers.kt (entity <-> domain)
│   ├── repo/         LedgerRepositoryImpl, AccountRepositoryImpl, CategoryRepositoryImpl, TransactionRepositoryImpl
│   └── seed/         DefaultDataSeeder.kt
├── di/
│   ├── DatabaseModule.kt  (extend: provide new DAOs)
│   └── RepositoryModule.kt  (new: bind repo impls)
└── ui/
    ├── navigation/   PsyNavHost.kt, Routes.kt
    ├── home/         HomeScreen.kt, HomeViewModel.kt
    ├── addedit/      AddEditTransactionScreen.kt, AddEditTransactionViewModel.kt
    └── components/    (shared composables as needed, e.g. AmountText.kt)
```

---

## Task 1: Domain models + enums + Currency

**Files (create under `android/app/src/main/java/com/psy/domain/model/`):**

- [ ] **Step 1: Enums + Currency**

`Enums.kt`:
```kotlin
package com.psy.domain.model

enum class TxType { INCOME, EXPENSE } // TRANSFER deferred
enum class AccountType { CASH, BANK, CREDIT, ASSET }
enum class CategoryType { INCOME, EXPENSE }
```

`Currency.kt`:
```kotlin
package com.psy.domain.model

/** Minimal currency metadata for formatting. v1 supports VND + USD. */
data class Currency(val code: String, val symbol: String, val fractionDigits: Int) {
    companion object {
        val VND = Currency("VND", "đ", 0)
        val USD = Currency("USD", "$", 2)
        fun of(code: String): Currency = when (code) {
            "USD" -> USD
            else -> VND
        }
    }
}
```

- [ ] **Step 2: Domain models**

`Ledger.kt`:
```kotlin
package com.psy.domain.model

data class Ledger(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val currency: String,
    val createdAt: Long,
)
```

`Account.kt`:
```kotlin
package com.psy.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val color: Long, // ARGB packed
)
```

`Category.kt`:
```kotlin
package com.psy.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long, // ARGB packed
    val type: CategoryType,
    val sortOrder: Int,
)
```

`Transaction.kt`:
```kotlin
package com.psy.domain.model

data class Transaction(
    val id: Long = 0,
    val ledgerId: Long,
    val type: TxType,
    val amountMinor: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val date: Long, // epoch millis of the day the bill occurred
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 3: Build + commit**

`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/domain/model/
git commit -m "feat(domain): add bookkeeping models (Account, Category, Transaction, enums, Currency)"
```

---

## Task 2: Room entities + DAOs + PsyDatabase bump

**Files under `android/app/src/main/java/com/psy/data/db/`:**

- [ ] **Step 1: Entities** (`entity/`)

`AccountEntity.kt`:
```kotlin
package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,   // AccountType.name
    val icon: String,
    val color: Long,
)
```

`CategoryEntity.kt`:
```kotlin
package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,   // CategoryType.name
    val sortOrder: Int,
)
```

`TransactionEntity.kt`:
```kotlin
package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("ledgerId"), Index("date"), Index("categoryId"), Index("accountId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val type: String,   // TxType.name
    val amountMinor: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 2: DAOs** (`dao/`)

`AccountDao.kt`:
```kotlin
package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(account: AccountEntity): Long
    @Query("SELECT * FROM accounts ORDER BY id ASC") fun observeAll(): Flow<List<AccountEntity>>
    @Query("SELECT COUNT(*) FROM accounts") suspend fun count(): Int
}
```

`CategoryDao.kt`:
```kotlin
package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(category: CategoryEntity): Long
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC") fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
}
```

`TransactionDao.kt`:
```kotlin
package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(tx: TransactionEntity): Long
    @Delete suspend fun delete(tx: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun getById(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE ledgerId = :ledgerId AND date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(ledgerId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>
}
```

- [ ] **Step 3: Extend PsyDatabase** (overwrite `PsyDatabase.kt`)

```kotlin
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
    version = 2,
    exportSchema = false,
)
abstract class PsyDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}
```

- [ ] **Step 4: Add destructive migration to the Room builder** (modify `di/DatabaseModule.kt`)

In `provideDatabase`, change the builder to allow destructive migration (dev-only; no production data yet). Use the current Room 2.8 API:
```kotlin
Room.databaseBuilder(context, PsyDatabase::class.java, "psy.db")
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
```
Also add provider functions for the new DAOs:
```kotlin
@Provides fun provideAccountDao(db: PsyDatabase): AccountDao = db.accountDao()
@Provides fun provideCategoryDao(db: PsyDatabase): CategoryDao = db.categoryDao()
@Provides fun provideTransactionDao(db: PsyDatabase): TransactionDao = db.transactionDao()
```
(If `fallbackToDestructiveMigration(dropAllTables = true)` is not the exact signature in Room 2.8.4, use the non-deprecated equivalent that the compiler accepts.)

- [ ] **Step 5: Build + commit**

`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (Room compiler regenerates).
```bash
git add android/app/src/main/java/com/psy/data/db/ android/app/src/main/java/com/psy/di/DatabaseModule.kt
git commit -m "feat(data): add Account/Category/Transaction entities, DAOs, db v2"
```

---

## Task 3: Mappers + repositories + DI binding

**Files:**

- [ ] **Step 1: Mappers** — `data/db/mapper/Mappers.kt`

Provide entity↔domain conversion for all four types. Enums map via `valueOf(...)`/`.name`. Example shape (implement all four pairs analogously):
```kotlin
package com.psy.data.db.mapper

import com.psy.data.db.entity.*
import com.psy.domain.model.*

fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), icon, color)
fun Account.toEntity() = AccountEntity(id, name, type.name, icon, color)

fun CategoryEntity.toDomain() = Category(id, name, icon, color, CategoryType.valueOf(type), sortOrder)
fun Category.toEntity() = CategoryEntity(id, name, icon, color, type.name, sortOrder)

fun LedgerEntity.toDomain() = Ledger(id, name, icon, currency, createdAt)
fun Ledger.toEntity() = LedgerEntity(id, name, icon, currency, createdAt)

fun TransactionEntity.toDomain() = Transaction(id, ledgerId, TxType.valueOf(type), amountMinor, categoryId, accountId, note, date, createdAt, updatedAt)
fun Transaction.toEntity() = TransactionEntity(id, ledgerId, type.name, amountMinor, categoryId, accountId, note, date, createdAt, updatedAt)
```

- [ ] **Step 2: Repository interfaces** — `domain/repository/`

```kotlin
// LedgerRepository.kt
package com.psy.domain.repository
import com.psy.domain.model.Ledger
import kotlinx.coroutines.flow.Flow
interface LedgerRepository {
    fun observeAll(): Flow<List<Ledger>>
    suspend fun firstOrNull(): Ledger?
    suspend fun upsert(ledger: Ledger): Long
}
```
```kotlin
// AccountRepository.kt
package com.psy.domain.repository
import com.psy.domain.model.Account
import kotlinx.coroutines.flow.Flow
interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    suspend fun count(): Int
    suspend fun upsert(account: Account): Long
}
```
```kotlin
// CategoryRepository.kt
package com.psy.domain.repository
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow
interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByType(type: CategoryType): Flow<List<Category>>
    suspend fun count(): Int
    suspend fun upsert(category: Category): Long
}
```
```kotlin
// TransactionRepository.kt
package com.psy.domain.repository
import com.psy.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
interface TransactionRepository {
    fun observeBetween(ledgerId: Long, start: Long, end: Long): Flow<List<Transaction>>
    suspend fun getById(id: Long): Transaction?
    suspend fun upsert(tx: Transaction): Long
    suspend fun delete(tx: Transaction)
}
```
Note: `LedgerDao` (from Phase 0) needs a `firstOrNull`/`count`. Add to `LedgerDao`:
```kotlin
@Query("SELECT * FROM ledgers ORDER BY createdAt ASC LIMIT 1") suspend fun firstOrNull(): com.psy.data.db.entity.LedgerEntity?
@Query("SELECT COUNT(*) FROM ledgers") suspend fun count(): Int
```

- [ ] **Step 3: Repository impls** — `data/repo/` (use the DAOs + mappers; map Flows with `.map { list -> list.map { it.toDomain() } }`). Implement all four (`LedgerRepositoryImpl`, `AccountRepositoryImpl`, `CategoryRepositoryImpl`, `TransactionRepositoryImpl`), each `@Inject constructor(private val dao: ...Dao)`.

- [ ] **Step 4: DI binding** — `di/RepositoryModule.kt`
```kotlin
package com.psy.di

import com.psy.data.repo.*
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
}
```

- [ ] **Step 5: Build + commit**

`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/data/db/mapper/ android/app/src/main/java/com/psy/data/db/dao/LedgerDao.kt android/app/src/main/java/com/psy/domain/repository/ android/app/src/main/java/com/psy/data/repo/ android/app/src/main/java/com/psy/di/RepositoryModule.kt
git commit -m "feat(data): add mappers, repositories, and Hilt repository bindings"
```

---

## Task 4: First-run seeder + app-start wiring

**Files:**

- [ ] **Step 1: Seeder** — `data/seed/DefaultDataSeeder.kt`

Seeds (only if empty) a default ledger ("Sổ của tôi", icon "wallet", currency "VND"), a "Tiền mặt" CASH account, and default categories. Use a fixed timestamp passed in (do not call System.currentTimeMillis at construction; use it inside the suspend fun). Default categories (icon = emoji string placeholder; color = packed ARGB Long):

Expense: Ăn uống 🍜, Di chuyển 🚌, Mua sắm 🛍️, Hoá đơn 🧾, Giải trí 🎮, Sức khoẻ 💊, Khác 📦
Income: Lương 💰, Thưởng 🎁, Khác 📦

```kotlin
package com.psy.data.seed

import com.psy.domain.model.*
import com.psy.domain.repository.*
import javax.inject.Inject

class DefaultDataSeeder @Inject constructor(
    private val ledgerRepo: LedgerRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
) {
    suspend fun seedIfEmpty(now: Long) {
        if (ledgerRepo.firstOrNull() == null) {
            ledgerRepo.upsert(Ledger(name = "Sổ của tôi", icon = "wallet", currency = "VND", createdAt = now))
        }
        if (accountRepo.count() == 0) {
            accountRepo.upsert(Account(name = "Tiền mặt", type = AccountType.CASH, icon = "cash", color = 0xFF22C55E))
        }
        if (categoryRepo.count() == 0) {
            val expense = listOf("Ăn uống" to "🍜", "Di chuyển" to "🚌", "Mua sắm" to "🛍️", "Hoá đơn" to "🧾", "Giải trí" to "🎮", "Sức khoẻ" to "💊", "Khác" to "📦")
            val income = listOf("Lương" to "💰", "Thưởng" to "🎁", "Khác" to "📦")
            expense.forEachIndexed { i, (n, ic) -> categoryRepo.upsert(Category(name = n, icon = ic, color = 0xFFFF8FD6, type = CategoryType.EXPENSE, sortOrder = i)) }
            income.forEachIndexed { i, (n, ic) -> categoryRepo.upsert(Category(name = n, icon = ic, color = 0xFF7FD8FF, type = CategoryType.INCOME, sortOrder = i)) }
        }
    }
}
```

- [ ] **Step 2: Run seeder at app start.** In `PsyApplication`, inject the seeder and run it in a background coroutine on `onCreate` (use `@Inject lateinit var seeder: DefaultDataSeeder` and a `CoroutineScope(Dispatchers.IO).launch { seeder.seedIfEmpty(System.currentTimeMillis()) }`). Keep it minimal.

- [ ] **Step 3: Build + commit**

`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/data/seed/ android/app/src/main/java/com/psy/PsyApplication.kt
git commit -m "feat(data): seed default ledger, account, and categories on first run"
```

---

## Task 5: Navigation + Home screen

**Files under `ui/`:**

- [ ] **Step 1: Routes + NavHost** — `ui/navigation/Routes.kt`, `ui/navigation/PsyNavHost.kt`
  - `Routes`: `const val HOME = "home"`, `fun addEdit(txId: Long? = null)` → `"addEdit?txId=${txId ?: -1L}"`, route pattern `"addEdit?txId={txId}"` with a `txId: Long` nav arg (default -1).
  - `PsyNavHost`: `NavHost(startDestination = HOME)`; `composable(HOME)` → `HomeScreen(onAddClick = { nav.navigate(Routes.addEdit()) }, onTxClick = { id -> nav.navigate(Routes.addEdit(id)) })`; `composable("addEdit?txId={txId}", args type Long default -1)` → `AddEditTransactionScreen(onDone = { nav.popBackStack() })` (ViewModel reads txId via SavedStateHandle).

- [ ] **Step 2: HomeViewModel** — `ui/home/HomeViewModel.kt`
  - `@HiltViewModel`, inject `LedgerRepository`, `TransactionRepository`, `CategoryRepository`, `AccountRepository`.
  - Compute current-month range [start, end] (use `java.time` with system default zone; start = first day of current month 00:00, end = last ms of month). Resolve the active ledger = `ledgerRepo.firstOrNull()` (collect ledgers flow; use the first).
  - Expose `StateFlow<HomeUiState>` where:
    ```kotlin
    data class HomeUiState(
        val monthLabel: String = "",
        val incomeMinor: Long = 0, val expenseMinor: Long = 0, val netMinor: Long = 0,
        val currency: Currency = Currency.VND,
        val days: List<DayGroup> = emptyList(),
        val loading: Boolean = true,
    )
    data class DayGroup(val dateLabel: String, val items: List<TxRow>)
    data class TxRow(val id: Long, val categoryName: String, val categoryIcon: String, val accountName: String, val type: TxType, val amountMinor: Long, val note: String)
    ```
  - Combine the transactions Flow for the month with categories+accounts (to resolve names/icons), group by day (by date → "dd/MM" or "Hôm nay"/"Hôm qua"), sum income/expense for the balance card. Income adds, expense subtracts for net.

- [ ] **Step 3: HomeScreen** — `ui/home/HomeScreen.kt`
  - `Scaffold` with a `FloatingActionButton` (Icons.Default.Add, container = primary) calling `onAddClick`. Top: a rounded **balance card** (gradient using `CandyViolet`→`CandySky`, large corner) showing `monthLabel`, big net amount (`Money.formatMinor`), and income/expense sub-row. Below: a `LazyColumn` of day groups — each group a small day header + rows; each `TxRow` is a rounded card: leading emoji icon in a tinted circle, category name + note + account, trailing amount colored (expense = `CandyPinkDeep`, income = `CandyGreen`) formatted with sign. Clicking a row → `onTxClick(id)`. Empty state: a friendly centered message when `days` is empty.
  - Collect state via `viewModel.uiState.collectAsStateWithLifecycle()`.

- [ ] **Step 4: Build + commit**

`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/ui/
git commit -m "feat(ui): add navigation and Home screen with month balance + grouped transactions"
```

---

## Task 6: Add/Edit Transaction screen + wire MainActivity

**Files:**

- [ ] **Step 1: AddEditTransactionViewModel** — `ui/addedit/AddEditTransactionViewModel.kt`
  - `@HiltViewModel`, inject repos + `SavedStateHandle` (read `txId`, -1 = new).
  - Form state:
    ```kotlin
    data class AddEditUiState(
        val isEdit: Boolean = false,
        val type: TxType = TxType.EXPENSE,
        val amountText: String = "",          // raw digits the user typed (minor units assembled on save)
        val categories: List<Category> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val selectedCategoryId: Long? = null,
        val selectedAccountId: Long? = null,
        val date: Long = 0L,
        val note: String = "",
        val currency: Currency = Currency.VND,
        val canSave: Boolean = false,
    )
    ```
  - On init: load accounts + the active ledger's currency; observe categories filtered by `type`; if editing, load the tx and prefill. When `type` changes, reload category list for that type and clear `selectedCategoryId` if it no longer belongs.
  - `onAmountChange`, `onTypeChange`, `selectCategory`, `selectAccount`, `onDateChange`, `onNoteChange` mutate state. `canSave = amount > 0 && selectedCategoryId != null && selectedAccountId != null`.
  - `save(now: Long)`: build `Transaction` (amountMinor parsed from amountText respecting `currency.fractionDigits` — for VND fractionDigits=0 the typed integer IS the minor amount; for 2-digit currencies, interpret typed value appropriately — keep v1 simple: treat the typed integer string as a whole-unit value and multiply by 10^fractionDigits, OR for VND just use the integer. Document the chosen rule in a comment), `createdAt`/`updatedAt = now`, `ledgerId` = active ledger. `upsert`, then signal done.
  - `delete()` when editing.

- [ ] **Step 2: AddEditTransactionScreen** — `ui/addedit/AddEditTransactionScreen.kt`
  - `Scaffold` with a TopAppBar (title "Thêm giao dịch"/"Sửa giao dịch", back nav, and a delete icon when editing). Body:
    - **Income/Expense segmented toggle** at top (two pill buttons; selected = primary).
    - **Amount** display (large, formatted) + a simple on-screen numeric keypad OR a `TextField` with number keyboard (keypad is nicer/cuter but a number `OutlinedTextField` is acceptable for v1 — implementer's choice, keep it clean).
    - **Category grid**: `LazyVerticalGrid` of category chips (emoji + name), selected highlighted.
    - **Account selector**: a row of account chips.
    - **Date**: a button showing the date; tapping opens Material3 `DatePicker` (dialog).
    - **Note**: an `OutlinedTextField`.
    - **Save** button (enabled when `canSave`) → `viewModel.save(System.currentTimeMillis()); onDone()`.
  - Collect state with `collectAsStateWithLifecycle()`.

- [ ] **Step 2b: Wire MainActivity** — replace the wizard `Greeting`/`Scaffold` body with `PsyNavHost()` inside `PsyTheme`. Remove the `Greeting` + `GreetingPreview` composables.

- [ ] **Step 3: Build + commit**

`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/ui/ android/app/src/main/java/com/psy/MainActivity.kt
git commit -m "feat(ui): add/edit transaction screen wired into nav and MainActivity"
```

---

## Task 7: Verification gate (manual run, no unit tests)

**Files:** none.

- [ ] **Step 1: Build + lint**

`./gradlew :app:assembleDebug :app:lintDebug` → BUILD SUCCESSFUL (review lint report for new errors; warnings acceptable).

- [ ] **Step 2: Manual run checklist** (install on emulator/device from Android Studio; the controller will hand this to the human or run via an emulator if available):
  1. App launches to Home with the Candy Pop balance card (this month, 0/0/0 initially).
  2. Tap + → Add screen. Default type = Expense; categories show the 7 expense categories; account "Tiền mặt" selectable.
  3. Enter amount 45000, pick "Ăn uống", pick "Tiền mặt", keep today, note "Cơm trưa", Save.
  4. Home now shows the transaction under today's group; expense + net updated (net = -45,000 đ).
  5. Switch to Income on a new entry (Lương 5,000,000) → Home income + net update.
  6. Tap an existing transaction → edit screen prefilled; change amount; Save → Home reflects it.
  7. Edit screen → delete → transaction disappears from Home.
  8. Kill & reopen app → data persisted (Room).

- [ ] **Step 3: Confirm git history clean**

`git --no-pager log --oneline && git status -s`.

---

## Self-Review Notes
- **Spec coverage:** CRUD income/expense (Tasks 2,3,6) + Home with month balance & grouped list (Task 5) + custom categories/accounts data model + seeded defaults (Tasks 1–4). Offline-first via Room (source of truth). Money handled as Long minor units via `Money.formatMinor`.
- **No tests** per user preference; verification is compile + manual run checklist.
- **Type consistency:** repository method names match between interface (Task 3) and ViewModels (Tasks 5,6); enum `.name`/`valueOf` mapping is symmetric (Task 3 mappers); `Routes.addEdit(txId)` arg matches the NavHost route pattern and the ViewModel's SavedStateHandle key (`txId`).
- **DB migration:** version bumped 1→2 with destructive fallback (no production data) — acceptable for this learning phase.
