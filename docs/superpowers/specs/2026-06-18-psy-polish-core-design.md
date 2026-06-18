# Psy — Polish Core Design Spec

**Date:** 2026-06-18
**Status:** Approved (brainstorming)
**Author:** hoalam

## Overview

Polish the core bookkeeping app (income/expense loop, already merged to `main`) before
building the larger feature plans. Adds the customization and navigation pieces that make
the core genuinely usable: a Settings hub, category & account management, transfer-type
transactions, and photo attachment. Builds on the existing single-module Kotlin / MVVM /
Hilt / Room / Compose architecture (`com.psy`, packages `data/domain/ui/di`).

**No unit tests** (user preference — too token-expensive). Verify by compiling
(`./gradlew :app:assembleDebug :app:lintDebug`) and manual run on the Pixel_10_Pro emulator.

## Scope (approved)

In scope:
1. Settings hub + navigation entry point
2. Category management (add / edit / delete)
3. Account management (add / edit)
4. Transfer-type transactions
5. Photo attachment to transactions
6. Auto-include polish: account icon emoji fix + extra default account; delete-confirmation dialogs; shared emoji+color picker component

Deferred (NOT in this spec):
- Category reorder (drag); account delete; multi-ledger switching; ledger management screen
- Stats & Calendar, Budget, Backup/Auth, Theming & Lock (their own plans, after this)

## Data Model Changes (Room v3, destructive migration — dev-only)

`TxType` gains `TRANSFER` → `enum class TxType { INCOME, EXPENSE, TRANSFER }`.

`TransactionEntity` / `Transaction` (domain) change:
- `categoryId: Long` → `categoryId: Long?` (transfers have no category)
- add `toAccountId: Long?` (destination account for transfers; null otherwise)
- add `photoUri: String?` (local file path of attached photo; null otherwise)

`PsyDatabase` version **2 → 3**, keep `fallbackToDestructiveMigration(dropAllTables = true)`
(still dev-only; seeded data is re-created on wipe). Mappers updated for the new/nullable fields.

`TransactionDao`: existing queries unchanged except they now select the new columns automatically
(SELECT *). No new query needed for transfers in the month list (they appear in `observeBetween`).

## Navigation

Add a `Scaffold` TopAppBar to **Home** with a trailing settings icon (⚙️) → navigate to `Routes.SETTINGS`.
New routes in `ui/navigation/Routes.kt`: `SETTINGS = "settings"`, `MANAGE_CATEGORIES = "manageCategories"`,
`MANAGE_ACCOUNTS = "manageAccounts"`. `PsyNavHost` adds `composable` destinations for each. Back arrows
pop the stack. The Settings hub is the extensible home for future entries (Backup, Theme).

## Feature Designs

### 1. Settings hub — `ui/settings/SettingsScreen.kt`
A simple list screen: rows "Quản lý danh mục" → MANAGE_CATEGORIES, "Quản lý tài khoản" → MANAGE_ACCOUNTS.
Each row: leading icon, title, chevron. No ViewModel needed (pure navigation); takes nav callbacks.

### 2. Category management — `ui/manage/category/`
- `ManageCategoriesScreen` + `ManageCategoriesViewModel`.
- Tabs/segmented toggle Thu/Chi (CategoryType); list of categories for the selected type (observe via
  `CategoryRepository.observeByType`).
- "Add" FAB and tap-to-edit open a `CategoryEditor` (bottom sheet or dialog): fields name (text),
  icon (emoji via shared picker), color (palette via shared picker), and for new ones the type is the
  active tab. Save → `CategoryRepository.upsert` (sortOrder = current max + 1 for new).
- Delete: a delete action with a confirmation dialog → remove the category. Existing transactions that
  referenced it keep their (now-dangling) categoryId and render the existing fallback (📦 / "—") on Home.
- Repository addition: `CategoryRepository.delete(category)` + `CategoryDao @Delete`.

### 3. Account management — `ui/manage/account/`
- `ManageAccountsScreen` + `ManageAccountsViewModel`.
- List all accounts (`AccountRepository.observeAll`). Add FAB + tap-to-edit open an `AccountEditor`
  (name, type dropdown CASH/BANK/CREDIT/ASSET, icon emoji, color). Save → `AccountRepository.upsert`.
- No delete in this spec.
- Seeder fix: change the default "Tiền mặt" account icon from `"cash"` to an emoji (💵) and add a
  second default account "Ngân hàng" (🏦, type BANK). (Re-seed happens on the destructive v3 wipe.)

### 4. Transfer-type — extend `ui/addedit/`
- `AddEditTransactionViewModel`/`Screen` gain a third segment "Chuyển khoản" (TxType.TRANSFER).
- When type = TRANSFER: hide the category grid; show two account selectors — "Từ tài khoản" and
  "Đến tài khoản" (must be different; `canSave` requires both set, distinct, amount>0). categoryId = null.
- Save builds a Transaction with type=TRANSFER, accountId = from, toAccountId = to, categoryId = null.
- Home/HomeViewModel: transfers are EXCLUDED from income and expense sums (net unaffected). A transfer row
  renders as "Tên-từ → Tên-đến" with the amount in a neutral color (e.g. onSurface), no +/- sign emphasis.
  `TxRow` gains optional `toAccountName: String?` and the row composable branches on `type == TRANSFER`.

### 5. Photo attachment — extend `ui/addedit/` (+ a small storage helper)
- Add an "Đính kèm ảnh" control on the Add/Edit screen using the Android Photo Picker
  (`ActivityResultContracts.PickVisualMedia` / `rememberLauncherForActivityResult`) — no runtime permission.
- On pick: copy the selected image into app internal storage (`context.filesDir/photos/<timestamp>.jpg`)
  via a `PhotoStorage` helper (in `data/photo/`), store the absolute file path in `photoUri`.
- Show a thumbnail (Coil `AsyncImage`) on the Add/Edit screen once attached, with a remove (×) button.
- Home `TxRow`: if `photoUri != null`, show a small thumbnail/indicator. (Full-screen photo view deferred.)

### 6. Shared component — `ui/components/IconColorPicker.kt`
A reusable composable (or two: `EmojiPicker`, `ColorPicker`) used by both editors: a grid of curated
emoji to pick an icon, and a row of Candy-palette colors (packed ARGB Long) to pick a color. Keep the
emoji set curated (~30–40 common finance/life emoji).

## Error / Edge Handling
- Transfer with from == to: blocked (canSave false); show a hint.
- Deleting a category in use: allowed; dangling references fall back gracefully on Home (existing behavior).
- Photo copy failure: catch, show a non-blocking message, leave photoUri null (don't block save).
- Destructive v3 migration: acceptable (no real user data); seeder re-creates defaults.

## Testing
No unit tests. Verification = `./gradlew :app:assembleDebug :app:lintDebug` green (0 lint errors) +
manual emulator run: open Settings → add a custom category and account → record an expense with the new
category + a photo → record a transfer between two accounts → confirm Home shows all three correctly
(expense affects balance, transfer doesn't) and data persists across app restart.

## File Structure (new/changed)
```
ui/navigation/{Routes.kt(+), PsyNavHost.kt(+)}
ui/home/{HomeScreen.kt(+TopAppBar settings icon, transfer row, photo thumb), HomeViewModel.kt(+transfer exclusion, toAccountName)}
ui/settings/SettingsScreen.kt                        (new)
ui/manage/category/{ManageCategoriesScreen.kt, ManageCategoriesViewModel.kt}   (new)
ui/manage/account/{ManageAccountsScreen.kt, ManageAccountsViewModel.kt}        (new)
ui/components/IconColorPicker.kt                     (new: EmojiPicker + ColorPicker)
ui/addedit/{AddEditTransactionScreen.kt(+transfer mode, photo), AddEditTransactionViewModel.kt(+transfer, photo)}
data/photo/PhotoStorage.kt                           (new)
data/db/entity/TransactionEntity.kt(+toAccountId, photoUri; categoryId nullable)
data/db/PsyDatabase.kt(version 3)
data/db/mapper/Mappers.kt(+new fields)
data/db/dao/CategoryDao.kt(+@Delete)
domain/model/{Transaction.kt(+fields), Enums.kt(+TRANSFER)}
domain/repository/CategoryRepository.kt(+delete)
data/repo/CategoryRepositoryImpl.kt(+delete)
data/seed/DefaultDataSeeder.kt(emoji icon + 2nd account)
```
