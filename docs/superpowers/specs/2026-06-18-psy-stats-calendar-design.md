# Psy — Stats & Calendar Design Spec

**Date:** 2026-06-18
**Status:** Approved (brainstorming)
**Author:** hoalam

## Overview

Add a Statistics screen and a Calendar month view to Psy, plus a bottom navigation shell to
host them alongside Home. Builds on the merged core + polish (offline-first Kotlin/Compose/Room/
Hilt/MVVM, `com.psy`, Candy Pop theme). Charts are drawn with **Compose Canvas** (no charting
library) for control over the cute aesthetic and to avoid dependency-compat risk on the very new
toolchain (AGP 9.2 / Kotlin 2.2.10 / Compose BOM 2026.02).

**No unit tests** (user preference). Verify by `./gradlew :app:assembleDebug :app:lintDebug` (0 lint
errors) + manual emulator run.

## Scope (approved)

In scope:
1. Bottom navigation shell: 🏠 Trang chủ / 📊 Thống kê / 📅 Lịch (Settings stays on Home's top bar).
2. Statistics screen: summary card, donut pie by category (Chi/Thu toggle, default Chi), top-10
   spend list, 6-month income/expense trend bars.
3. Calendar month view: month grid (week starts Monday), per-day income/expense indicators, tap a
   day → that day's transaction list.
4. Shared month selector (← Tháng M/YYYY →) for Stats and Calendar.
5. Canvas chart components: DonutChart, TrendBars.

Deferred: date-range (custom) selection; account-level stats; export; pie drill-down; charts for
transfers; Budget/Backup/Theming (their own plans).

Conventions (approved defaults): week starts **Monday**; pie/top default to **EXPENSE** with a toggle
to INCOME; trend window = **last 6 months**; **TRANSFER excluded** from all income/expense totals.

## Navigation shell

Restructure the app root so a `Scaffold` with a Material3 `NavigationBar` wraps the NavHost. Three
top-level routes are bottom-nav destinations: `HOME`, `STATS`, `CALENDAR`. The bottom bar is shown
ONLY when the current destination is one of these three (derive from the current back-stack entry's
route); it is hidden on `addEdit`, `settings`, `manageCategories`, `manageAccounts` (pushed screens).
Bottom-nav clicks use `navigate(route){ launchSingleTop = true; popUpTo(graph start){ saveState }; restoreState }`
so tab switches don't stack. Settings remains reachable via the ⚙️ icon on Home's TopAppBar.

New routes in `ui/navigation/Routes.kt`: `STATS = "stats"`, `CALENDAR = "calendar"`.
`PsyNavHost` adds `composable(STATS){ StatsScreen() }` and `composable(CALENDAR){ CalendarScreen() }`.
A new `ui/navigation/PsyBottomBar.kt` renders the NavigationBar (3 items with icon + label).

## Shared period selector

`ui/components/MonthSelector.kt`: a row "← Tháng M/YYYY →" with prev/next IconButtons; takes a
`YearMonth` and `onChange`. Used by Stats and Calendar. Each screen's ViewModel owns its own selected
`YearMonth` (defaults to current month). Month range is computed half-open `[firstOfMonth, firstOfNextMonth)`
to match `TransactionDao.observeBetween`.

## Statistics screen (`ui/stats/`)

`StatsViewModel` (@HiltViewModel; inject TransactionRepository, CategoryRepository, LedgerRepository):
- Owns `selectedMonth: MutableStateFlow<YearMonth>` and a `pieMode: MutableStateFlow<TxType>` (EXPENSE/INCOME).
- Resolves active ledger (firstOrNull) + currency.
- For the selected month: observe `observeBetween(ledgerId, monthStart, monthEnd)` + categories; compute:
  - **Summary**: total income, total expense, net, average-per-day (expense / days elapsed in month, guard /0).
  - **Pie slices**: group the `pieMode` transactions by categoryId → sum; build slices (category name, color, amount, percent); ignore null-category (transfers excluded since pieMode is INCOME/EXPENSE only).
  - **Top 10**: the same category sums sorted desc, take 10, with percent.
- For the **trend**: observe a single wider range `[startOf(month-5), monthEnd)` and bucket by YearMonth into 6 `MonthBars(label, income, expense)` (TRANSFER excluded).
- Expose `StateFlow<StatsUiState>` (monthLabel, currency, summary, pieMode, slices, top list, trend list, loading).

`StatsScreen` (`StatsScreen(viewModel = hiltViewModel())`): a scrollable column:
- `MonthSelector`.
- Summary card (Candy gradient): income / expense / net / avg-per-day.
- Chi/Thu segmented toggle → `pieMode`.
- `DonutChart` of slices + a legend (color dot + name + percent).
- "Top chi tiêu" section: rows with category emoji+name, a horizontal proportion bar (Box width = percent), amount + percent.
- "Xu hướng 6 tháng" section: `TrendBars`.
- Empty states when the month has no data.

## Calendar screen (`ui/calendar/`)

`CalendarViewModel` (@HiltViewModel; inject TransactionRepository, CategoryRepository, AccountRepository, LedgerRepository):
- Owns `selectedMonth: MutableStateFlow<YearMonth>` and `selectedDay: MutableStateFlow<LocalDate?>` (default today if in month, else first day).
- Observe `observeBetween(month)` → bucket per `LocalDate` into `DayCell(date, incomeMinor, expenseMinor)` for all days in the month; also expose the selected day's transaction rows (resolve category/account names; transfers rendered "A → B" neutrally like Home).
- Expose `StateFlow<CalendarUiState>` (monthLabel, currency, weeks: List<List<DayCell?>> with leading/trailing null padding so the grid aligns to Monday-start, selectedDay, dayTransactions).

`CalendarScreen`:
- `MonthSelector`.
- Weekday header row (T2 T3 T4 T5 T6 T7 CN).
- Month grid: 6 rows × 7 cols of day cells. Each cell: day number; if it has expense, a small red amount/dot; if income, a small green one. Selected day highlighted (Candy primary ring). Empty padding cells blank. Tap a cell with a date → set selectedDay.
- Below the grid: the selected day's transaction list (reuse the Home row style; empty state if none).

## Canvas chart components (`ui/components/charts/`)

- `DonutChart.kt`: `@Composable fun DonutChart(slices: List<PieSlice>, modifier, centerLabel: String)`. Draw each slice with `drawArc(useCenter=false, stroke=Stroke(width, cap=Round))` around a ring; sweep proportional to value/total; colors from slice.color (Long→Color). Show `centerLabel` (e.g. total) in the hole via a Text overlay (Box). Animate sweep with `animateFloatAsState` (simple grow-in). Handle empty (total 0) → a muted full ring + "—".
  `data class PieSlice(val label: String, val amountMinor: Long, val color: Long)`.
- `TrendBars.kt`: `@Composable fun TrendBars(months: List<MonthBars>, currency, modifier)`. For each month draw two bars (income green, expense pink) scaled to the max value across the window; month label under each pair; a small legend. Canvas `drawRoundRect` per bar.
  `data class MonthBars(val label: String, val incomeMinor: Long, val expenseMinor: Long)`.

## Data flow

All from existing repositories — no new entities/DAOs. `TransactionRepository.observeBetween(ledgerId, start, end)`
serves month ranges (pie/top/calendar) and the 6-month trend (one wide query, bucketed in the VM by
`Instant.ofEpochMilli(date).atZone(systemZone).toLocalDate()`). Category/account name+color/icon resolved
from `observeAll`/`observeByType` maps. Amounts stay `Long` minor units; format with `Money.formatMinor`.

## Error / Edge handling
- Empty month: summary shows zeros; pie shows muted ring + "—"; top/trend/calendar show friendly empties.
- avg-per-day: divide expense by days-elapsed (current month) or days-in-month (past months); guard divide-by-zero.
- TRANSFER excluded from all income/expense aggregations; transfer rows in the day list render neutrally.
- Null categoryId rows never appear in pie/top (pieMode is INCOME/EXPENSE; transfers have null category and are excluded).
- Month with >31 transactions / many categories: lists are bounded (top-10); pie groups all categories (consider an "Khác" merge if >8 slices — optional, can show all).

## Testing
No unit tests. Verification = build + lint green + manual emulator: switch months on Stats (pie/top/
trend update, Chi/Thu toggle works), Calendar grid shows per-day amounts and tapping a day lists its
transactions, bottom nav switches Home/Thống kê/Lịch and preserves state, transfers don't skew totals.

## File Structure (new/changed)
```
ui/navigation/{Routes.kt(+STATS,CALENDAR), PsyNavHost.kt(restructure: Scaffold+NavigationBar), PsyBottomBar.kt(new)}
ui/components/{MonthSelector.kt(new), charts/DonutChart.kt(new), charts/TrendBars.kt(new)}
ui/stats/{StatsScreen.kt, StatsViewModel.kt}        (new)
ui/calendar/{CalendarScreen.kt, CalendarViewModel.kt} (new)
ui/home/HomeScreen.kt                                (no longer owns app-level scaffold if it did; keep its own TopAppBar/FAB; bottom bar is provided by the shell)
MainActivity.kt                                       (still just PsyTheme { PsyNavHost() })
```
