# Psy — Budget Design Spec

**Date:** 2026-06-18
**Status:** Approved (brainstorming)
**Author:** hoalam

## Overview

Add monthly budgets to Psy: a total monthly spending limit plus optional per-category limits, each
shown as progress (spent vs limit) for the selected month with an over-budget warning. Surfaced as a
4th bottom-nav tab "Ngân sách". Builds on the merged core + polish + stats/calendar (offline-first
Kotlin/Compose/Room/Hilt/MVVM, `com.psy`, Candy Pop theme).

**No unit tests** (user preference). Verify by `./gradlew :app:assembleDebug :app:lintDebug` (0 lint
errors) + manual emulator run.

## Scope (approved)

- **Total + per-category** monthly budgets.
- Budgets are **recurring monthly limits** (not tied to a specific month); "spent" is computed for the
  selected month from EXPENSE transactions.
- 4th bottom-nav tab "Ngân sách".
- Add dependency `androidx.compose.material:material-icons-extended` for proper tab icons (Savings for
  Budget; also upgrade the Stats tab icon from the placeholder Star to BarChart).

Deferred: yearly budgets; budget rollover/carry-over; notifications/alerts; budget history per month.

## Data model (Room v4, destructive migration — dev-only)

New `BudgetEntity` (table `budgets`):
- `id: Long @PrimaryKey(autoGenerate = true)`
- `ledgerId: Long` (Index)
- `categoryId: Long?` — null means the **total** budget; non-null means that category's budget
- `amountMinor: Long`

Domain `Budget(id, ledgerId, categoryId: Long?, amountMinor)`.
`PsyDatabase` version **3 → 4**, add `BudgetEntity` + `budgetDao()`, keep
`fallbackToDestructiveMigration(dropAllTables = true)` (dev-only; re-seeds defaults — budgets start empty).

`BudgetDao`: `@Upsert`/`@Insert(REPLACE)` `upsert(BudgetEntity): Long`, `@Query observeAll(ledgerId): Flow<List<BudgetEntity>>`,
`@Query findByCategory(ledgerId, categoryId): BudgetEntity?` (categoryId nullable — use two queries: one
`WHERE categoryId IS NULL` for total, one `WHERE categoryId = :id`), `@Delete delete(BudgetEntity)`.

`BudgetRepository` (interface + impl, Hilt @Binds):
- `observeAll(ledgerId): Flow<List<Budget>>`
- `suspend setBudget(ledgerId, categoryId: Long?, amountMinor)` — find existing budget for that
  categoryId (null = total) and update it, else insert (enforces one budget per category + one total).
- `suspend removeBudget(budget: Budget)`

## Navigation

Add `Routes.BUDGET = "budget"`. `PsyBottomBar` gains a 4th `NavigationBarItem` "Ngân sách"
(`Icons.Default.Savings`). `PsyNavHost` adds `composable(Routes.BUDGET){ BudgetScreen() }` and includes
BUDGET in the `bottomBarRoutes` set. Also swap the Stats tab icon to `Icons.Default.BarChart`.
Build dependency: add `material-icons-extended` to the version catalog + app `dependencies`.

## Budget screen (`ui/budget/`)

`BudgetViewModel` (@HiltViewModel; inject BudgetRepository, TransactionRepository, CategoryRepository, LedgerRepository):
- `selectedMonth: MutableStateFlow<YearMonth>` (default now); `prevMonth()/nextMonth()`.
- Resolve active ledger → ledgerId + `Currency.of(currency)`.
- Reactively combine: budgets(ledger), month EXPENSE transactions (`observeBetween` half-open month range),
  categories(map). Compute:
  - **totalBudget**: the budget with categoryId == null (or none). totalSpent = sum of all EXPENSE in
    month (exclude INCOME/TRANSFER). progress = spent/limit (guard limit==0).
  - **categoryBudgets**: for each budget with categoryId != null → `CategoryBudgetRow(category, limitMinor,
    spentMinor = sum EXPENSE of that category in month, percent)`. Sort by percent desc (most-used first).
  - editor state: `editorOpen`, `editingBudget: Budget?` (null = adding), `editorCategoryId: Long?`
    (null = total when adding total), `draftAmountText`, plus the list of categories that don't yet have a
    budget (for the add-category picker).
- Functions: `startAddTotal()`, `startAddCategory()`, `startEdit(budget)`, `onAmountChange`, `selectEditorCategory(id)`,
  `saveEditor()` (setBudget), `removeEditor()` (removeBudget), `closeEditor()`.
- Expose `StateFlow<BudgetUiState>` (monthLabel, currency, totalBudget {limit, spent, percent} or null,
  categoryBudgets list, editor fields, loading).

`BudgetScreen` (`BudgetScreen(viewModel = hiltViewModel())`): Scaffold + TopAppBar("Ngân sách"). Column (scroll):
- `MonthSelector`.
- **Total budget card**: if set → a `BudgetProgress` bar (spent vs limit), text "đã chi X / Y (z%)",
  "Còn lại R" or (if over) "⚠️ Vượt V" in red; tap → edit. If not set → an outlined "＋ Đặt ngân sách tổng" button → startAddTotal.
- **Section "Ngân sách theo danh mục"**: list of `CategoryBudgetRow`s (emoji+name, BudgetProgress bar,
  spent/limit + percent, over = red). A "＋ Thêm ngân sách danh mục" button → startAddCategory (only if
  there are categories without a budget).
- Editor (`ModalBottomSheet`): when adding a category budget, a category picker (chips of budget-less
  EXPENSE categories) + an amount field; when editing/total, just the amount field. Save (enabled when
  amount>0 and, for new category budget, a category selected) + (when editing) a Remove button.
- Empty state when no budgets set: a friendly hint + the two add buttons.
- collectAsStateWithLifecycle.

## Progress component (`ui/components/BudgetProgress.kt`)

`@Composable fun BudgetProgress(spentMinor: Long, limitMinor: Long, modifier)`: a rounded track (Box,
light) with a filled Box whose width fraction = `(spent/limit).coerceIn(0f,1f)`; fill color = CandyGreen
when spent ≤ limit, CandyPinkDeep when over. (Simple Compose, not Canvas.)

## Spent computation
- All from `TransactionRepository.observeBetween(ledgerId, monthStart, monthEnd)` (half-open month range).
- Total spent = Σ EXPENSE amountMinor in month. Category spent = Σ EXPENSE of that categoryId in month.
- INCOME and TRANSFER never counted. Amounts stay Long; format with `Money.formatMinor`.

## Error / Edge handling
- Over budget (spent > limit): progress bar clamped to 100%, colored red, "Vượt {amount}" warning.
- limit == 0 / no budget: guard divide-by-zero (percent = 0); total card shows the "set budget" CTA.
- Category with a budget but no spend this month: 0% bar.
- Add-category picker excludes categories that already have a budget; if none remain, hide/disable the add button.
- Deleting a category (in category management) that has a budget: the budget row's category resolves to a
  fallback name; acceptable. (Cascade cleanup deferred.)

## Testing
No unit tests. Verification = build + lint green + manual emulator: open Ngân sách tab → set a total
budget → record/has expenses → progress bar reflects spend; add a category budget → its bar reflects that
category's spend; exceed a budget → bar turns red with "Vượt"; switch months → spent recomputes; data persists.

## File Structure (new/changed)
```
data/db/entity/BudgetEntity.kt (new); data/db/dao/BudgetDao.kt (new); data/db/PsyDatabase.kt (v4 + budgetDao)
data/db/mapper/Mappers.kt (+Budget); domain/model/Budget.kt (new)
domain/repository/BudgetRepository.kt (new); data/repo/BudgetRepositoryImpl.kt (new); di/RepositoryModule.kt (+bind); di/DatabaseModule.kt (+provideBudgetDao)
ui/budget/{BudgetScreen.kt, BudgetViewModel.kt} (new)
ui/components/BudgetProgress.kt (new)
ui/navigation/{Routes.kt(+BUDGET), PsyBottomBar.kt(+4th item, BarChart for stats), PsyNavHost.kt(+budget composable, bottomBarRoutes)}
gradle/libs.versions.toml + app/build.gradle.kts (+material-icons-extended)
```
