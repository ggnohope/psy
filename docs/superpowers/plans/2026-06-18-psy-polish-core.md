# Psy Polish Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.
> **NO UNIT TESTS** (user preference). Verify each task by compiling (`./gradlew :app:assembleDebug`); final gate adds lint + manual emulator run. Two-stage (spec + quality) review per task still applies.
> Source spec: `docs/superpowers/specs/2026-06-18-psy-polish-core-design.md` — read it for full rationale.

**Goal:** Polish the core: a Settings hub, category & account management, transfer-type transactions, and photo attachment — on top of the merged income/expense loop.

**Environment for every gradle command:** `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` then `cd /Users/hoalam/Codes/psy/android && ./gradlew ...`

**Tech:** Kotlin, Compose Material3, Room (v3), Hilt, Navigation Compose, Coil, Android Photo Picker. Amounts = Long minor units via `com.psy.domain.util.Money`. Candy Pop theme.

---

## Task 1: Data model + DB v3 (transfer, photo, nullable category)

**Files:**
- `domain/model/Enums.kt`: `enum class TxType { INCOME, EXPENSE, TRANSFER }`.
- `domain/model/Transaction.kt`: change `categoryId: Long` → `categoryId: Long?`; add `toAccountId: Long? = null`; add `photoUri: String? = null`.
- `data/db/entity/TransactionEntity.kt`: `categoryId: Long?`; add `toAccountId: Long?`; add `photoUri: String?` (add an `Index("toAccountId")`).
- `data/db/PsyDatabase.kt`: bump `version = 3`.
- `data/db/mapper/Mappers.kt`: update Transaction↔Entity both ways for the new/nullable fields (TxType.valueOf still fine; null categoryId maps straight through).
- `data/db/dao/CategoryDao.kt`: add `@Delete suspend fun delete(category: CategoryEntity)`.
- `domain/repository/CategoryRepository.kt`: add `suspend fun delete(category: Category)`.
- `data/repo/CategoryRepositoryImpl.kt`: implement `delete` via `dao.delete(category.toEntity())`.
- `data/seed/DefaultDataSeeder.kt`: change default account "Tiền mặt" icon `"cash"` → `"💵"`; add a second default account `Account(name="Ngân hàng", type=AccountType.BANK, icon="🏦", color=0xFF7FD8FF)` (still guarded by `accountRepo.count() == 0`, seed both).

- [ ] Step 1: Apply all the above edits.
- [ ] Step 2: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (Room regenerates for v3). Also `./gradlew :app:testDebugUnitTest` (existing tests stay green — note the Money/LedgerDao tests don't touch these fields).
- [ ] Step 3: Commit `feat(data): db v3 — transfer (toAccountId), photo (photoUri), nullable category; category delete; emoji account icons`.

---

## Task 2: Navigation + Settings hub

**Files:**
- `ui/navigation/Routes.kt`: add `SETTINGS = "settings"`, `MANAGE_CATEGORIES = "manageCategories"`, `MANAGE_ACCOUNTS = "manageAccounts"`.
- `ui/navigation/PsyNavHost.kt`: add `composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }, onManageCategories = { nav.navigate(Routes.MANAGE_CATEGORIES) }, onManageAccounts = { nav.navigate(Routes.MANAGE_ACCOUNTS) }) }`, plus `composable(Routes.MANAGE_CATEGORIES) { ManageCategoriesScreen(onBack = { nav.popBackStack() }) }` and `composable(Routes.MANAGE_ACCOUNTS) { ManageAccountsScreen(onBack = { nav.popBackStack() }) }`. Pass `onSettingsClick = { nav.navigate(Routes.SETTINGS) }` into `HomeScreen`.
- `ui/home/HomeScreen.kt`: add a Material3 `TopAppBar` (title "Psy" or month) with a trailing `IconButton` (Icons.Default.Settings) → `onSettingsClick`. Keep the balance card + list below it.
- `ui/settings/SettingsScreen.kt` (new): `@Composable fun SettingsScreen(onBack, onManageCategories, onManageAccounts)` — Scaffold + TopAppBar (back arrow) + a list of rows: "Quản lý danh mục" → onManageCategories, "Quản lý tài khoản" → onManageAccounts. Each row: leading icon, title, trailing chevron. No ViewModel.

(`ManageCategoriesScreen`/`ManageAccountsScreen` are placeholders here if Tasks 3/4 not yet done — a stub composable that compiles; Tasks 3 & 4 implement them. If implementing sequentially, create minimal stubs so Task 2 compiles, replaced in 3/4.)

- [ ] Step 1: Implement routes, NavHost wiring, Home TopAppBar, SettingsScreen (+ compiling stubs for the two manage screens).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): settings hub + nav entry from Home`.

---

## Task 3: Shared picker + Category management

**Files:**
- `ui/components/IconColorPicker.kt` (new): `EmojiPicker(selected: String, onPick: (String) -> Unit)` — a `LazyVerticalGrid` of a curated emoji set (~36 finance/life emoji: 🍜🚌🛍️🧾🎮💊📦💰🎁🏠🚗☕🍺👕💊🏥🎬📱✈️🎓🐶🎁💵🏦💳🪙📈🎀🧴🍔🍰🚕⛽🏋️🎵🛒). `ColorPicker(selected: Long, onPick: (Long) -> Unit)` — a Row of swatches from a small Candy palette (e.g. CandyViolet, CandySky, CandyPink, CandyPinkDeep, CandyGreen + a few pastels), each a colored circle, selected ringed.
- `ui/manage/category/ManageCategoriesViewModel.kt` (new): `@HiltViewModel`, inject `CategoryRepository`. State: selected `CategoryType` tab (default EXPENSE), `categories: List<Category>` (observe `observeByType(type)` reacting to tab), plus an editor state (editing category or null, draft name/icon/color). Functions: `selectTab`, `startAdd`, `startEdit(category)`, `onNameChange/onIconChange/onColorChange`, `saveEditor()` (upsert; new → sortOrder = (max existing sortOrder)+1; keep type = active tab), `requestDelete(category)`/`confirmDelete()`.
- `ui/manage/category/ManageCategoriesScreen.kt` (new): Scaffold + TopAppBar (back) + Thu/Chi segmented toggle + list of category rows (emoji + name, tap → edit, trailing delete icon → confirm dialog) + Add FAB. Editor as a `ModalBottomSheet` or `AlertDialog`: name TextField, `EmojiPicker`, `ColorPicker`, Save button. Delete shows an `AlertDialog` confirm.

- [ ] Step 1: Implement the picker + ViewModel + screen (replace the Task 2 stub for categories).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): category management with shared emoji/color picker`.

---

## Task 4: Account management

**Files:**
- `ui/manage/account/ManageAccountsViewModel.kt` (new): `@HiltViewModel`, inject `AccountRepository`. State: `accounts: List<Account>` (observeAll) + editor state (editing account or null, draft name/type/icon/color). Functions: `startAdd`, `startEdit`, `onNameChange/onTypeChange/onIconChange/onColorChange`, `saveEditor()` (upsert). No delete.
- `ui/manage/account/ManageAccountsScreen.kt` (new): Scaffold + TopAppBar (back) + list of account rows (emoji + name + type label, tap → edit) + Add FAB. Editor (ModalBottomSheet/AlertDialog): name TextField, account-type selector (CASH/BANK/CREDIT/ASSET — a dropdown or segmented), reuse `EmojiPicker` + `ColorPicker`, Save. (Reuses `ui/components/IconColorPicker` from Task 3.)

- [ ] Step 1: Implement (replace the Task 2 stub for accounts).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): account management (add/edit)`.

---

## Task 5: Transfer-type transactions

**Files:**
- `ui/addedit/AddEditTransactionViewModel.kt`: add TRANSFER handling. State gains `toAccountId: Long?`. When `type == TRANSFER`: category not required/cleared (categoryId = null); require `selectedAccountId` (from) and `toAccountId` (to) set AND distinct; `canSave = amount>0 && from!=null && to!=null && from!=to`. `onTypeChange` to TRANSFER hides category. `save` builds Transaction(type=TRANSFER, accountId=from, toAccountId=to, categoryId=null). Edit prefill loads toAccountId for transfers.
- `ui/addedit/AddEditTransactionScreen.kt`: the type segmented control gains a 3rd option "Chuyển khoản". When TRANSFER: hide the category grid; show two account selectors labeled "Từ tài khoản" / "Đến tài khoản" (reuse the account-chip row twice, the second bound to toAccountId). Hint when from==to.
- `ui/home/HomeViewModel.kt`: exclude TRANSFER from income/expense sums (only INCOME adds, EXPENSE subtracts; TRANSFER ignored for totals). `TxRow` gains `toAccountName: String?`; for transfer rows set it.
- `ui/home/HomeScreen.kt`: in the row composable, if `type == TxType.TRANSFER` render "fromAccount → toAccount" with a neutral amount color (onSurface) and no +/- emphasis; else existing income/expense rendering.

- [ ] Step 1: Implement.
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): transfer-type transactions (from→to, excluded from income/expense)`.

---

## Task 6: Photo attachment

**Files:**
- `data/photo/PhotoStorage.kt` (new): `@Inject constructor(@ApplicationContext context)` with `suspend fun savePicked(uri: Uri): String` — copy the content-uri stream into `context.filesDir/photos/<timestampOrUuid>.jpg` and return the absolute path; and `fun delete(path: String)`. Use the passed-in timestamp/uuid (avoid Date.now in ctor; generate a name from the input uri hash + an incrementing counter or accept a name param). Wrap IO in try/catch; on failure throw so caller can handle.
- `ui/addedit/AddEditTransactionViewModel.kt`: inject `PhotoStorage`. State gains `photoUri: String?`. `onPickPhoto(uri: Uri)` → viewModelScope: copy via PhotoStorage, set photoUri (catch failure → emit a transient error, leave null). `onRemovePhoto()` → clear (optionally delete file). `save` includes photoUri.
- `ui/addedit/AddEditTransactionScreen.kt`: add an "Đính kèm ảnh" button using `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` with `PickVisualMediaRequest(ImageOnly)`. On result non-null → `viewModel.onPickPhoto(uri)`. If `state.photoUri != null` show a Coil `AsyncImage` thumbnail with a remove (×) overlay.
- `ui/home/HomeScreen.kt`: in `TxRow`, if `photoUri != null` show a small Coil thumbnail (or a 📎 indicator). `HomeViewModel.TxRow` gains `photoUri: String?`.

- [ ] Step 1: Implement.
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): photo attachment via Android Photo Picker`.

---

## Task 7: Verification gate (build + lint + manual emulator)

- [ ] Step 1: `./gradlew :app:assembleDebug :app:lintDebug` → BUILD SUCCESSFUL, 0 lint errors.
- [ ] Step 2: Manual run on Pixel_10_Pro emulator (install app-debug.apk):
  1. Home shows a TopAppBar with ⚙️; tap it → Settings hub.
  2. Quản lý tài khoản → add an account (e.g. "Ví Momo", type ASSET, emoji, color) → appears in list.
  3. Quản lý danh mục → Chi tab → add a custom category (name, emoji, color) → appears; edit it; delete one with confirm.
  4. Home → + → Expense with the new custom category + attach a photo → Save → Home shows it with thumbnail; balance updated.
  5. Home → + → Chuyển khoản → from "Tiền mặt" to "Ngân hàng", amount 100000 → Save → Home shows "Tiền mặt → Ngân hàng" neutral; income/expense/net unchanged by the transfer.
  6. Kill & reopen → data persists.
- [ ] Step 3: `git --no-pager log --oneline && git status -s` clean.

---

## Self-Review Notes
- Spec coverage: Settings hub + nav (Task 2); category mgmt (Task 3); account mgmt + seeder icon fix (Tasks 1,4); transfer (Tasks 1,5); photo (Tasks 1,6); shared picker (Task 3). All spec items covered.
- No tests; verification is compile + lint + the manual checklist.
- Type consistency: `categoryId: Long?` threads through entity/model/mapper/VMs; `toAccountId`/`photoUri` added consistently; `TxRow` gains `toAccountName`+`photoUri` used by HomeScreen; `Routes` constants shared by NavHost + Home + Settings.
- DB v3 destructive (no real data) — re-seeds defaults (now with emoji icons + 2nd account).
