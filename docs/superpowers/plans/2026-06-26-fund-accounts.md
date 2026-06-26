# Fund accounts (Quỹ) + icon picker expansion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let any account be flagged as a *fund* (quỹ) whose transactions are excluded from income/expense statistics & budget but still appear in the transaction list (cash flow), marked with a "Quỹ" badge; and expand the icon picker from ~36 to ~90 icons. Ship on both Android and iOS with parity.

**Architecture:** Add a boolean `isFund` to the `Account` model on both platforms (domain model + persistence entity + backup DTO, snapshot version bumped 2→3). Stats and budget exclude fund-account transactions by filtering at a single point — the "all accounts" window derivation — so the existing summary/pie/trend/budget code needs no other change. The by-account breakdown keeps fund rows (flagged). Icons expand by adding a shared, ordered name list (Android maps names to the Compose Lucide library; iOS scaffolds `lucide-<name>.imageset` SVGs from lucide-static v1.21.0).

**Tech Stack:** Android (Kotlin, Compose, Room, Hilt, kotlinx.serialization), iOS (Swift, SwiftUI, SwiftData, PsyCore SwiftPM, swift test), shared cross-platform snapshot JSON.

**Spec:** `docs/superpowers/specs/2026-06-26-fund-accounts-design.md`

**Key design notes (read before starting):**
- `isFund` is always added as the **last** field/parameter with a default `= false`, so existing positional constructors and mapper call sites keep compiling.
- Android DB uses `fallbackToDestructiveMigration(dropAllTables = true)` (DEV-only) — **no Room `Migration` object is needed**, just bump the `@Database` version so the destructive rebuild adds the column.
- Stats exclusion semantics: in the **"Tất cả" (all accounts)** view, fund transactions are excluded from summary/pie/top/trend. When the user **explicitly filters to a specific account** (even a fund), that account's real numbers are shown (avoids a confusing 0 đ). This falls out naturally from filtering only the `effectiveFilter == null` branch.

---

## Phase A — Data model & persistence (`isFund` field)

### Task 1: Android — `isFund` on Account domain + Room entity + mapper + DB version

**Files:**
- Modify: `android/app/src/main/java/com/psy/domain/model/Account.kt`
- Modify: `android/app/src/main/java/com/psy/data/db/entity/AccountEntity.kt`
- Modify: `android/app/src/main/java/com/psy/data/db/mapper/Mappers.kt:6-7`
- Modify: `android/app/src/main/java/com/psy/data/db/PsyDatabase.kt:20`

- [ ] **Step 1: Add `isFund` to the domain model**

Replace the body of `android/app/src/main/java/com/psy/domain/model/Account.kt`:

```kotlin
package com.psy.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val color: Long, // ARGB packed
    val isFund: Boolean = false, // fund (quỹ): excluded from income/expense stats & budget
)
```

- [ ] **Step 2: Add `isFund` to the Room entity**

Replace the body of `android/app/src/main/java/com/psy/data/db/entity/AccountEntity.kt`:

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
    val isFund: Boolean = false,
)
```

- [ ] **Step 3: Update the entity↔domain mappers**

In `android/app/src/main/java/com/psy/data/db/mapper/Mappers.kt`, replace lines 6-7:

```kotlin
fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), icon, color, isFund)
fun Account.toEntity() = AccountEntity(id, name, type.name, icon, color, isFund)
```

- [ ] **Step 4: Bump the Room database version**

In `android/app/src/main/java/com/psy/data/db/PsyDatabase.kt`, change `version = 5` to `version = 6` on line 20. (No `Migration` object needed — `DatabaseModule` uses `fallbackToDestructiveMigration(dropAllTables = true)`.)

- [ ] **Step 5: Build to verify it compiles**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/psy/domain/model/Account.kt \
        android/app/src/main/java/com/psy/data/db/entity/AccountEntity.kt \
        android/app/src/main/java/com/psy/data/db/mapper/Mappers.kt \
        android/app/src/main/java/com/psy/data/db/PsyDatabase.kt
git commit -m "feat(android): add isFund flag to Account model + Room entity"
```

---

### Task 2: Android — `isFund` on backup `AccountDto` + snapshot version bump

**Files:**
- Modify: `android/app/src/main/java/com/psy/data/backup/SnapshotDto.kt:15,36-42,92-93`

- [ ] **Step 1: Bump the snapshot version**

In `SnapshotDto.kt` line 15, change `val version: Int = 2,` to `val version: Int = 3,`.

- [ ] **Step 2: Add `isFund` to `AccountDto` (last field, default false)**

Replace the `AccountDto` data class (lines 35-42):

```kotlin
@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val type: String,
    val icon: String,
    val color: Long,
    val isFund: Boolean = false,
)
```

The default value makes kotlinx.serialization tolerate v2 blobs that lack the field (missing → false).

- [ ] **Step 3: Update the `AccountDto` mappers**

Replace lines 92-93:

```kotlin
fun AccountEntity.toDto() = AccountDto(id, name, type, icon, color, isFund)
fun AccountDto.toEntity() = AccountEntity(id, name, type, icon, color, isFund)
```

- [ ] **Step 4: Build to verify**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/psy/data/backup/SnapshotDto.kt
git commit -m "feat(android): backup AccountDto carries isFund, snapshot v3"
```

---

### Task 3: iOS — `isFund` on Account (PsyCore) + SwiftData entity + mappers

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/Models.swift:13-22`
- Modify: `ios/Psy/Data/Persistence/Entities.swift:16-26`
- Modify: `ios/Psy/Data/Persistence/Mappers.swift:12-20`

- [ ] **Step 1: Add `isFund` to the PsyCore domain model**

In `ios/PsyCore/Sources/PsyCore/Models.swift`, replace the `Account` struct (lines 13-22):

```swift
public struct Account: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var type: AccountType
    public var icon: String
    public var color: Int64 // ARGB packed
    public var isFund: Bool // fund (quỹ): excluded from income/expense stats & budget
    public init(id: Int64 = 0, name: String, type: AccountType, icon: String, color: Int64, isFund: Bool = false) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color; self.isFund = isFund
    }
}
```

The trailing `isFund: Bool = false` keeps existing call sites (e.g. `EngineTests.acc(...)`) compiling.

- [ ] **Step 2: Add `isFund` to the SwiftData entity**

In `ios/Psy/Data/Persistence/Entities.swift`, replace `AccountEntity` (lines 16-26):

```swift
@Model
final class AccountEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var type: String
    var icon: String
    var color: Int64
    var isFund: Bool = false
    init(id: Int64, name: String, type: String, icon: String, color: Int64, isFund: Bool = false) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color; self.isFund = isFund
    }
}
```

The `= false` default lets SwiftData lightweight-migrate existing stores (additive property).

- [ ] **Step 3: Update the iOS mappers**

In `ios/Psy/Data/Persistence/Mappers.swift`, replace the `AccountEntity` extension (lines 12-20):

```swift
extension AccountEntity {
    func toDomain() -> Account {
        Account(id: id, name: name, type: AccountType(rawValue: type) ?? .cash, icon: icon, color: color, isFund: isFund)
    }
    func apply(_ d: Account) { name = d.name; type = d.type.rawValue; icon = d.icon; color = d.color; isFund = d.isFund }
    convenience init(from d: Account, id: Int64) {
        self.init(id: id, name: d.name, type: d.type.rawValue, icon: d.icon, color: d.color, isFund: d.isFund)
    }
}
```

- [ ] **Step 4: Build PsyCore to verify**

Run:
```bash
cd ios/PsyCore && swift build
```
Expected: Build complete.

- [ ] **Step 5: Commit**

```bash
git add ios/PsyCore/Sources/PsyCore/Models.swift \
        ios/Psy/Data/Persistence/Entities.swift \
        ios/Psy/Data/Persistence/Mappers.swift
git commit -m "feat(ios): add isFund flag to Account domain + SwiftData entity"
```

---

### Task 4: iOS — `isFund` on `AccountDTO` (backward-compatible decode) + version bump

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/SnapshotDTO.swift:14,50-59`

- [ ] **Step 1: Bump the default snapshot version**

In `SnapshotDTO.swift`, change the `init` default on line 14 from `version: Int = 2` to `version: Int = 3`.

- [ ] **Step 2: Add `isFund` to `AccountDTO` with a backward-compatible decoder**

Replace the `AccountDTO` struct (lines 50-59):

```swift
public struct AccountDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var type: String
    public var icon: String
    public var color: Int64
    public var isFund: Bool
    public init(id: Int64, name: String, type: String, icon: String, color: Int64, isFund: Bool = false) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color; self.isFund = isFund
    }

    enum CodingKeys: String, CodingKey { case id, name, type, icon, color, isFund }

    // isFund defaults to false when decoding a v2 blob that lacks the key.
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int64.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        type = try c.decode(String.self, forKey: .type)
        icon = try c.decode(String.self, forKey: .icon)
        color = try c.decode(Int64.self, forKey: .color)
        isFund = try c.decodeIfPresent(Bool.self, forKey: .isFund) ?? false
    }
}
```

(Encoding stays synthesized — it emits `isFund` as a JSON bool, matching kotlinx output.)

- [ ] **Step 3: Build PsyCore to verify**

Run:
```bash
cd ios/PsyCore && swift build
```
Expected: Build complete.

- [ ] **Step 4: Commit**

```bash
git add ios/PsyCore/Sources/PsyCore/SnapshotDTO.swift
git commit -m "feat(ios): backup AccountDTO carries isFund, snapshot v3, v2-compatible decode"
```

---

## Phase B — Stats & budget exclusion (core logic)

### Task 5: iOS `StatsEngine` — exclude funds + flag `AccountStat` (with test)

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/StatsEngine.swift:17-25,64-72`
- Test: `ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift`

- [ ] **Step 1: Write the failing test**

In `ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift`, add this test method inside the existing test class (it uses the file's existing `acc(...)` helper; create local categories/groups/transactions inline). Use a fixed `Calendar`/`Date` as the existing tests do — copy the calendar/now setup from a neighbouring StatsEngine test in this file:

```swift
func testFundAccountExcludedFromSummaryButKeptInBreakdown() {
    let cal = Calendar(identifier: .gregorian)
    let now = Date(timeIntervalSince1970: 1_750_000_000) // mid-2025, any fixed instant
    let month = PsyMonth.current(cal, now: now)
    let mid = month.startMillis(cal) + 86_400_000 // 1 day into the month

    let normal = Account(id: 1, name: "Tiền mặt", type: .cash, icon: "wallet", color: 0)
    let fund = Account(id: 2, name: "M2", type: .cash, icon: "wallet", color: 0, isFund: true)
    let group = CategoryGroup(id: 10, name: "Ăn uống", icon: "utensils", color: 0, type: .expense, sortOrder: 0)
    let leaf = Category(id: 100, groupId: 10, name: "Cà phê", icon: "coffee", sortOrder: 0)

    let txNormal = Transaction(id: 1, ledgerId: 1, type: .expense, amountMinor: 7_000,
                               categoryId: 100, accountId: 1, note: "", date: mid,
                               createdAt: mid, updatedAt: mid)
    let txFund = Transaction(id: 2, ledgerId: 1, type: .expense, amountMinor: 100_000,
                             categoryId: 100, accountId: 2, note: "", date: mid,
                             createdAt: mid, updatedAt: mid)

    let r = StatsEngine.build(windowTransactions: [txNormal, txFund], categories: [leaf],
                              groups: [group], accounts: [normal, fund], month: month,
                              pieMode: .expense, accountFilter: nil, calendar: cal, now: now)

    // Summary excludes the fund tx → only 7,000 expense.
    XCTAssertEqual(r.summary.expenseMinor, 7_000)
    // Pie total excludes the fund tx.
    XCTAssertEqual(r.slices.reduce(Int64(0)) { $0 + $1.amountMinor }, 7_000)
    // By-account breakdown still contains the fund account, flagged.
    let m2 = r.accountBreakdown.first { $0.id == 2 }
    XCTAssertNotNil(m2)
    XCTAssertTrue(m2!.isFund)
    XCTAssertEqual(m2!.expenseMinor, 100_000)
    // Explicitly filtering to the fund account shows its real numbers.
    let filtered = StatsEngine.build(windowTransactions: [txNormal, txFund], categories: [leaf],
                                     groups: [group], accounts: [normal, fund], month: month,
                                     pieMode: .expense, accountFilter: 2, calendar: cal, now: now)
    XCTAssertEqual(filtered.summary.expenseMinor, 100_000)
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
cd ios/PsyCore && swift test --filter testFundAccountExcludedFromSummaryButKeptInBreakdown
```
Expected: FAIL — `AccountStat` has no member `isFund` (compile error) / assertion failure.

- [ ] **Step 3: Add `isFund` to `AccountStat`**

In `StatsEngine.swift`, replace the `AccountStat` struct (lines 17-25):

```swift
public struct AccountStat: Identifiable, Sendable {
    public let id: Int64
    public let name: String
    public let icon: String
    public let color: Int64
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
    public let isFund: Bool
}
```

- [ ] **Step 4: Set `isFund` on the breakdown rows and exclude funds from the all-accounts window**

In `StatsEngine.swift`, replace lines 64-72 (the `accountBreakdown` build + the `filteredWindow` line):

```swift
        let accountBreakdown: [AccountStat] = byAccount.compactMap { id, sums in
            guard let acc = accMap[id] else { return nil }
            return AccountStat(id: acc.id, name: acc.name, icon: acc.icon, color: acc.color,
                               incomeMinor: sums.inc, expenseMinor: sums.exp, netMinor: sums.inc - sums.exp,
                               isFund: acc.isFund)
        }.sorted { ($0.incomeMinor + $0.expenseMinor) > ($1.incomeMinor + $1.expenseMinor) }

        // Drop filter if the account no longer exists.
        let effectiveFilter = accountFilter.flatMap { accMap[$0] != nil ? $0 : nil }
        let fundIds = Set(accounts.filter { $0.isFund }.map { $0.id })
        // "All accounts" view excludes fund txns; explicitly filtering to one account
        // (even a fund) shows that account's real numbers.
        let filteredWindow: [Transaction]
        if let f = effectiveFilter {
            filteredWindow = windowTransactions.filter { $0.accountId == f }
        } else {
            filteredWindow = windowTransactions.filter { !fundIds.contains($0.accountId) }
        }
```

Everything downstream (`monthTxns`, summary, pie, top, trend) already derives from `filteredWindow`, so no further edits are needed.

- [ ] **Step 5: Run the test to verify it passes**

Run:
```bash
cd ios/PsyCore && swift test --filter testFundAccountExcludedFromSummaryButKeptInBreakdown
```
Expected: PASS.

- [ ] **Step 6: Run the full PsyCore suite (guard against regressions in other AccountStat callers)**

Run:
```bash
cd ios/PsyCore && swift test
```
Expected: all tests pass. If another test constructs `AccountStat(...)` positionally, add `isFund: false` there.

- [ ] **Step 7: Commit**

```bash
git add ios/PsyCore/Sources/PsyCore/StatsEngine.swift ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift
git commit -m "feat(ios): StatsEngine excludes fund accounts from thu/chi, flags breakdown"
```

---

### Task 6: iOS `BudgetEngine` — exclude funds (with test) + update call site

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/BudgetEngine.swift:30-37`
- Test: `ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift`
- Modify: budget call site (find it in step 4)

- [ ] **Step 1: Write the failing test**

Add to `EngineTests.swift`:

```swift
func testBudgetExcludesFundAccounts() {
    let normal = Account(id: 1, name: "Tiền mặt", type: .cash, icon: "wallet", color: 0)
    let fund = Account(id: 2, name: "M2", type: .cash, icon: "wallet", color: 0, isFund: true)
    let group = CategoryGroup(id: 10, name: "Ăn uống", icon: "utensils", color: 0, type: .expense, sortOrder: 0)
    let leaf = Category(id: 100, groupId: 10, name: "Cà phê", icon: "coffee", sortOrder: 0)
    let t = Int64(1_750_000_000_000)
    let txNormal = Transaction(id: 1, ledgerId: 1, type: .expense, amountMinor: 7_000,
                               categoryId: 100, accountId: 1, note: "", date: t, createdAt: t, updatedAt: t)
    let txFund = Transaction(id: 2, ledgerId: 1, type: .expense, amountMinor: 100_000,
                             categoryId: 100, accountId: 2, note: "", date: t, createdAt: t, updatedAt: t)
    let totalBudget = Budget(id: 1, ledgerId: 1, groupId: nil, amountMinor: 1_000_000)

    let r = BudgetEngine.build(monthTransactions: [txNormal, txFund], budgets: [totalBudget],
                               categories: [leaf], groups: [group], accounts: [normal, fund])
    XCTAssertEqual(r.total?.spentMinor, 7_000) // fund tx excluded
}
```

(Match the `Budget` initializer to the real one — check `Models.swift`; adjust argument labels if needed.)

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
cd ios/PsyCore && swift test --filter testBudgetExcludesFundAccounts
```
Expected: FAIL — `BudgetEngine.build` has no `accounts:` parameter (compile error).

- [ ] **Step 3: Add an `accounts` parameter and filter funds**

In `BudgetEngine.swift`, replace the function signature + the `expenseTxns` line (lines 30-37):

```swift
    public static func build(monthTransactions: [Transaction], budgets: [Budget],
                             categories: [Category], groups: [CategoryGroup], accounts: [Account]) -> BudgetResult {
        let groupMap = Dictionary(groups.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        // leafId → groupId so each EXPENSE tx can be attributed to its parent group.
        let leafToGroup = Dictionary(categories.map { ($0.id, $0.groupId) }, uniquingKeysWith: { a, _ in a })
        let fundIds = Set(accounts.filter { $0.isFund }.map { $0.id })

        // Spent excludes fund-account txns (consistent with Stats "Ngân sách x").
        let expenseTxns = monthTransactions.filter { $0.type == .expense && !fundIds.contains($0.accountId) }
        let totalSpent = expenseTxns.reduce(Int64(0)) { $0 + $1.amountMinor }
```

- [ ] **Step 4: Update the budget call site**

Find the caller of `BudgetEngine.build`:
```bash
grep -rn "BudgetEngine.build" ios/Psy --include=*.swift
```
It is the budget ViewModel (`ios/Psy/Features/Budget/...`). Pass the accounts it already observes (or fetch from `container.accountRepo`). Add `accounts:` as the final argument, e.g.:

```swift
let result = BudgetEngine.build(monthTransactions: txns, budgets: budgets,
                                categories: categories, groups: groups, accounts: accounts)
```

If the budget ViewModel does not yet observe accounts, subscribe to `container.accountRepo.observeAll()` and combine it into the pipeline the same way `monthTransactions` is combined (mirror the pattern already in that file). Read the file fully before editing.

- [ ] **Step 5: Run the test + build**

Run:
```bash
cd ios/PsyCore && swift test --filter testBudgetExcludesFundAccounts && swift build
```
Expected: PASS + build complete.

- [ ] **Step 6: Commit**

```bash
git add ios/PsyCore/Sources/PsyCore/BudgetEngine.swift ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift ios/Psy/Features/Budget
git commit -m "feat(ios): BudgetEngine excludes fund-account spending"
```

---

### Task 7: Android `StatsViewModel` — exclude funds + flag `AccountStat`

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/stats/StatsViewModel.kt:67-76,178-199`

- [ ] **Step 1: Add `isFund` to the `AccountStat` data class**

In `StatsViewModel.kt`, replace the `AccountStat` data class (lines 67-76):

```kotlin
/** Income/expense rollup for a single account in the selected month (transfers excluded). */
data class AccountStat(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netMinor: Long,
    val isFund: Boolean = false,
)
```

- [ ] **Step 2: Set `isFund` on breakdown rows and exclude funds from the all-accounts window**

In `StatsViewModel.kt`, replace the `accountBreakdown` mapping block + the `filteredWindow` block (lines 178-199):

```kotlin
                    val accountBreakdown = byAccount
                        .mapNotNull { (id, sums) ->
                            val acc = accountMap[id] ?: return@mapNotNull null
                            AccountStat(
                                id = acc.id,
                                name = acc.name,
                                icon = acc.icon,
                                color = acc.color,
                                incomeMinor = sums[0],
                                expenseMinor = sums[1],
                                netMinor = sums[0] - sums[1],
                                isFund = acc.isFund,
                            )
                        }
                        .sortedByDescending { it.incomeMinor + it.expenseMinor }

                    // Drop the filter if the selected account no longer exists.
                    val effectiveFilter = currentAccountFilter?.takeIf { accountMap.containsKey(it) }

                    // Fund accounts: excluded from the "Tất cả" view; an explicit account
                    // filter (even a fund) still shows that account's real numbers.
                    val fundAccountIds = accounts.filter { it.isFund }.map { it.id }.toSet()
                    val filteredWindow =
                        if (effectiveFilter == null) windowTxns.filter { it.accountId !in fundAccountIds }
                        else windowTxns.filter { it.accountId == effectiveFilter }
```

Everything downstream (`monthTxns`, summary, pie, top, trend) already derives from `filteredWindow`.

- [ ] **Step 3: Build to verify**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/stats/StatsViewModel.kt
git commit -m "feat(android): StatsViewModel excludes fund accounts from thu/chi, flags breakdown"
```

---

### Task 8: Android `BudgetViewModel` — observe accounts + exclude funds

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/budget/BudgetViewModel.kt:47-53,104-110,236-254`

- [ ] **Step 1: Add an accounts repo dependency**

In `BudgetViewModel.kt`, add the import and constructor param. Add to imports:
```kotlin
import com.psy.domain.model.Account
import com.psy.domain.repository.AccountRepository
```
Replace the constructor (lines 104-110) to add `accountRepo`:
```kotlin
class BudgetViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val groupRepo: CategoryGroupRepository,
    private val ledgerRepo: LedgerRepository,
    private val accountRepo: AccountRepository,
) : ViewModel() {
```

- [ ] **Step 2: Add accounts to the bundled `DomainData`**

Replace the `DomainData` data class (lines 47-53):
```kotlin
/** Domain inputs bundled to keep the main combine within the 5-flow typed limit. */
private data class DomainData(
    val monthTxns: List<com.psy.domain.model.Transaction>,
    val budgets: List<Budget>,
    val categories: List<com.psy.domain.model.Category>,
    val groups: List<CategoryGroup>,
    val accounts: List<Account>,
)
```

- [ ] **Step 3: Feed accounts into the combine and filter funds out of spend**

In the main pipeline, the `domainFlow` currently combines 4 flows. Replace the `domainFlow` definition (around line 242-245) with a 5-flow combine:
```kotlin
                val domainFlow = combine(monthTxnsFlow, budgetsFlow, categoriesFlow, groupsFlow,
                    accountRepo.observeAll()) {
                    monthTxns, budgets, categories, groups, accounts ->
                    DomainData(monthTxns, budgets, categories, groups, accounts)
                }
```
Then update the destructuring in the next `combine` (line 247) to include `accounts`:
```kotlin
                combine(domainFlow, editorSnapshot) { (monthTxns, budgets, categories, groups, accounts), editor ->
```
And replace the `expenseTxns` line (line 253) to exclude fund accounts:
```kotlin
                    // Spent = EXPENSE only; INCOME + TRANSFER + fund-account txns excluded.
                    val fundAccountIds = accounts.filter { it.isFund }.map { it.id }.toSet()
                    val expenseTxns = monthTxns.filter {
                        it.type == TxType.EXPENSE && it.accountId !in fundAccountIds
                    }
```

- [ ] **Step 4: Build to verify**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. (If the 5-arg `combine` destructuring on a `data class` of 5 components errors, note Kotlin only auto-destructures up to `component5` — `DomainData` has exactly 5, so it works.)

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/budget/BudgetViewModel.kt
git commit -m "feat(android): BudgetViewModel excludes fund-account spending"
```

---

## Phase C — UI (editor toggle + "Quỹ" badge)

### Task 9: Android — fund toggle in the account editor

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/manage/account/ManageAccountsViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/manage/account/ManageAccountsScreen.kt`

- [ ] **Step 1: Add `draftIsFund` state to the ViewModel**

In `ManageAccountsViewModel.kt`:
- Add to `ManageAccountsUiState` (after `draftColor`, line 25): `val draftIsFund: Boolean = false,`
- Add a backing flow after line 38: `private val _draftIsFund = MutableStateFlow(false)`
- The `combine` currently takes 7 flows via the vararg `values` form. Add `_draftIsFund` as the 8th flow argument (line 49, before the closing `)`), then read it: `val draftIsFund = values[7] as Boolean` and pass `draftIsFund = draftIsFund` into `ManageAccountsUiState(...)`.
- In `startAdd()` add `_draftIsFund.value = false`.
- In `startEdit(account)` add `_draftIsFund.value = account.isFund`.
- Add a callback:
```kotlin
    fun onIsFundChange(value: Boolean) {
        _draftIsFund.value = value
    }
```
- In `saveEditor()` pass `isFund = _draftIsFund.value` into the `Account(...)` constructor.

- [ ] **Step 2: Add the toggle UI + wire it**

In `ManageAccountsScreen.kt`:
- Add imports: `import androidx.compose.material3.Switch` and `import androidx.compose.foundation.layout.Spacer` (Spacer already imported) and `import androidx.compose.foundation.layout.weight` is via `Modifier.weight` (already used).
- In the `AccountEditor` call (lines 107-115) add `onIsFundChange = viewModel::onIsFundChange,`.
- Add `onIsFundChange: (Boolean) -> Unit,` to the `AccountEditor` signature (after `onColorChange`).
- Inside `AccountEditor`, after the ColorPicker line (line 204), insert:
```kotlin
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Quỹ", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "Không tính vào thu/chi & ngân sách",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text3,
                )
            }
            Switch(checked = state.draftIsFund, onCheckedChange = onIsFundChange)
        }
```
(`Alignment` and `MaterialTheme` are already imported in this file.)

- [ ] **Step 3: Build + commit**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/ui/manage/account/
git commit -m "feat(android): fund toggle in account editor"
```

---

### Task 10: iOS — fund toggle in the account editor

**Files:**
- Modify: `ios/Psy/Features/Manage/ManageAccountsViewModel.swift`
- Modify: `ios/Psy/Features/Manage/ManageAccountsView.swift`

- [ ] **Step 1: Add `draftIsFund` to the ViewModel**

In `ManageAccountsViewModel.swift`:
- Add a property after `draftColor` (line 19): `var draftIsFund: Bool = false`
- In `startAdd()` add: `draftIsFund = false`
- In `startEdit(_:)` add: `draftIsFund = account.isFund`
- In `save()` pass it into the `Account(...)`:
```swift
        let account = Account(
            id: editingId ?? 0,
            name: trimmed,
            type: draftType,
            icon: draftIcon,
            color: draftColor,
            isFund: draftIsFund
        )
```

- [ ] **Step 2: Add the toggle UI**

In `ManageAccountsView.swift`, inside `AccountEditorSheet`'s `VStack`, after the "Màu sắc" block (lines 135-138), insert:
```swift
                    VStack(alignment: .leading, spacing: 8) {
                        Toggle(isOn: $vm.draftIsFund) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Quỹ")
                                    .font(PsyFont.bodyLarge.weight(.semibold))
                                    .foregroundStyle(psyColors.text)
                                Text("Không tính vào thu/chi & ngân sách")
                                    .font(PsyFont.bodyMedium)
                                    .foregroundStyle(psyColors.text3)
                            }
                        }
                        .tint(psyColors.blue)
                    }
```

- [ ] **Step 3: Build + commit**

Run:
```bash
cd ios && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build 2>&1 | tail -5
```
Expected: BUILD SUCCEEDED.
```bash
git add ios/Psy/Features/Manage/
git commit -m "feat(ios): fund toggle in account editor"
```

---

### Task 11: Android — "Quỹ" badge in transaction rows + by-account breakdown

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/components/Cards.kt:48-90`
- Modify: `android/app/src/main/java/com/psy/ui/home/HomeViewModel.kt` (TxRow + builder)
- Modify: `android/app/src/main/java/com/psy/ui/home/HomeScreen.kt` (pass isFund)
- Modify: `android/app/src/main/java/com/psy/ui/calendar/CalendarViewModel.kt` (CalendarTxRow + builder)
- Modify: `android/app/src/main/java/com/psy/ui/calendar/CalendarScreen.kt` (pass isFund)
- Modify: the Stats by-account breakdown renderer (find in step 5)

- [ ] **Step 1: Add an `isFund` param + badge to the shared `TransactionRow`**

In `Cards.kt`, add `isFund: Boolean = false,` to the `TransactionRow` parameter list (after `account: String,`, line 58). Then in the right-hand `Column` (lines 79-88), render a badge under the account text:
```kotlin
        Column(horizontalAlignment = Alignment.End) {
            Text(
                amount,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isIncome) colors.green else colors.red,
            )
            Text(account, color = colors.text3, fontSize = 11.sp)
            if (isFund) {
                Text(
                    "Quỹ",
                    color = colors.text3,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.text3.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
```
(`Modifier`, `clip`, `background`, `RoundedCornerShape`, `padding`, `dp`, `FontWeight` are all already imported in Cards.kt.)

- [ ] **Step 2: Add `isFund` to `TxRow` (Home) and set it in the builder**

In `HomeViewModel.kt`:
- Add `val isFund: Boolean = false,` to the `TxRow` data class (after `accountName`, ~line 37).
- In the row builder where `acc` is resolved (~lines 142-168), set `isFund = acc?.isFund == true,` on both the INCOME/EXPENSE and any non-transfer `TxRow(...)` constructions. (Transfers: leave default false.)

- [ ] **Step 3: Pass `isFund` from `HomeScreen` into `TransactionRow`**

In `HomeScreen.kt`, in the `TransactionRow(...)` call (~line 154), add `isFund = row.isFund,`.

- [ ] **Step 4: Mirror for Calendar**

In `CalendarViewModel.kt`: add `val isFund: Boolean = false,` to `CalendarTxRow` (after `accountName`, ~line 47) and set `isFund = acc?.isFund == true,` in the row builder (~lines 179-185).
In `CalendarScreen.kt`: find the `TransactionRow(...)` call and add `isFund = row.isFund,`. (Run `grep -n "TransactionRow(" android/app/src/main/java/com/psy/ui/calendar/CalendarScreen.kt` to locate it.)

- [ ] **Step 5: Badge the by-account breakdown rows in Stats**

Locate the Stats by-account breakdown renderer:
```bash
grep -rn "accountBreakdown\|AccountStat" android/app/src/main/java/com/psy/ui/stats/*.kt
```
In the composable that renders each `AccountStat` row (the "Theo tài khoản" card), add — next to the account name `Text` — the same badge when `stat.isFund`:
```kotlin
            if (stat.isFund) {
                Text(
                    "Quỹ",
                    color = colors.text3,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.text3.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
```
Add any missing imports (`clip`, `background`, `RoundedCornerShape`) to that file.

- [ ] **Step 6: Build + commit**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.
```bash
git add android/app/src/main/java/com/psy/ui/
git commit -m "feat(android): Quỹ badge on transaction rows + by-account breakdown"
```

---

### Task 12: iOS — "Quỹ" badge in transaction rows + by-account breakdown

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/ViewData.swift:5-28,96-111`
- Modify: `ios/Psy/UI/Components/HGComponents.swift:84-105`
- Modify: `ios/Psy/Features/Home/HomeView.swift:182`
- Modify: `ios/Psy/Features/Calendar/CalendarView.swift:193`
- Modify: `ios/Psy/Features/Stats/StatsView.swift` (by-account row)

- [ ] **Step 1: Add `isFund` to `TxRow` + its factory**

In `ViewData.swift`:
- Add `public let isFund: Bool` to the `TxRow` struct (after `accountName`/`toAccountName`, around line 13), add it to the `init` (default `= false` for safety), and set `self.isFund = isFund`.
- In the `TxRow.from(...)` factory (lines 96-111), set `isFund: acc?.isFund ?? false` on the non-transfer `TxRow(...)` construction (line 111 area). Leave transfer rows default.

Read lines 5-28 and 96-111 first, then make the edits matching the exact initializer argument order.

- [ ] **Step 2: Add an `isFund` param + badge to `TransactionRowView`**

In `HGComponents.swift`, add `var isFund: Bool = false` to `TransactionRowView` (after `let account: String`, line 92). After the account `Text` (line 105), add:
```swift
                if isFund {
                    Text("Quỹ")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(psyColors.text3)
                        .padding(.horizontal, 6).padding(.vertical, 1)
                        .background(psyColors.text3.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
                        .padding(.top, 2)
                }
```

- [ ] **Step 3: Pass `isFund` at the row call sites**

In `HomeView.swift` (around line 182) and `CalendarView.swift` (around line 193), add `isFund: row.isFund` to the `TransactionRowView(...)` initializer call. Read the surrounding lines to insert the argument in the right position.

- [ ] **Step 4: Badge the by-account breakdown in StatsView**

In `ios/Psy/Features/Stats/StatsView.swift`, find where each `AccountStat` row renders the account name (`grep -n "name\|AccountStat\|accountBreakdown" ios/Psy/Features/Stats/StatsView.swift`). Next to the name, add when `stat.isFund`:
```swift
                if stat.isFund {
                    Text("Quỹ")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(psyColors.text3)
                        .padding(.horizontal, 6).padding(.vertical, 1)
                        .background(psyColors.text3.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
                }
```
Note: `StatsView.swift` already has uncommitted changes (it is `M` in git status) — preserve those and add to them.

- [ ] **Step 5: Build + commit**

Run:
```bash
cd ios/PsyCore && swift build && cd .. && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build 2>&1 | tail -5
```
Expected: BUILD SUCCEEDED.
```bash
git add ios/PsyCore/Sources/PsyCore/ViewData.swift ios/Psy/UI/Components/HGComponents.swift ios/Psy/Features/
git commit -m "feat(ios): Quỹ badge on transaction rows + by-account breakdown"
```

---

## Phase D — Icon picker expansion (~36 → ~90)

The shared ordered icon-name list below is appended (in this exact order) to **both** platforms' picker sets. Each entry is `kebab-name` (iOS asset/string) → `Lucide.PascalCase` (Android Compose symbol).

```
pizza→Pizza, sandwich→Sandwich, ice-cream-cone→IceCreamCone, cake→Cake, wine→Wine,
milk→Milk, soup→Soup, salad→Salad, cookie→Cookie, apple→Apple, fish→Fish, egg→Egg,
ham→Ham, drumstick→Drumstick, candy→Candy, ship→Ship, truck→Truck, footprints→Footprints,
plane-takeoff→PlaneTakeoff, store→Store, shopping-basket→ShoppingBasket, tag→Tag, tags→Tags,
gem→Gem, watch→Watch, glasses→Glasses, baby→Baby, zap→Zap, droplet→Droplet, flame→Flame,
wifi→Wifi, phone→Phone, plug→Plug, recycle→Recycle, stethoscope→Stethoscope,
heart-pulse→HeartPulse, activity→Activity, syringe→Syringe, brain→Brain, film→Film, tv→Tv,
headphones→Headphones, mic→Mic, camera→Camera, ticket→Ticket, party-popper→PartyPopper,
dices→Dices, book→Book, book-open→BookOpen, star→Star, briefcase→Briefcase, building→Building,
building-2→Building2, coins→Coins, piggy-bank→PiggyBank, hand-coins→HandCoins,
calculator→Calculator, chart-pie→ChartPie, percent→Percent, dollar-sign→DollarSign,
users→Users, heart→Heart, cat→Cat, bone→Bone, paw-print→PawPrint, map-pin→MapPin, map→Map,
luggage→Luggage, tent→Tent, mountain→Mountain, hotel→Hotel, compass→Compass, bed→Bed,
sofa→Sofa, lamp→Lamp, key→Key
```

**Self-correcting rule:** if Android fails to compile a `Lucide.X` (symbol absent in the library version) OR the iOS `curl` returns HTTP 404 for `<name>.svg`, **remove that single icon from BOTH lists** (keep the two platforms identical) and continue.

### Task 13: Android — extend `byName` map + explicit ordered `pickerSet`

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/icons/LucideIcon.kt`

- [ ] **Step 1: Add the new icons to `byName`**

In `LucideIcon.kt`, inside the `byName = mapOf(...)` (before the closing `)` on line 60), append entries for every icon in the shared list, e.g.:
```kotlin
        "pizza" to Lucide.Pizza,
        "sandwich" to Lucide.Sandwich,
        "ice-cream-cone" to Lucide.IceCreamCone,
        // ... one line per icon in the shared list ...
        "key" to Lucide.Key,
```

- [ ] **Step 2: Make `pickerSet` an explicit ordered list (parity with iOS)**

The current `pickerSet = byName.keys.toList()` leaks UI-chrome icons (chart-column, arrow-right-left, list, etc.) into the picker. Replace line 63 with an explicit ordered list = the original category icons (the first 36 entries, excluding chrome) **followed by** the shared new list, so it matches iOS's `pickerSet` exactly:
```kotlin
    /** Icons offered in the picker (ordered). Must stay identical to iOS LucideIcons.pickerSet. */
    val pickerSet: List<String> = listOf(
        "wallet", "landmark", "utensils", "shopping-cart", "coffee", "cup-soda", "bus", "bike",
        "fuel", "train-front", "square-parking", "car", "shopping-bag", "shirt", "package",
        "receipt", "lightbulb", "globe", "gamepad-2", "banknote", "gift", "circle-dollar-sign",
        "house", "pill", "hospital", "smartphone", "plane", "graduation-cap", "dog", "credit-card",
        "trending-up", "dumbbell", "music", "umbrella", "beer", "clapperboard",
        // ── expansion (keep in sync with iOS) ──
        "pizza", "sandwich", "ice-cream-cone", "cake", "wine", "milk", "soup", "salad", "cookie",
        "apple", "fish", "egg", "ham", "drumstick", "candy", "ship", "truck", "footprints",
        "plane-takeoff", "store", "shopping-basket", "tag", "tags", "gem", "watch", "glasses",
        "baby", "zap", "droplet", "flame", "wifi", "phone", "plug", "recycle", "stethoscope",
        "heart-pulse", "activity", "syringe", "brain", "film", "tv", "headphones", "mic", "camera",
        "ticket", "party-popper", "dices", "book", "book-open", "star", "briefcase", "building",
        "building-2", "coins", "piggy-bank", "hand-coins", "calculator", "chart-pie", "percent",
        "dollar-sign", "users", "heart", "cat", "bone", "paw-print", "map-pin", "map", "luggage",
        "tent", "mountain", "hotel", "compass", "bed", "sofa", "lamp", "key",
    )
```

- [ ] **Step 3: Build (this verifies every `Lucide.X` symbol exists)**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. If a symbol is unresolved (e.g. `Lucide.PawPrint`), remove that icon from `byName` AND `pickerSet` here AND from the iOS lists in Task 14, then rebuild.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/icons/LucideIcon.kt
git commit -m "feat(android): expand icon picker (~36 → ~90), explicit ordered pickerSet"
```

---

### Task 14: iOS — scaffold SVG imagesets + extend `bundled`/`pickerSet`

**Files:**
- Create: `ios/scripts/fetch-lucide-icons.sh`
- Create: `ios/Psy/Resources/Assets.xcassets/lucide-<name>.imageset/` (one per new icon)
- Modify: `ios/Psy/UI/Icons/LucideIcon.swift:8-27`

- [ ] **Step 1: Write the fetch script**

Create `ios/scripts/fetch-lucide-icons.sh`:
```bash
#!/usr/bin/env bash
# Scaffolds lucide-<name>.imageset asset folders from lucide-static v1.21.0.
# Usage: ./fetch-lucide-icons.sh name1 name2 ...
set -euo pipefail
ASSETS="$(cd "$(dirname "$0")/.." && pwd)/Psy/Resources/Assets.xcassets"
VER="1.21.0"
for name in "$@"; do
  dir="$ASSETS/lucide-$name.imageset"
  mkdir -p "$dir"
  url="https://unpkg.com/lucide-static@$VER/icons/$name.svg"
  if ! curl -fsSL "$url" -o "$dir/$name.svg"; then
    echo "MISSING: $name (HTTP error from $url)" >&2
    rmdir "$dir" 2>/dev/null || true
    continue
  fi
  cat > "$dir/Contents.json" <<JSON
{
  "images" : [ { "filename" : "$name.svg", "idiom" : "universal" } ],
  "info" : { "author" : "xcode", "version" : 1 },
  "properties" : { "preserves-vector-representation" : true, "template-rendering-intent" : "template" }
}
JSON
  echo "OK: $name"
done
```

- [ ] **Step 2: Run the script for the shared icon list**

Run (one space-separated list of all new kebab names):
```bash
chmod +x ios/scripts/fetch-lucide-icons.sh
ios/scripts/fetch-lucide-icons.sh \
  pizza sandwich ice-cream-cone cake wine milk soup salad cookie apple fish egg ham drumstick \
  candy ship truck footprints plane-takeoff store shopping-basket tag tags gem watch glasses baby \
  zap droplet flame wifi phone plug recycle stethoscope heart-pulse activity syringe brain film tv \
  headphones mic camera ticket party-popper dices book book-open star briefcase building building-2 \
  coins piggy-bank hand-coins calculator chart-pie percent dollar-sign users heart cat bone paw-print \
  map-pin map luggage tent mountain hotel compass bed sofa lamp key
```
Expected: `OK: <name>` for each. Note any `MISSING:` lines — those names must be removed from the iOS `bundled`/`pickerSet` (Step 3) AND from Android's lists (Task 13). If `unpkg` is unreachable in the execution environment, source the SVGs another way (e.g. a local `lucide-static@1.21.0` checkout) into the same imageset structure.

- [ ] **Step 3: Extend `bundled` + `pickerSet`**

In `ios/Psy/UI/Icons/LucideIcon.swift`, append every new kebab name to the `bundled` set (lines 8-18) AND to `pickerSet` (lines 21-27) in the same shared order as Android. The resulting `pickerSet` must be byte-for-byte the same name sequence as Android's `pickerSet` from Task 13 Step 2.

- [ ] **Step 4: Build the app (verifies imagesets resolve)**

Run:
```bash
cd ios && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build 2>&1 | tail -5
```
Expected: BUILD SUCCEEDED. (Missing asset references only warn at build but render the fallback at runtime — confirm in Step 5 of Task 15.)

- [ ] **Step 5: Commit**

```bash
git add ios/scripts/fetch-lucide-icons.sh ios/Psy/Resources/Assets.xcassets ios/Psy/UI/Icons/LucideIcon.swift
git commit -m "feat(ios): expand icon picker (~36 → ~90) from lucide-static SVGs"
```

---

## Phase E — Verification

### Task 15: Full build + manual parity check

- [ ] **Step 1: Android full debug build**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android && ./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: iOS full build + PsyCore tests**

Run:
```bash
cd ios/PsyCore && swift test && cd .. && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build 2>&1 | tail -5
```
Expected: all tests pass, BUILD SUCCEEDED.

- [ ] **Step 3: Manual check — Android emulator**

Install & run; create or edit an account, toggle **Quỹ** on (e.g. "M2"). Add an expense on M2 and an expense on a normal account. Verify:
- Stats "Tất cả": Thu/Chi totals, pie, and trend exclude the M2 expense.
- "Theo tài khoản": M2 still listed, with a "Quỹ" badge.
- Tapping the M2 account filter chip shows M2's real numbers.
- Home/Calendar transaction list: the M2 transaction shows a "Quỹ" badge.
- Budget screen: the M2 expense is not counted in spent.
- Icon picker: ~90 icons visible and searchable.

- [ ] **Step 4: Manual check — iOS simulator**

Repeat the same checklist on iOS. Confirm parity with Android (badge placement, exclusion behavior, icon set).

- [ ] **Step 5: Confirm icon parity**

Run a quick diff of the two `pickerSet` orders to ensure they match:
```bash
grep -A40 "pickerSet" ios/Psy/UI/Icons/LucideIcon.swift
grep -A40 "pickerSet" android/app/src/main/java/com/psy/ui/icons/LucideIcon.kt
```
Names + order must be identical (minus any icon dropped on both sides per the self-correcting rule).

- [ ] **Step 6: Final commit (if any fixups)**

```bash
git add -A && git commit -m "chore: fund accounts + icon expansion — verification fixups"
```

---

## Self-review notes

- **Spec coverage:** data model (Tasks 1-4), stats exclusion + by-account flag (Tasks 5,7), budget exclusion (Tasks 6,8), editor toggle (Tasks 9,10), transaction-row + breakdown badge (Tasks 11,12), icon expansion (Tasks 13,14), version bump 2→3 + backward-compatible decode (Tasks 2,4), tests as regression guard (Tasks 5,6), parity verification (Task 15). All spec sections mapped.
- **Balance non-goal:** intentionally untouched (no task changes balance math) — matches the spec's scope decision.
- **Type consistency:** `isFund` field name identical across `Account`/`AccountEntity`/`AccountDto`/`AccountDTO`/`AccountStat`/`TxRow`/`CalendarTxRow` on both platforms; `AccountStat.isFund` and `TxRow.isFund` referenced consistently by the UI tasks.
```
