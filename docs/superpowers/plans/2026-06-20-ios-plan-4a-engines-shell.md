# iOS Port — Plan 4a: Feature Engines + App Shell

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`).

**Goal:** Port every feature computation from the Android ViewModels into pure, CLI-tested engines in `PsyCore`, and stand up the app shell (4-tab `TabView`, AppContainer wiring, seed-on-launch, shared UI). Plan 4b then builds the screens on top.

**Architecture:** All math (home grouping, stats, calendar grid, budget) lives in `PsyCore` as pure `static` functions over domain value types + an injected `Calendar` — fully unit-testable via `swift test` (no simulator). The app's ViewModels (Plan 4b) become thin: subscribe to repo Combine publishers, call an engine, publish the result.

**Tech Stack:** PsyCore (Swift 6), SwiftUI, Combine.

**Reference (exact logic to mirror):** the Android ViewModels — `HomeViewModel.kt`, `StatsViewModel.kt`, `CalendarViewModel.kt`, `BudgetViewModel.kt`, `AddEditTransactionViewModel.kt`. Spec §4.

**Prerequisites:** Plans 1+2 landed. PsyCore verify: `cd ios/PsyCore && swift test`. App verify: `cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`.

---

## File Structure

```
ios/PsyCore/Sources/PsyCore/
  Formatting.swift        # Currency.format(_ minor:)
  PsyMonth.swift          # month value + epoch boundaries + labels (Calendar-based)
  ViewData.swift          # shared output types: TxRow, DayGroup, PieSlice, MonthBars, etc.
  HomeEngine.swift
  StatsEngine.swift
  CalendarEngine.swift
  BudgetEngine.swift
  AddEditLogic.swift
ios/PsyCore/Tests/PsyCoreTests/
  EngineTests.swift       # representative tests per engine (fixed timezone)

ios/Psy/App/
  PsyApp.swift            # (modify) create AppContainer, seed, show RootView
  RootView.swift          # 4-tab TabView + Settings entry (tabs stubbed; 4b fills them)
ios/Psy/UI/Components/
  MoneyText.swift         # formatted amount text
  MonthSelector.swift     # ‹ MM/yyyy › selector
```

---

## Task 1: Currency formatting + month helpers (PsyCore)

**Files:**
- Create: `ios/PsyCore/Sources/PsyCore/Formatting.swift`
- Create: `ios/PsyCore/Sources/PsyCore/PsyMonth.swift`

- [ ] **Step 1: Currency.format**

`ios/PsyCore/Sources/PsyCore/Formatting.swift`:
```swift
public extension Currency {
    /// Formats a minor-unit amount with this currency's fraction digits + symbol suffix.
    func format(_ amountMinor: Int64) -> String {
        Money.formatMinor(amountMinor, fractionDigits: fractionDigits, suffix: symbol)
    }
}
```

- [ ] **Step 2: PsyMonth (mirrors java.time.YearMonth usage in the VMs)**

`ios/PsyCore/Sources/PsyCore/PsyMonth.swift`:
```swift
import Foundation

/// A calendar month, with epoch-millis boundaries computed against an injected Calendar
/// (the Calendar carries the timezone — mirrors ZoneId.systemDefault() in the Android VMs).
public struct PsyMonth: Hashable, Sendable {
    public var year: Int
    public var month: Int // 1...12
    public init(year: Int, month: Int) { self.year = year; self.month = month }

    public static func current(_ cal: Calendar, now: Date) -> PsyMonth {
        let c = cal.dateComponents([.year, .month], from: now)
        return PsyMonth(year: c.year!, month: c.month!)
    }

    public func adding(_ months: Int, _ cal: Calendar) -> PsyMonth {
        let base = cal.date(from: DateComponents(year: year, month: month, day: 1))!
        let shifted = cal.date(byAdding: .month, value: months, to: base)!
        let c = cal.dateComponents([.year, .month], from: shifted)
        return PsyMonth(year: c.year!, month: c.month!)
    }

    /// Epoch millis at the start of day of day 1 (half-open range start).
    public func startMillis(_ cal: Calendar) -> Int64 {
        let day1 = cal.date(from: DateComponents(year: year, month: month, day: 1))!
        return Int64(cal.startOfDay(for: day1).timeIntervalSince1970 * 1000)
    }

    /// Epoch millis at the start of next month (half-open range end).
    public func endMillis(_ cal: Calendar) -> Int64 { adding(1, cal).startMillis(cal) }

    public func lengthOfMonth(_ cal: Calendar) -> Int {
        let day1 = cal.date(from: DateComponents(year: year, month: month, day: 1))!
        return cal.range(of: .day, in: .month, for: day1)!.count
    }

    public func atDay(_ day: Int, _ cal: Calendar) -> Date {
        cal.startOfDay(for: cal.date(from: DateComponents(year: year, month: month, day: day))!)
    }

    public var label: String { String(format: "%02d/%04d", month, year) }      // MM/yyyy
    public var shortLabel: String { "\(month)/\(year % 100)" }                   // M/yy (trend)
}
```

- [ ] **Step 3: Verify** `cd ios/PsyCore && swift build` → `Build complete!`
- [ ] **Step 4: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/Formatting.swift ios/PsyCore/Sources/PsyCore/PsyMonth.swift
git commit -m "feat(ios): Currency.format + PsyMonth calendar helpers"
```

---

## Task 2: Shared view-data types (PsyCore)

**Files:**
- Create: `ios/PsyCore/Sources/PsyCore/ViewData.swift`

- [ ] **Step 1: Output types used by engines + screens**

`ios/PsyCore/Sources/PsyCore/ViewData.swift`:
```swift
import Foundation

/// A single transaction row for Home/Calendar lists. For TRANSFER, `categoryIcon` is "🔄",
/// `title` is the source account name and `toAccountName` is the destination (mirrors the VMs).
public struct TxRow: Identifiable, Hashable, Sendable {
    public let id: Int64
    public let title: String          // category name, or source account for transfer
    public let icon: String
    public let accountName: String
    public let toAccountName: String?
    public let type: TxType
    public let amountMinor: Int64
    public let note: String
    public let photoUri: String?
    public init(id: Int64, title: String, icon: String, accountName: String, toAccountName: String?,
                type: TxType, amountMinor: Int64, note: String, photoUri: String?) {
        self.id = id; self.title = title; self.icon = icon; self.accountName = accountName
        self.toAccountName = toAccountName; self.type = type; self.amountMinor = amountMinor
        self.note = note; self.photoUri = photoUri
    }
}

public struct DayGroup: Identifiable, Hashable, Sendable {
    public var id: String { dateLabel }
    public let dateLabel: String
    public let items: [TxRow]
    public init(dateLabel: String, items: [TxRow]) { self.dateLabel = dateLabel; self.items = items }
}

public struct PieSlice: Identifiable, Hashable, Sendable {
    public var id: String { name }
    public let name: String
    public let amountMinor: Int64
    public let color: Int64
    public init(name: String, amountMinor: Int64, color: Int64) { self.name = name; self.amountMinor = amountMinor; self.color = color }
}

public struct MonthBars: Identifiable, Hashable, Sendable {
    public var id: String { label }
    public let label: String
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public init(label: String, incomeMinor: Int64, expenseMinor: Int64) { self.label = label; self.incomeMinor = incomeMinor; self.expenseMinor = expenseMinor }
}

/// Helper for building TxRow from a Transaction + lookup maps (shared by Home/Calendar engines).
public enum TxRowBuilder {
    public static func make(_ tx: Transaction, categories: [Int64: Category], accounts: [Int64: Account]) -> TxRow {
        let acc = accounts[tx.accountId]
        if tx.type == .transfer {
            let toAcc = tx.toAccountId.flatMap { accounts[$0] }
            return TxRow(id: tx.id, title: acc?.name ?? "—", icon: "🔄", accountName: acc?.name ?? "—",
                         toAccountName: toAcc?.name ?? "—", type: tx.type, amountMinor: tx.amountMinor,
                         note: tx.note, photoUri: tx.photoUri)
        } else {
            let cat = tx.categoryId.flatMap { categories[$0] }
            return TxRow(id: tx.id, title: cat?.name ?? "—", icon: cat?.icon ?? "📦", accountName: acc?.name ?? "—",
                         toAccountName: nil, type: tx.type, amountMinor: tx.amountMinor,
                         note: tx.note, photoUri: tx.photoUri)
        }
    }
}
```

- [ ] **Step 2: Verify** `cd ios/PsyCore && swift build` → `Build complete!`
- [ ] **Step 3: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/ViewData.swift
git commit -m "feat(ios): shared view-data types (TxRow, DayGroup, PieSlice, MonthBars)"
```

---

## Task 3: HomeEngine (PsyCore)

**Files:**
- Create: `ios/PsyCore/Sources/PsyCore/HomeEngine.swift`

**Context:** Mirrors `HomeViewModel`. Input `transactions` is the current month, already sorted date DESC, id DESC by the repository. TRANSFER excluded from income/expense sums. Day labels: today→"Hôm nay", yesterday→"Hôm qua", else "dd/MM/yyyy".

- [ ] **Step 1: Implement**

`ios/PsyCore/Sources/PsyCore/HomeEngine.swift`:
```swift
import Foundation

public struct HomeResult: Sendable {
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
    public let days: [DayGroup]
}

public enum HomeEngine {
    public static func build(transactions: [Transaction], categories: [Category], accounts: [Account],
                             calendar: Calendar, now: Date) -> HomeResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let accMap = Dictionary(accounts.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })

        let today = calendar.startOfDay(for: now)
        let yesterday = calendar.date(byAdding: .day, value: -1, to: today)!

        // Bucket by start-of-day, preserving input order within each day.
        var order: [Date] = []
        var buckets: [Date: [Transaction]] = [:]
        for tx in transactions {
            let day = calendar.startOfDay(for: Date(timeIntervalSince1970: Double(tx.date) / 1000))
            if buckets[day] == nil { order.append(day) }
            buckets[day, default: []].append(tx)
        }
        let sortedDays = order.sorted(by: >)

        let df = DateFormatter()
        df.calendar = calendar
        df.timeZone = calendar.timeZone
        df.dateFormat = "dd/MM/yyyy"

        let days: [DayGroup] = sortedDays.map { day in
            let label: String = day == today ? "Hôm nay" : (day == yesterday ? "Hôm qua" : df.string(from: day))
            let rows = buckets[day]!.map { TxRowBuilder.make($0, categories: catMap, accounts: accMap) }
            return DayGroup(dateLabel: label, items: rows)
        }

        var income: Int64 = 0, expense: Int64 = 0
        for tx in transactions {
            switch tx.type {
            case .income: income += tx.amountMinor
            case .expense: expense += tx.amountMinor
            case .transfer: break
            }
        }
        return HomeResult(incomeMinor: income, expenseMinor: expense, netMinor: income - expense, days: days)
    }
}
```

- [ ] **Step 2: Verify** `cd ios/PsyCore && swift build`. **Step 3: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/HomeEngine.swift
git commit -m "feat(ios): HomeEngine (day grouping + income/expense sums)"
```

---

## Task 4: StatsEngine (PsyCore)

**Files:**
- Create: `ios/PsyCore/Sources/PsyCore/StatsEngine.swift`

**Context:** Faithful port of `StatsViewModel`. Input `windowTransactions` = the 6-month window `[month-5 start, monthEnd)` for the ledger. `accountBreakdown` is computed from ALL accounts before filtering; everything else respects `accountFilter`. Pie colors come from the fixed `piePalette` by slice index (NOT category color). Avg-per-day uses today's day-of-month for the current month, else the month length.

- [ ] **Step 1: Implement**

`ios/PsyCore/Sources/PsyCore/StatsEngine.swift`:
```swift
import Foundation

public struct StatsSummary: Sendable {
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
    public let avgPerDayMinor: Int64
}

public struct TopEntry: Identifiable, Sendable {
    public var id: Int64 { category.id }
    public let category: Category
    public let amountMinor: Int64
    public let percent: Double
}

public struct AccountStat: Identifiable, Sendable {
    public let id: Int64
    public let name: String
    public let icon: String
    public let color: Int64
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
}

public struct StatsResult: Sendable {
    public let summary: StatsSummary
    public let pieMode: TxType
    public let slices: [PieSlice]
    public let top: [TopEntry]
    public let trend: [MonthBars]
    public let accountBreakdown: [AccountStat]
    public let selectedAccountId: Int64?
}

public enum StatsEngine {
    /// Fixed palette: slice colors by index so the chart is always readable (mirrors StatsViewModel.piePalette).
    public static let piePalette: [Int64] = [
        0xFFA18CFF, 0xFF7FD8FF, 0xFFFF8FD6, 0xFFFF5FA2, 0xFF22C55E,
        0xFFFFB86B, 0xFF6BCB77, 0xFF4D96FF, 0xFFFF6B6B, 0xFFB088F9,
    ]

    public static func build(windowTransactions: [Transaction], categories: [Category], accounts: [Account],
                             month: PsyMonth, pieMode: TxType, accountFilter: Int64?,
                             calendar: Calendar, now: Date) -> StatsResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let accMap = Dictionary(accounts.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })

        let monthStart = month.startMillis(calendar)
        let monthEnd = month.endMillis(calendar)

        // ── Account breakdown (ALL accounts, before filtering) ──
        let monthTxnsAll = windowTransactions.filter { $0.date >= monthStart && $0.date < monthEnd }
        var byAccount: [Int64: (inc: Int64, exp: Int64)] = [:]
        for tx in monthTxnsAll {
            switch tx.type {
            case .income: byAccount[tx.accountId, default: (0, 0)].inc += tx.amountMinor
            case .expense: byAccount[tx.accountId, default: (0, 0)].exp += tx.amountMinor
            case .transfer: break
            }
        }
        let accountBreakdown: [AccountStat] = byAccount.compactMap { id, sums in
            guard let acc = accMap[id] else { return nil }
            return AccountStat(id: acc.id, name: acc.name, icon: acc.icon, color: acc.color,
                               incomeMinor: sums.inc, expenseMinor: sums.exp, netMinor: sums.inc - sums.exp)
        }.sorted { ($0.incomeMinor + $0.expenseMinor) > ($1.incomeMinor + $1.expenseMinor) }

        // Drop filter if the account no longer exists.
        let effectiveFilter = accountFilter.flatMap { accMap[$0] != nil ? $0 : nil }
        let filteredWindow = effectiveFilter == nil ? windowTransactions : windowTransactions.filter { $0.accountId == effectiveFilter }
        let monthTxns = filteredWindow.filter { $0.date >= monthStart && $0.date < monthEnd }

        // ── Summary ──
        var income: Int64 = 0, expense: Int64 = 0
        for tx in monthTxns {
            switch tx.type {
            case .income: income += tx.amountMinor
            case .expense: expense += tx.amountMinor
            case .transfer: break
            }
        }
        let currentYM = PsyMonth.current(calendar, now: now)
        let todayDay = calendar.component(.day, from: now)
        let daysToCount = (month == currentYM) ? todayDay : month.lengthOfMonth(calendar)
        let avgPerDay = expense / Int64(max(1, daysToCount))

        // ── Pie slices (by index palette) ──
        let pieTxns = monthTxns.filter { $0.type == pieMode && $0.categoryId != nil }
        var pieByCategory: [Int64: Int64] = [:]
        for tx in pieTxns { pieByCategory[tx.categoryId!, default: 0] += tx.amountMinor }

        let sortedPie = pieByCategory.compactMap { catId, amount -> (name: String, amount: Int64)? in
            guard let cat = catMap[catId] else { return nil }
            return (cat.name, amount)
        }.sorted { $0.amount > $1.amount }
        let slices = sortedPie.enumerated().map { i, e in
            PieSlice(name: e.name, amountMinor: e.amount, color: piePalette[i % piePalette.count])
        }

        // ── Top entries ──
        let pieTotal = slices.reduce(Int64(0)) { $0 + $1.amountMinor }
        let top = pieByCategory.compactMap { catId, amount -> TopEntry? in
            guard let cat = catMap[catId] else { return nil }
            let pct = pieTotal > 0 ? Double(amount) / Double(pieTotal) : 0
            return TopEntry(category: cat, amountMinor: amount, percent: pct)
        }.sorted { $0.amountMinor > $1.amountMinor }.prefix(10).map { $0 }

        // ── Trend (6 months) ──
        let trend: [MonthBars] = (0...5).reversed().map { offset in
            let ym = month.adding(-offset, calendar)
            let s = ym.startMillis(calendar), e = ym.endMillis(calendar)
            var inc: Int64 = 0, exp: Int64 = 0
            for tx in filteredWindow where tx.date >= s && tx.date < e {
                switch tx.type {
                case .income: inc += tx.amountMinor
                case .expense: exp += tx.amountMinor
                case .transfer: break
                }
            }
            return MonthBars(label: ym.shortLabel, incomeMinor: inc, expenseMinor: exp)
        }

        return StatsResult(
            summary: StatsSummary(incomeMinor: income, expenseMinor: expense, netMinor: income - expense, avgPerDayMinor: avgPerDay),
            pieMode: pieMode, slices: slices, top: top, trend: trend,
            accountBreakdown: accountBreakdown, selectedAccountId: effectiveFilter)
    }
}
```

- [ ] **Step 2: Verify** `cd ios/PsyCore && swift build`. **Step 3: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/StatsEngine.swift
git commit -m "feat(ios): StatsEngine (summary, pie palette, top, trend, account breakdown)"
```

---

## Task 5: CalendarEngine + BudgetEngine + AddEditLogic (PsyCore)

**Files:**
- Create: `ios/PsyCore/Sources/PsyCore/CalendarEngine.swift`
- Create: `ios/PsyCore/Sources/PsyCore/BudgetEngine.swift`
- Create: `ios/PsyCore/Sources/PsyCore/AddEditLogic.swift`

- [ ] **Step 1: CalendarEngine (Monday-start grid; TRANSFER excluded from day totals — mirrors CalendarViewModel)**

`ios/PsyCore/Sources/PsyCore/CalendarEngine.swift`:
```swift
import Foundation

public struct DayCell: Identifiable, Hashable, Sendable {
    public var id: Int { day }
    public let day: Int
    public let date: Date
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let isToday: Bool
}

public struct CalendarResult: Sendable {
    public let weeks: [[DayCell?]]
    public let dayRows: [TxRow]
}

public enum CalendarEngine {
    public static func build(monthTransactions: [Transaction], month: PsyMonth, categories: [Category],
                             accounts: [Account], selectedDay: Date?, calendar: Calendar, now: Date) -> CalendarResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let accMap = Dictionary(accounts.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let today = calendar.startOfDay(for: now)

        // Day totals (income, expense); TRANSFER excluded.
        var totals: [Date: (inc: Int64, exp: Int64)] = [:]
        for tx in monthTransactions {
            let day = calendar.startOfDay(for: Date(timeIntervalSince1970: Double(tx.date) / 1000))
            switch tx.type {
            case .income: totals[day, default: (0, 0)].inc += tx.amountMinor
            case .expense: totals[day, default: (0, 0)].exp += tx.amountMinor
            case .transfer: break
            }
        }

        // Monday-start grid: weekday is 1=Sun...7=Sat → leading nulls = (weekday + 5) % 7.
        let day1 = calendar.date(from: DateComponents(year: month.year, month: month.month, day: 1))!
        let weekday = calendar.component(.weekday, from: day1)
        let lead = (weekday + 5) % 7
        let daysInMonth = month.lengthOfMonth(calendar)

        var cells: [DayCell?] = Array(repeating: nil, count: lead)
        for day in 1...daysInMonth {
            let date = month.atDay(day, calendar)
            let t = totals[date] ?? (0, 0)
            cells.append(DayCell(day: day, date: date, incomeMinor: t.inc, expenseMinor: t.exp, isToday: date == today))
        }
        let remainder = cells.count % 7
        if remainder != 0 { cells.append(contentsOf: Array(repeating: nil, count: 7 - remainder)) }
        let weeks = stride(from: 0, to: cells.count, by: 7).map { Array(cells[$0..<min($0 + 7, cells.count)]) }

        // Selected-day rows.
        var dayRows: [TxRow] = []
        if let sel = selectedDay {
            let selDay = calendar.startOfDay(for: sel)
            dayRows = monthTransactions
                .filter { calendar.startOfDay(for: Date(timeIntervalSince1970: Double($0.date) / 1000)) == selDay }
                .map { TxRowBuilder.make($0, categories: catMap, accounts: accMap) }
        }
        return CalendarResult(weeks: weeks, dayRows: dayRows)
    }
}
```

- [ ] **Step 2: BudgetEngine (spent = EXPENSE only; per-category sorted desc by percent — mirrors BudgetViewModel)**

`ios/PsyCore/Sources/PsyCore/BudgetEngine.swift`:
```swift
import Foundation

public struct TotalBudget: Sendable {
    public let budget: Budget
    public let limitMinor: Int64
    public let spentMinor: Int64
    public let percent: Double
}

public struct CategoryBudgetItem: Identifiable, Sendable {
    public var id: Int64 { budget.id }
    public let budget: Budget
    public let category: Category?
    public let limitMinor: Int64
    public let spentMinor: Int64
    public let percent: Double
}

public struct BudgetResult: Sendable {
    public let total: TotalBudget?
    public let categoryBudgets: [CategoryBudgetItem]
    public let availableCategories: [Category]
}

public enum BudgetEngine {
    public static func build(monthTransactions: [Transaction], budgets: [Budget], categories: [Category]) -> BudgetResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let expenseTxns = monthTransactions.filter { $0.type == .expense }
        let totalSpent = expenseTxns.reduce(Int64(0)) { $0 + $1.amountMinor }

        let totalBudget = budgets.first { $0.categoryId == nil }
        let total = totalBudget.map { b -> TotalBudget in
            let pct = b.amountMinor > 0 ? Double(totalSpent) / Double(b.amountMinor) : 0
            return TotalBudget(budget: b, limitMinor: b.amountMinor, spentMinor: totalSpent, percent: pct)
        }

        let categoryBudgets = budgets.filter { $0.categoryId != nil }.compactMap { b -> CategoryBudgetItem in
            let catSpent = expenseTxns.filter { $0.categoryId == b.categoryId }.reduce(Int64(0)) { $0 + $1.amountMinor }
            let pct = b.amountMinor > 0 ? Double(catSpent) / Double(b.amountMinor) : 0
            return CategoryBudgetItem(budget: b, category: catMap[b.categoryId!], limitMinor: b.amountMinor, spentMinor: catSpent, percent: pct)
        }.sorted { $0.percent > $1.percent }

        let budgetedIds = Set(budgets.compactMap { $0.categoryId })
        let available = categories.filter { $0.type == .expense && !budgetedIds.contains($0.id) }

        return BudgetResult(total: total, categoryBudgets: categoryBudgets, availableCategories: available)
    }
}
```

- [ ] **Step 3: AddEditLogic (amount parsing + canSave — mirrors AddEditTransactionViewModel)**

`ios/PsyCore/Sources/PsyCore/AddEditLogic.swift`:
```swift
public enum AddEditLogic {
    /// amountMinor = (typed integer) * 10^fractionDigits. Non-digits ignored.
    public static func amountMinor(typed: String, fractionDigits: Int) -> Int64 {
        let digits = typed.filter(\.isNumber)
        let value = Int64(digits) ?? 0
        var mult: Int64 = 1
        for _ in 0..<fractionDigits { mult *= 10 }
        return value * mult
    }

    /// Reverse: minor → typed whole-unit string (used when editing).
    public static func typedString(amountMinor: Int64, fractionDigits: Int) -> String {
        var div: Int64 = 1
        for _ in 0..<fractionDigits { div *= 10 }
        return div > 0 ? String(amountMinor / div) : "0"
    }

    public static func canSave(amountText: String, type: TxType, categoryId: Int64?, accountId: Int64?, toAccountId: Int64?) -> Bool {
        let amount = Int64(amountText.filter(\.isNumber)) ?? 0
        guard amount > 0 else { return false }
        switch type {
        case .income, .expense: return categoryId != nil && accountId != nil
        case .transfer: return accountId != nil && toAccountId != nil && accountId != toAccountId
        }
    }
}
```

- [ ] **Step 4: Verify** `cd ios/PsyCore && swift build` → `Build complete!`
- [ ] **Step 5: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/CalendarEngine.swift ios/PsyCore/Sources/PsyCore/BudgetEngine.swift ios/PsyCore/Sources/PsyCore/AddEditLogic.swift
git commit -m "feat(ios): CalendarEngine, BudgetEngine, AddEditLogic"
```

---

## Task 6: Engine tests (PsyCore, fixed timezone)

**Files:**
- Create: `ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift`

**Context:** Tests pin a fixed timezone so date math is deterministic. They guard the non-obvious rules: TRANSFER exclusion, pie palette-by-index, avg-per-day, budget spent = expense-only, Monday-start leading cells, amount parsing.

- [ ] **Step 1: Write tests**

`ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift`:
```swift
import XCTest
import Foundation
@testable import PsyCore

final class EngineTests: XCTestCase {
    // Fixed calendar: UTC+7 (Asia/Bangkok), Gregorian.
    private func cal() -> Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "Asia/Bangkok")!
        return c
    }
    // millis for a given Y-M-D at noon local (avoids day-boundary ambiguity).
    private func millis(_ y: Int, _ m: Int, _ d: Int) -> Int64 {
        let c = cal()
        let date = c.date(from: DateComponents(year: y, month: m, day: d, hour: 12))!
        return Int64(date.timeIntervalSince1970 * 1000)
    }
    private func acc(_ id: Int64, _ name: String) -> Account { Account(id: id, name: name, type: .cash, icon: "💵", color: 0xFF22C55E) }
    private func cat(_ id: Int64, _ name: String, _ t: CategoryType) -> Category { Category(id: id, name: name, icon: "🍜", color: 0xFFFF8FD6, type: t, sortOrder: 0) }
    private func tx(_ id: Int64, _ type: TxType, _ amount: Int64, cat: Int64?, acc: Int64, to: Int64? = nil, at: Int64) -> Transaction {
        Transaction(id: id, ledgerId: 1, type: type, amountMinor: amount, categoryId: cat, accountId: acc, toAccountId: to, note: "", date: at, createdAt: at, updatedAt: at, photoUri: nil)
    }

    func testHomeExcludesTransferFromSums() {
        let txns = [
            tx(1, .income, 1000, cat: 10, acc: 1, at: millis(2026, 6, 10)),
            tx(2, .expense, 400, cat: 20, acc: 1, at: millis(2026, 6, 10)),
            tx(3, .transfer, 999, cat: nil, acc: 1, to: 2, at: millis(2026, 6, 10)),
        ]
        let r = HomeEngine.build(transactions: txns, categories: [cat(10, "Lương", .income), cat(20, "Ăn", .expense)],
                                 accounts: [acc(1, "Tiền mặt"), acc(2, "Bank")], calendar: cal(),
                                 now: Date(timeIntervalSince1970: Double(millis(2026, 6, 15)) / 1000))
        XCTAssertEqual(r.incomeMinor, 1000)
        XCTAssertEqual(r.expenseMinor, 400)
        XCTAssertEqual(r.netMinor, 600)
    }

    func testStatsPiePaletteByIndexAndAvg() {
        let month = PsyMonth(year: 2026, month: 6)
        let txns = [
            tx(1, .expense, 600, cat: 20, acc: 1, at: millis(2026, 6, 2)),
            tx(2, .expense, 400, cat: 21, acc: 1, at: millis(2026, 6, 3)),
        ]
        let cats = [cat(20, "Ăn", .expense), cat(21, "Đi lại", .expense)]
        let now = Date(timeIntervalSince1970: Double(millis(2026, 6, 10)) / 1000) // day 10 of current month
        let r = StatsEngine.build(windowTransactions: txns, categories: cats, accounts: [acc(1, "TM")],
                                  month: month, pieMode: .expense, accountFilter: nil, calendar: cal(), now: now)
        // Largest slice first, palette index 0 then 1.
        XCTAssertEqual(r.slices.first?.name, "Ăn")
        XCTAssertEqual(r.slices[0].color, StatsEngine.piePalette[0])
        XCTAssertEqual(r.slices[1].color, StatsEngine.piePalette[1])
        // avg = expense(1000) / day-of-month(10) = 100
        XCTAssertEqual(r.summary.avgPerDayMinor, 100)
        XCTAssertEqual(r.trend.count, 6)
    }

    func testStatsAccountFilter() {
        let month = PsyMonth(year: 2026, month: 6)
        let txns = [
            tx(1, .expense, 500, cat: 20, acc: 1, at: millis(2026, 6, 2)),
            tx(2, .expense, 300, cat: 20, acc: 2, at: millis(2026, 6, 3)),
        ]
        let r = StatsEngine.build(windowTransactions: txns, categories: [cat(20, "Ăn", .expense)],
                                  accounts: [acc(1, "A"), acc(2, "B")], month: month, pieMode: .expense,
                                  accountFilter: 1, calendar: cal(), now: Date(timeIntervalSince1970: Double(millis(2026, 6, 10)) / 1000))
        XCTAssertEqual(r.summary.expenseMinor, 500)            // only account 1
        XCTAssertEqual(r.accountBreakdown.count, 2)            // breakdown still shows both
    }

    func testCalendarMondayGridLeading() {
        // June 2026: day 1 is a Monday → 0 leading nulls.
        let r = CalendarEngine.build(monthTransactions: [], month: PsyMonth(year: 2026, month: 6),
                                     categories: [], accounts: [], selectedDay: nil, calendar: cal(),
                                     now: Date(timeIntervalSince1970: Double(millis(2026, 6, 1)) / 1000))
        XCTAssertEqual(r.weeks.first?.first ?? nil != nil, true) // first cell is day 1, not nil
        XCTAssertEqual(r.weeks.flatMap { $0 }.compactMap { $0 }.count, 30)
    }

    func testBudgetSpentExpenseOnly() {
        let txns = [
            tx(1, .expense, 700, cat: 20, acc: 1, at: millis(2026, 6, 2)),
            tx(2, .income, 5000, cat: 10, acc: 1, at: millis(2026, 6, 2)),
            tx(3, .transfer, 100, cat: nil, acc: 1, to: 2, at: millis(2026, 6, 2)),
        ]
        let budgets = [Budget(id: 1, ledgerId: 1, categoryId: nil, amountMinor: 1000)]
        let r = BudgetEngine.build(monthTransactions: txns, budgets: budgets, categories: [cat(20, "Ăn", .expense)])
        XCTAssertEqual(r.total?.spentMinor, 700)   // income + transfer excluded
        XCTAssertEqual(r.total?.percent ?? 0, 0.7, accuracy: 0.0001)
    }

    func testAddEditAmountAndCanSave() {
        XCTAssertEqual(AddEditLogic.amountMinor(typed: "12a3", fractionDigits: 0), 123)
        XCTAssertEqual(AddEditLogic.amountMinor(typed: "12", fractionDigits: 2), 1200)
        XCTAssertFalse(AddEditLogic.canSave(amountText: "0", type: .expense, categoryId: 1, accountId: 1, toAccountId: nil))
        XCTAssertTrue(AddEditLogic.canSave(amountText: "10", type: .expense, categoryId: 1, accountId: 1, toAccountId: nil))
        XCTAssertFalse(AddEditLogic.canSave(amountText: "10", type: .transfer, categoryId: nil, accountId: 1, toAccountId: 1)) // same acct
        XCTAssertTrue(AddEditLogic.canSave(amountText: "10", type: .transfer, categoryId: nil, accountId: 1, toAccountId: 2))
    }
}
```

- [ ] **Step 2: Run** `cd ios/PsyCore && swift test` → expect ALL pass (Money + SnapshotDTO + Engine tests).

> If `testCalendarMondayGridLeading`'s weekday assumption is off (June 1 2026 is a Monday — verify), fix the expectation, not the engine, unless the engine's `(weekday + 5) % 7` is genuinely wrong. Report which.

- [ ] **Step 3: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift
git commit -m "test(ios): engine tests (transfer exclusion, palette, avg, budget, grid, amounts)"
```

---

## Task 7: App shell — AppContainer wiring + RootView + shared UI

**Files:**
- Modify: `ios/Psy/PsyApp.swift`
- Create: `ios/Psy/App/RootView.swift`
- Create: `ios/Psy/UI/Components/MoneyText.swift`
- Create: `ios/Psy/UI/Components/MonthSelector.swift`

**Context:** For Plan 4 the app shows the tabs directly and seeds on first launch (the login/lock gate arrives in Plan 5). `AppContainer` is created once and passed down by initializer (explicit DI). Tabs are stubbed here; Plan 4b replaces each stub with the real screen. A shared `Calendar` (`.current`) and `Date()` "now" are provided to ViewModels later.

- [ ] **Step 1: MoneyText**

`ios/Psy/UI/Components/MoneyText.swift`:
```swift
import SwiftUI
import PsyCore

/// Renders a minor-unit amount using a Currency. Optional sign prefix for income(+)/expense(-).
struct MoneyText: View {
    let amountMinor: Int64
    var currency: Currency = .vnd
    var prefix: String = ""
    var body: some View {
        Text(prefix + currency.format(amountMinor))
    }
}
```

- [ ] **Step 2: MonthSelector**

`ios/Psy/UI/Components/MonthSelector.swift`:
```swift
import SwiftUI
import PsyCore

/// ‹ MM/yyyy › selector used by Stats / Calendar / Budget.
struct MonthSelector: View {
    let label: String
    let onPrev: () -> Void
    let onNext: () -> Void
    @Environment(\.psyColors) private var colors
    var body: some View {
        HStack(spacing: 16) {
            Button(action: onPrev) { Image(systemName: "chevron.left") }
            Text(label).font(PsyFont.titleMedium).frame(minWidth: 90)
            Button(action: onNext) { Image(systemName: "chevron.right") }
        }
        .foregroundStyle(colors.onSurface)
        .padding(.vertical, 6)
    }
}
```

- [ ] **Step 3: RootView (4-tab TabView; stubs for now)**

`ios/Psy/App/RootView.swift`:
```swift
import SwiftUI

struct RootView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var colors

    var body: some View {
        TabView {
            tabStub("Trang chủ")
                .tabItem { Label("Trang chủ", systemImage: "house.fill") }
            tabStub("Thống kê")
                .tabItem { Label("Thống kê", systemImage: "chart.bar.fill") }
            tabStub("Lịch")
                .tabItem { Label("Lịch", systemImage: "calendar") }
            tabStub("Ngân sách")
                .tabItem { Label("Ngân sách", systemImage: "banknote.fill") }
        }
    }

    private func tabStub(_ title: String) -> some View {
        ZStack {
            colors.background.ignoresSafeArea()
            Text(title).font(PsyFont.headlineMedium).foregroundStyle(colors.onSurface)
        }
    }
}
```

- [ ] **Step 4: PsyApp wires container + seeds**

Replace `ios/Psy/PsyApp.swift` with:
```swift
import SwiftUI

@main
struct PsyApp: App {
    @State private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView(container: container)
                .psyTheme(mode: .system, accent: .candyViolet)
                .task {
                    // Offline-first: ensure default data exists on first launch (Plan 5 moves this
                    // behind the login gate via restore-or-seed).
                    container.seeder.seedIfEmpty(now: Int64(Date().timeIntervalSince1970 * 1000))
                }
        }
    }
}
```

- [ ] **Step 5: Verify build + run**

Run:
```bash
cd ios && xcodegen generate
xcodebuild -project Psy.xcodeproj -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build
```
Expected: `** BUILD SUCCEEDED **`. Then launch + screenshot:
```bash
APP=$(xcodebuild -project ios/Psy.xcodeproj -scheme Psy -showBuildSettings -destination 'platform=iOS Simulator,name=iPhone 17' 2>/dev/null | awk -F' = ' '/ TARGET_BUILD_DIR /{d=$2} / FULL_PRODUCT_NAME /{p=$2} END{print d"/"p}')
xcrun simctl boot "iPhone 17" 2>/dev/null; open -a Simulator
xcrun simctl install booted "$APP"
xcrun simctl launch booted com.psy
sleep 3
xcrun simctl io booted screenshot /tmp/psy-shell.png
```
Expected: app shows a 4-tab bar (Trang chủ / Thống kê / Lịch / Ngân sách).

- [ ] **Step 6: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/PsyApp.swift ios/Psy/App/RootView.swift ios/Psy/UI/Components/MoneyText.swift ios/Psy/UI/Components/MonthSelector.swift
git commit -m "feat(ios): app shell — AppContainer wiring, 4-tab RootView, shared UI"
```

---

## Self-Review (completed during authoring)

**Spec coverage:** §4.1–4.5 computations → engines Tasks 3–5; §4 formatting → Task 1; shell/tabs → Task 7. Tests guard the non-obvious rules (Task 6).

**Placeholder scan:** none. Tab stubs in Task 7 are explicitly temporary and replaced in Plan 4b; the Monday-grid test note is a verify-the-date guard, not a placeholder.

**Type consistency:** Engine output types (`HomeResult`, `StatsResult`, `CalendarResult`, `BudgetResult`, `TxRow`, `PieSlice`, `MonthBars`, `TopEntry`, `AccountStat`, `DayCell`, `TotalBudget`, `CategoryBudgetItem`) defined once in PsyCore, consumed by Plan 4b VMs. `PsyMonth` API (`startMillis`/`endMillis`/`adding`/`label`/`shortLabel`/`atDay`/`lengthOfMonth`) used consistently across engines. `TxRowBuilder.make` shared by Home + Calendar engines.

---

## Execution Handoff

After Plan 4a, **Plan 4b** builds the six real screens (replacing the RootView stubs), then **Plan 5** adds auth/sync/lock.
