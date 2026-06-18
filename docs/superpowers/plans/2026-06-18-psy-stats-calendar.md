# Psy Stats & Calendar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.
> **NO UNIT TESTS** (user preference). Verify each task by `./gradlew :app:assembleDebug`; final gate adds lint + manual emulator. Spec: `docs/superpowers/specs/2026-06-18-psy-stats-calendar-design.md`.

**Goal:** A bottom-nav shell (Trang chủ / Thống kê / Lịch), a Statistics screen (summary + Canvas donut pie with Chi/Thu toggle + top-10 + 6-month trend bars), and a Calendar month view (Monday-start grid with per-day income/expense, tap a day → its transactions).

**Environment for every gradle command:** `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` then `cd /Users/hoalam/Codes/psy/android && ./gradlew ...`

**Tech:** Kotlin, Compose Material3, Compose Canvas (charts — no library), Room, Hilt, Navigation Compose, java.time (minSdk 26). Amounts = Long minor units via `com.psy.domain.util.Money`. Candy Pop theme. Transfers excluded from income/expense aggregations.

---

## Task 1: Bottom navigation shell

**Files:**
- `ui/navigation/Routes.kt`: add `const val STATS = "stats"`, `const val CALENDAR = "calendar"`.
- `ui/navigation/PsyBottomBar.kt` (new): `@Composable fun PsyBottomBar(currentRoute: String?, onSelect: (String) -> Unit)` — a Material3 `NavigationBar` with 3 `NavigationBarItem`s: Home (Icons.Default.Home, "Trang chủ"), Stats (Icons.Default.BarChart or AutoMirrored equivalent, "Thống kê"), Calendar (Icons.Default.DateRange, "Lịch"); `selected = currentRoute == route`.
- `ui/navigation/PsyNavHost.kt` (restructure): wrap the `NavHost` in a `Scaffold` whose `bottomBar` shows `PsyBottomBar` ONLY when the current destination route ∈ {HOME, STATS, CALENDAR} (read `navController.currentBackStackEntryAsState()` → `destination?.route`). Bottom-nav `onSelect` navigates with `launchSingleTop = true; restoreState = true; popUpTo(navController.graph.findStartDestination().id){ saveState = true }`. Pass the Scaffold inner padding into the NavHost. Add `composable(Routes.STATS){ StatsScreen() }` and `composable(Routes.CALENDAR){ CalendarScreen() }`.
- Stubs (replaced in Tasks 3/4): `ui/stats/StatsScreen.kt` `@Composable fun StatsScreen()` → Scaffold/TopAppBar("Thống kê") + centered Text("coming soon"); `ui/calendar/CalendarScreen.kt` likewise ("Lịch").
- `ui/home/HomeScreen.kt`: ensure Home still renders its own TopAppBar (⚙️) + balance + list + FAB inside the shell content area (its content now sits above the shell bottom bar — remove any bottom padding assumptions; the shell provides bottom inset). Keep onAddClick/onTxClick/onSettingsClick params.

- [ ] Step 1: Implement routes, bottom bar, NavHost restructure, stubs, Home adjustment.
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): bottom navigation shell (Home/Stats/Calendar)`.

---

## Task 2: Shared MonthSelector + Canvas chart components

**Files:**
- `ui/components/MonthSelector.kt` (new): `@Composable fun MonthSelector(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, modifier)` — a centered Row: IconButton(◀) + Text("Tháng ${month.monthValue}/${month.year}", titleMedium) + IconButton(▶).
- `ui/components/charts/DonutChart.kt` (new):
  ```kotlin
  data class PieSlice(val label: String, val amountMinor: Long, val color: Long)
  ```
  `@Composable fun DonutChart(slices: List<PieSlice>, centerLabel: String, modifier: Modifier = Modifier)`:
  - A `Box(contentAlignment = Center)` containing a `Canvas` (size ~200dp) and a centered `Text(centerLabel)`.
  - total = slices.sumOf { it.amountMinor }. If total <= 0: draw one muted full ring (a single drawArc 360° in a light gray) and center "—".
  - Else: iterate slices, `var startAngle = -90f`; for each, `sweep = 360f * value/total`; `drawArc(color = Color(slice.color), startAngle, sweep, useCenter = false, style = Stroke(width = 40dp.toPx(), cap = StrokeCap.Butt), size = arcSize, topLeft = ...)` inset so the stroke ring fits; advance startAngle. Optionally animate a 0→1 progress with `animateFloatAsState` multiplying sweeps.
- `ui/components/charts/TrendBars.kt` (new):
  ```kotlin
  data class MonthBars(val label: String, val incomeMinor: Long, val expenseMinor: Long)
  ```
  `@Composable fun TrendBars(months: List<MonthBars>, modifier: Modifier = Modifier)`:
  - maxVal = max over all income/expense (≥1 to avoid /0). A `Canvas` (height ~160dp): for each month, compute an x-slot; draw two `drawRoundRect`s (income = CandyGreen, expense = CandyPinkDeep) with height proportional to value/maxVal, rounded top corners. Draw month labels under each slot (use `drawContext.canvas.nativeCanvas.drawText` or place Text via an overlay Row beneath the Canvas — simpler: a Row of labels under the Canvas aligned to slots). A small legend (green=Thu, pink=Chi) above/below.

- [ ] Step 1: Implement MonthSelector + DonutChart + TrendBars. Add `@Preview`s if helpful (optional).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): shared MonthSelector + Canvas donut & trend charts`.

---

## Task 3: Statistics screen + ViewModel

**Files:**
- `ui/stats/StatsViewModel.kt` (new): `@HiltViewModel`, inject TransactionRepository, CategoryRepository, LedgerRepository.
  - `selectedMonth: MutableStateFlow<YearMonth>` (default `YearMonth.now()`), `pieMode: MutableStateFlow<TxType>` (default EXPENSE). `prevMonth()/nextMonth()/setPieMode(t)`.
  - Resolve active ledger (firstOrNull) → ledgerId + `Currency.of(ledger.currency)`.
  - Combine month transactions (`observeBetween(ledgerId, monthStart, monthEnd)` reacting to selectedMonth via flatMapLatest), a 6-month-window query (`observeBetween(ledgerId, startOf(month-5), monthEnd)`), categories (observeAll → map id→Category), and pieMode into `StatsUiState`:
    - summary: incomeMinor (sum INCOME), expenseMinor (sum EXPENSE), netMinor = income − expense, avgPerDayMinor = expense / max(1, daysToCount) where daysToCount = if month==current → today.dayOfMonth else month.lengthOfMonth().
    - slices: of `pieMode` txns grouped by categoryId (non-null) → `PieSlice(category.name, sum, category.color)` sorted desc.
    - top: same sums as a sorted list (take 10) with percent of pieMode total.
    - trend: bucket the 6-month-window txns by YearMonth → 6 `MonthBars(label="M/yy", income, expense)` in chronological order (TRANSFER excluded everywhere).
  - `StateFlow<StatsUiState>` (monthLabel via the YearMonth, currency, summary fields, pieMode, slices, top, trend, loading).
- `ui/stats/StatsScreen.kt` (REPLACE stub): `StatsScreen(viewModel = hiltViewModel())`. Scaffold + TopAppBar("Thống kê"). Scrollable Column:
  - `MonthSelector` (prev/next).
  - Summary card (Candy gradient): Thu / Chi / Chênh lệch / TB ngày (Money.formatMinor).
  - Chi/Thu segmented toggle → setPieMode.
  - `DonutChart(slices, centerLabel = formatted pie total)` + legend (color dot + name + percent), only the slices list.
  - "Top chi tiêu"/"Top thu nhập" (by pieMode): rows = emoji+name, a proportion bar (Box(Modifier.fillMaxWidth(percent))), amount + percent.
  - "Xu hướng 6 tháng": `TrendBars(trend)`.
  - Empty state per section when no data.
  - Collect via collectAsStateWithLifecycle.

- [ ] Step 1: Implement VM + screen (replace stub).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): statistics screen (summary, pie, top-10, 6-month trend)`.

---

## Task 4: Calendar screen + ViewModel

**Files:**
- `ui/calendar/CalendarViewModel.kt` (new): `@HiltViewModel`, inject TransactionRepository, CategoryRepository, AccountRepository, LedgerRepository.
  - `selectedMonth: MutableStateFlow<YearMonth>` (default now), `selectedDay: MutableStateFlow<LocalDate?>` (default today if in current month else first of month). `prevMonth()/nextMonth()/selectDay(date)`.
  - Resolve active ledger + currency. Combine month txns + categories + accounts + selectedDay into `CalendarUiState`:
    - `dayTotals: Map<LocalDate, DayCell>` where `DayCell(date, incomeMinor, expenseMinor)` (TRANSFER excluded), built by bucketing month txns by `Instant.ofEpochMilli(date).atZone(zone).toLocalDate()`.
    - `weeks: List<List<DayCell?>>` — build a Monday-start grid: leading nulls for days before the 1st (offset = `firstOfMonth.dayOfWeek.value - 1` for Monday=1), one cell per day-of-month (DayCell from dayTotals or zeros), trailing nulls to fill the last week. Chunk into weeks of 7.
    - `dayRows`: the selectedDay's transactions as rows (reuse TxRow shape: category name/icon, account name, type, amount, note, toAccountName for transfers).
  - `StateFlow<CalendarUiState>` (monthLabel, currency, weeks, selectedDay, dayRows, loading).
- `ui/calendar/CalendarScreen.kt` (REPLACE stub): Scaffold + TopAppBar("Lịch"). Column:
  - `MonthSelector`.
  - Weekday header Row: T2 T3 T4 T5 T6 T7 CN.
  - Month grid: Column of week Rows; each cell a Box (weight 1f, aspect ~1): day number; if expenseMinor>0 a tiny red text/dot, if incomeMinor>0 a tiny green one; selected day → Candy primary ring/background; null cells empty; clickable cells call selectDay.
  - Divider + selected day's transaction list (reuse Home row style; empty state "Không có giao dịch" when empty).
  - collectAsStateWithLifecycle.

- [ ] Step 1: Implement VM + screen (replace stub).
- [ ] Step 2: `./gradlew :app:assembleDebug` → SUCCESSFUL.
- [ ] Step 3: Commit `feat(ui): calendar month view with per-day totals + day transactions`.

---

## Task 5: Verification gate (build + lint + manual emulator)

- [ ] Step 1: `./gradlew :app:assembleDebug :app:lintDebug` → SUCCESSFUL, 0 lint errors.
- [ ] Step 2: Manual run on Pixel_10_Pro:
  1. Bottom nav shows Trang chủ / Thống kê / Lịch; switching tabs preserves each tab's state.
  2. Record a couple expenses + an income (if not present) on Home.
  3. Thống kê: summary card shows totals; donut pie shows category slices; Chi/Thu toggle switches the pie/top; top list shows categories with bars; trend shows 6-month bars; month ◀/▶ changes the data.
  4. Lịch: grid is Monday-start, days with spending show red amounts; tap a day → its transactions list below; month ◀/▶ works.
  5. A transfer (if any) does not change income/expense totals on Stats.
- [ ] Step 3: `git --no-pager log --oneline && git status -s` clean.

---

## Self-Review Notes
- Spec coverage: bottom nav (Task 1); MonthSelector + Canvas charts (Task 2); stats summary/pie/top/trend (Task 3); calendar grid + day list (Task 4). All spec items covered.
- No tests; verification = compile + lint + manual checklist.
- Type consistency: `PieSlice`/`MonthBars` defined in Task 2 used by Task 3; `DayCell` defined+used in Task 4; `Routes.STATS/CALENDAR` shared by bottom bar + NavHost; half-open month ranges match `observeBetween`; TRANSFER excluded consistently.
- No new Room entities/DAOs — reuses `observeBetween` + category/account observers.
