# Psy Budget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.
> **NO UNIT TESTS** (user preference). Verify each task by `./gradlew :app:assembleDebug`; final gate adds lint + manual emulator. Spec: `docs/superpowers/specs/2026-06-18-psy-budget-design.md`.

**Goal:** Monthly budgets — a total limit + optional per-category limits — shown as progress (spent vs limit) for the selected month with over-budget warnings, on a new "Ngân sách" bottom-nav tab.

**Environment for every gradle command:** `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` then `cd /Users/hoalam/Codes/psy/android && ./gradlew ...`

**Tech:** Kotlin, Compose Material3, Room (v4), Hilt, java.time. Amounts = Long minor units via `Money.formatMinor`. Candy Pop. Spent = Σ EXPENSE in selected month (INCOME/TRANSFER excluded).

---

## Task 1: Budget data layer (entity, DAO, db v4, repo, DI)

**Files:**
- `domain/model/Budget.kt` (new):
  ```kotlin
  package com.psy.domain.model
  data class Budget(val id: Long = 0, val ledgerId: Long, val categoryId: Long?, val amountMinor: Long)
  ```
- `data/db/entity/BudgetEntity.kt` (new):
  ```kotlin
  package com.psy.data.db.entity
  import androidx.room.Entity
  import androidx.room.Index
  import androidx.room.PrimaryKey
  @Entity(tableName = "budgets", indices = [Index("ledgerId")])
  data class BudgetEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val ledgerId: Long,
      val categoryId: Long?,   // null = total budget
      val amountMinor: Long,
  )
  ```
- `data/db/dao/BudgetDao.kt` (new):
  ```kotlin
  package com.psy.data.db.dao
  import androidx.room.*
  import com.psy.data.db.entity.BudgetEntity
  import kotlinx.coroutines.flow.Flow
  @Dao
  interface BudgetDao {
      @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(budget: BudgetEntity): Long
      @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId") fun observeAll(ledgerId: Long): Flow<List<BudgetEntity>>
      @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND categoryId IS NULL LIMIT 1") suspend fun findTotal(ledgerId: Long): BudgetEntity?
      @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND categoryId = :categoryId LIMIT 1") suspend fun findByCategory(ledgerId: Long, categoryId: Long): BudgetEntity?
      @Delete suspend fun delete(budget: BudgetEntity)
  }
  ```
- `data/db/PsyDatabase.kt`: add `BudgetEntity::class` to entities, bump `version = 4`, add `abstract fun budgetDao(): BudgetDao`.
- `data/db/mapper/Mappers.kt`: add `fun BudgetEntity.toDomain() = Budget(id, ledgerId, categoryId, amountMinor)` and `fun Budget.toEntity() = BudgetEntity(id, ledgerId, categoryId, amountMinor)`.
- `domain/repository/BudgetRepository.kt` (new):
  ```kotlin
  package com.psy.domain.repository
  import com.psy.domain.model.Budget
  import kotlinx.coroutines.flow.Flow
  interface BudgetRepository {
      fun observeAll(ledgerId: Long): Flow<List<Budget>>
      suspend fun setBudget(ledgerId: Long, categoryId: Long?, amountMinor: Long)
      suspend fun removeBudget(budget: Budget)
  }
  ```
- `data/repo/BudgetRepositoryImpl.kt` (new): `@Inject constructor(private val dao: BudgetDao)`. `observeAll` maps to domain. `setBudget`: `val existing = if (categoryId == null) dao.findTotal(ledgerId) else dao.findByCategory(ledgerId, categoryId); dao.upsert(BudgetEntity(id = existing?.id ?: 0, ledgerId, categoryId, amountMinor))`. `removeBudget` → `dao.delete(budget.toEntity())`.
- `di/DatabaseModule.kt`: add `@Provides fun provideBudgetDao(db: PsyDatabase): BudgetDao = db.budgetDao()`.
- `di/RepositoryModule.kt`: add `@Binds @Singleton abstract fun bindBudgetRepo(impl: BudgetRepositoryImpl): BudgetRepository`.

- [ ] Step 1: Implement all above.
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL (Room regenerates v4); `./gradlew :app:testDebugUnitTest` → green.
- [ ] Step 3: Commit `feat(data): budget entity/dao/repo + db v4`.

---

## Task 2: Navigation — material-icons-extended + 4th tab

**Files:**
- `gradle/libs.versions.toml`: add library `androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }` (version comes from the Compose BOM — no version.ref needed since it's a BOM-managed artifact; declare without version and rely on the BOM platform already applied).
- `app/build.gradle.kts`: add `implementation(libs.androidx.compose.material.icons.extended)`.
- `ui/navigation/Routes.kt`: add `const val BUDGET = "budget"`.
- `ui/navigation/PsyBottomBar.kt`: add a 4th `NavigationBarItem` (Routes.BUDGET, "Ngân sách", `Icons.Default.Savings`). Change the Stats item icon from the placeholder to `Icons.Default.BarChart`.
- `ui/navigation/PsyNavHost.kt`: include `Routes.BUDGET` in the `bottomBarRoutes` set; add `composable(Routes.BUDGET){ BudgetScreen() }`.
- Stub (Task 3 replaces): `ui/budget/BudgetScreen.kt` `@Composable fun BudgetScreen()` → Scaffold + TopAppBar("Ngân sách") + centered Text("Ngân sách — coming soon"). `@OptIn(ExperimentalMaterial3Api::class)`.

- [ ] Step 1: Implement (add dep, routes, 4th tab + BarChart, NavHost, stub).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL (icons-extended resolves; Savings + BarChart exist there). `./gradlew :app:testDebugUnitTest` → green.
- [ ] Step 3: Commit `feat(ui): add Ngân sách bottom-nav tab + material-icons-extended`.

---

## Task 3: BudgetProgress + Budget screen + ViewModel

**Files:**
- `ui/components/BudgetProgress.kt` (new): `@Composable fun BudgetProgress(spentMinor: Long, limitMinor: Long, modifier: Modifier = Modifier)`. A rounded track `Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(track color))` containing a filled `Box(Modifier.fillMaxWidth(fraction = if (limitMinor>0) (spentMinor.toFloat()/limitMinor).coerceIn(0f,1f) else 0f).fillMaxHeight().background(if (spentMinor>limitMinor && limitMinor>0) CandyPinkDeep else CandyGreen))`.
- `ui/budget/BudgetViewModel.kt` (new): `@HiltViewModel`, inject BudgetRepository, TransactionRepository, CategoryRepository, LedgerRepository.
  - `selectedMonth = MutableStateFlow(YearMonth.now())`; `prevMonth()/nextMonth()`.
  - Resolve active ledger → ledgerId + currency.
  - Reactively combine budgets(ledgerId), month EXPENSE txns (`observeBetween` half-open month range), categories(map id→Category) into `BudgetUiState`:
    - totalSpent = Σ EXPENSE amountMinor in month.
    - total budget = budgets.firstOrNull { categoryId == null }; if present expose `TotalBudget(limitMinor, spentMinor = totalSpent, percent)`, else null.
    - categoryBudgets = budgets.filter { categoryId != null }.map { b -> CategoryBudgetItem(category = catMap[b.categoryId], limitMinor = b.amountMinor, spentMinor = Σ EXPENSE of that category in month, percent) }.sortedByDescending { percent }.
  - Editor state: `editorOpen`, `editingBudget: Budget?`, `editorMode` (TOTAL or CATEGORY), `editorCategoryId: Long?`, `draftAmountText: String`, `availableCategories: List<Category>` (EXPENSE categories without an existing budget — for the add-category picker).
  - Functions: `startAddTotal()`, `startAddCategory()`, `startEdit(budget)`, `onAmountChange`, `selectEditorCategory(id)`, `closeEditor()`, `saveEditor()` (parse amount as minor units: digits → Long; for VND fractionDigits 0 the integer IS minor; call `repo.setBudget(ledgerId, categoryId-or-null, amount)`; close), `removeEditor()` (repo.removeBudget(editingBudget); close).
  - Expose `StateFlow<BudgetUiState>` (monthLabel, currency, total?, categoryBudgets, editor fields, loading).
- `ui/budget/BudgetScreen.kt` (REPLACE stub): `BudgetScreen(viewModel = hiltViewModel())`. Scaffold + TopAppBar("Ngân sách"). Column (scroll):
  - `MonthSelector`.
  - **Total budget card** (Candy gradient or outlined): if total != null → "Ngân sách tổng", `BudgetProgress(spent, limit)`, text "Đã chi {spent} / {limit} ({pct}%)" and "Còn lại {limit-spent}" OR (if over) "⚠️ Vượt {spent-limit}" in CandyPinkDeep; clickable → startEdit(total budget). If null → an outlined Button "＋ Đặt ngân sách tổng" → startAddTotal.
  - **Section "Ngân sách theo danh mục"** + each `CategoryBudgetItem`: a card row with category emoji+name, `BudgetProgress`, "Đã chi {spent} / {limit} ({pct}%)", over → red "Vượt". Tap → startEdit. Below: a Button "＋ Thêm ngân sách danh mục" → startAddCategory (disabled/hidden if availableCategories empty).
  - Editor (`ModalBottomSheet` when editorOpen): if editorMode == CATEGORY and adding → a category picker (FlowRow of chips from availableCategories, select → selectEditorCategory); an amount `OutlinedTextField` (number); Save button (enabled when amount>0 and (mode==TOTAL or editing or a category selected)); when editing, a "Xoá" TextButton → removeEditor.
  - Empty state when total == null && categoryBudgets empty: a friendly hint + the two add buttons.
  - collectAsStateWithLifecycle. `@OptIn(ExperimentalMaterial3Api::class)`.

- [ ] Step 1: Implement BudgetProgress + VM + screen (replace stub).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL; `./gradlew :app:testDebugUnitTest` → green.
- [ ] Step 3: Commit `feat(ui): budget screen (total + per-category progress, editor)`.

---

## Task 4: Verification gate (build + lint + manual)

- [ ] Step 1: `./gradlew :app:assembleDebug :app:lintDebug` → SUCCESSFUL, 0 lint errors.
- [ ] Step 2: Manual run on Pixel_10_Pro:
  1. Bottom nav now has a 4th tab "Ngân sách" (Savings icon); Stats tab shows a BarChart icon.
  2. Ngân sách tab: empty state with add buttons. Set a total budget (e.g. 5,000,000).
  3. Record some expenses on Home (or pre-existing). The total budget bar reflects spend; "Còn lại" updates.
  4. Add a category budget (e.g. Ăn uống 1,000,000); its bar reflects that category's spend.
  5. Make spend exceed a budget → bar turns red + "Vượt".
  6. Switch months ◀/▶ → spent recomputes (budget limits persist).
  7. Kill & reopen → budgets persist (Room v4).
- [ ] Step 3: `git --no-pager log --oneline && git status -s` clean.

---

## Self-Review Notes
- Spec coverage: budget data layer + v4 (Task 1); 4th tab + icons-extended (Task 2); total + per-category progress UI + editor + spent computation (Task 3). All covered.
- No tests; verification = compile + lint + manual.
- Type consistency: `Budget(categoryId: Long?)` threads entity↔domain↔repo; `setBudget(ledgerId, categoryId?, amount)` matches VM calls; `BudgetUiState`/`CategoryBudgetItem`/`TotalBudget` defined in the VM and consumed by the screen; `Routes.BUDGET` shared by bottom bar + NavHost.
- Spent = EXPENSE only (INCOME/TRANSFER excluded), half-open month range matching observeBetween. DB v4 destructive (no real data).
