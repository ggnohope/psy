# iOS Port — Plan 4b: Feature Screens

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`).

**Goal:** Build the six offline feature screens (Home, Add/Edit, Manage Accounts, Manage Categories, Calendar, Stats, Budget) as SwiftUI ViewModels + Views, wired to the PsyCore engines (Plan 4a) and repositories (Plan 2), replacing the RootView tab stubs. Visual/UX parity with the Android Compose screens.

**Architecture:** Each screen has an `@MainActor @Observable` ViewModel that subscribes to repository Combine publishers (the Flow analog), runs the relevant PsyCore engine, and publishes state. Views read the `psyColors`/`PsyFont` theme. The ViewModel wiring follows the **canonical pattern** below. The Android Compose screen is the layout source of truth — port it faithfully.

**Tech Stack:** SwiftUI, Combine, Swift Charts (Stats), PhotosUI (Add/Edit photo).

**Reference (port these layouts):**
- `android/.../ui/home/HomeScreen.kt`
- `android/.../ui/addedit/AddEditTransactionScreen.kt`
- `android/.../ui/manage/account/ManageAccountsScreen.kt`, `.../manage/category/ManageCategoriesScreen.kt`
- `android/.../ui/components/IconColorPicker.kt`, `BudgetProgress.kt`, `MonthSelector.kt`
- `android/.../ui/calendar/CalendarScreen.kt`
- `android/.../ui/stats/StatsScreen.kt`, `.../ui/components/charts/DonutChart.kt`, `TrendBars.kt`
- `android/.../ui/budget/BudgetScreen.kt`
- The matching `*ViewModel.kt` files (logic already ported into PsyCore engines — use them, don't re-derive).

**Prerequisites:** Plans 1, 2, 4a landed. Build/screenshot verify per task:
```
cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build
```
Screenshot helper (used in each screen task):
```bash
APP=$(xcodebuild -project ios/Psy.xcodeproj -scheme Psy -showBuildSettings -destination 'platform=iOS Simulator,name=iPhone 17' 2>/dev/null | awk -F' = ' '/ TARGET_BUILD_DIR /{d=$2} / FULL_PRODUCT_NAME /{p=$2} END{print d"/"p}')
xcrun simctl boot "iPhone 17" 2>/dev/null; open -a Simulator 2>/dev/null
xcrun simctl install booted "$APP"; xcrun simctl launch booted com.psy; sleep 3
xcrun simctl io booted screenshot /private/tmp/claude-501/-Users-hoalam-Codes-psy/92a0178c-85c8-4058-a0d6-f7ded334a8e4/scratchpad/<name>.png
```

---

## Canonical ViewModel pattern (READ FIRST — every screen follows this)

- `@MainActor @Observable final class XViewModel` with a `private let container: AppContainer`, a `let calendar: Calendar` (Gregorian, `TimeZone.current`), and `private var cancellables = Set<AnyCancellable>()`.
- State is stored as plain `var` properties (SwiftUI observes them via `@Observable`).
- The Flow→Combine mapping mirrors Android's `ledgerRepo.observeAll().flatMapLatest { … combine(...) }`:
  - `container.ledgerRepo.observeAll()` → `.map { ledgers -> AnyPublisher<…?, Never> in … }` → `.switchToLatest()` → `.receive(on: RunLoop.main)` → `.sink`.
  - Inside the map, build the inner publisher with `Publishers.CombineLatest3/4` of the needed repo publishers, then `.map { … EngineCall … }`.
- For month-navigating screens (Stats/Calendar/Budget), add `private let monthSubject = CurrentValueSubject<PsyMonth, Never>(...)` and combine it into the chain so changing the month re-queries. `prevMonth()/nextMonth()` mutate `monthSubject.value` via `PsyMonth.adding(±1, calendar)`.
- The app target is Swift 5 language mode, so capturing `container`/`calendar` in these closures is fine.
- Views are built with `@State private var vm: XViewModel`, initialized `init(container:) { _vm = State(initialValue: XViewModel(container: container)) }`. RootView passes `container` into each tab.

Example currency: `Currency.of(ledger.currency)`. Amount display via `MoneyText`/`currency.format(_:)`.

---

## Task 1: PhotoStorage + AddEdit screen

**Files:**
- Create: `ios/Psy/Data/Photo/PhotoStorage.swift`
- Create: `ios/Psy/Features/AddEdit/AddEditViewModel.swift`
- Create: `ios/Psy/Features/AddEdit/AddEditView.swift`
- Modify: `ios/Psy/Info.plist` (add `NSPhotoLibraryUsageDescription`)

**Context:** Mirrors `AddEditTransactionViewModel` + `AddEditTransactionScreen.kt`. Type segmented control (Chi/Thu/Chuyển khoản), amount field (digits only), category grid (by type), account picker, to-account for TRANSFER, date picker, note, photo attach via `PhotosPicker` (copy into `Documents/photos/`), save + delete (delete only when editing). Use `AddEditLogic` from PsyCore for amount parsing + `canSave`. This screen is presented as a sheet; on save/delete it dismisses.

- [ ] **Step 1: PhotoStorage**

`ios/Psy/Data/Photo/PhotoStorage.swift`:
```swift
import Foundation

/// Stores attached photos in the app's Documents/photos directory. Returns the absolute file path
/// (mirrors Android's internal-storage path stored in Transaction.photoUri).
enum PhotoStorage {
    private static var dir: URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let d = docs.appendingPathComponent("photos", isDirectory: true)
        try? FileManager.default.createDirectory(at: d, withIntermediateDirectories: true)
        return d
    }

    static func save(data: Data, name: String) throws -> String {
        let url = dir.appendingPathComponent("\(name).jpg")
        try data.write(to: url)
        return url.path
    }

    static func delete(path: String) {
        try? FileManager.default.removeItem(atPath: path)
    }
}
```

- [ ] **Step 2: Info.plist photo permission**

In `ios/Psy/Info.plist`, add inside the top-level `<dict>`:
```xml
    <key>NSPhotoLibraryUsageDescription</key>
    <string>Đính kèm ảnh hoá đơn vào giao dịch.</string>
```

- [ ] **Step 3: AddEditViewModel**

`ios/Psy/Features/AddEdit/AddEditViewModel.swift`:
```swift
import Foundation
import SwiftUI
import PsyCore

@MainActor @Observable
final class AddEditViewModel {
    private let container: AppContainer
    private let txId: Int64          // 0 = new
    private var originalCreatedAt: Int64 = 0
    private var ledgerId: Int64 = 0

    var isEdit: Bool { txId != 0 }
    var type: TxType = .expense
    var amountText: String = ""
    var categories: [Category] = []
    var accounts: [Account] = []
    var selectedCategoryId: Int64?
    var selectedAccountId: Int64?
    var toAccountId: Int64?
    var date: Date = Date()
    var note: String = ""
    var currency: Currency = .vnd
    var photoUri: String?
    var photoErrorMessage: String?

    var canSave: Bool {
        AddEditLogic.canSave(amountText: amountText, type: type, categoryId: selectedCategoryId,
                             accountId: selectedAccountId, toAccountId: toAccountId)
    }

    init(container: AppContainer, txId: Int64) {
        self.container = container
        self.txId = txId
        load()
    }

    private func load() {
        let ledger = container.ledgerRepo.firstOrNull()
        ledgerId = ledger?.id ?? 0
        currency = ledger.map { Currency.of($0.currency) } ?? .vnd
        accounts = currentAccounts()
        selectedAccountId = accounts.first?.id

        if isEdit, let tx = container.transactionRepo.getById(txId) {
            type = tx.type
            amountText = AddEditLogic.typedString(amountMinor: tx.amountMinor, fractionDigits: currency.fractionDigits)
            if tx.type == .transfer { selectedAccountId = tx.accountId; toAccountId = tx.toAccountId; selectedCategoryId = nil }
            else { selectedCategoryId = tx.categoryId; selectedAccountId = tx.accountId }
            date = Date(timeIntervalSince1970: Double(tx.date) / 1000)
            note = tx.note
            photoUri = tx.photoUri
            originalCreatedAt = tx.createdAt
        }
        reloadCategories()
    }

    private func currentAccounts() -> [Account] {
        var result: [Account] = []
        let sem = DispatchSemaphore(value: 0)
        _ = container.accountRepo.observeAll().first().sink { result = $0; sem.signal() }
        sem.wait()
        return result
    }
    // NOTE: prefer a synchronous fetch helper on the repo if the semaphore feels heavy — see Step 3a.

    func reloadCategories() {
        guard type != .transfer else { categories = []; selectedCategoryId = nil; return }
        let target: CategoryType = (type == .income) ? .income : .expense
        var result: [Category] = []
        let sem = DispatchSemaphore(value: 0)
        _ = container.categoryRepo.observeByType(target).first().sink { result = $0; sem.signal() }
        sem.wait()
        categories = result
        if !categories.contains(where: { $0.id == selectedCategoryId }) { selectedCategoryId = nil }
    }

    func onTypeChange(_ newType: TxType) {
        type = newType
        if newType == .transfer { categories = []; selectedCategoryId = nil }
        else { toAccountId = nil; reloadCategories() }
    }

    func attachPhoto(data: Data) {
        photoErrorMessage = nil
        do {
            let name = "img_\(Int(Date().timeIntervalSince1970 * 1000))"
            photoUri = try PhotoStorage.save(data: data, name: name)
        } catch { photoErrorMessage = "Không thể đính kèm ảnh: \(error.localizedDescription)" }
    }

    func removePhoto() {
        if let p = photoUri { PhotoStorage.delete(path: p) }
        photoUri = nil
    }

    func save(onDone: () -> Void) {
        guard canSave else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let amountMinor = AddEditLogic.amountMinor(typed: amountText, fractionDigits: currency.fractionDigits)
        let dateMillis = Int64(date.timeIntervalSince1970 * 1000)
        let tx: Transaction
        if type == .transfer {
            tx = Transaction(id: txId, ledgerId: ledgerId, type: .transfer, amountMinor: amountMinor,
                             categoryId: nil, accountId: selectedAccountId!, toAccountId: toAccountId,
                             note: note, date: dateMillis, createdAt: isEdit ? originalCreatedAt : now, updatedAt: now, photoUri: photoUri)
        } else {
            tx = Transaction(id: txId, ledgerId: ledgerId, type: type, amountMinor: amountMinor,
                             categoryId: selectedCategoryId, accountId: selectedAccountId!, toAccountId: nil,
                             note: note, date: dateMillis, createdAt: isEdit ? originalCreatedAt : now, updatedAt: now, photoUri: photoUri)
        }
        container.transactionRepo.upsert(tx)
        onDone()
    }

    func delete(onDone: () -> Void) {
        guard isEdit, let tx = container.transactionRepo.getById(txId) else { return }
        container.transactionRepo.delete(tx)
        onDone()
    }
}
```

- [ ] **Step 3a (DRY helper — apply before building):** the `DispatchSemaphore` synchronous reads above are a stopgap. Instead, add tiny synchronous fetch helpers to the repos and use them here. Add to `AccountRepository`: `func all() -> [Account]` and to `CategoryRepository`: `func byType(_ type: CategoryType) -> [Category]` (same FetchDescriptor as their `observe*`, returning the mapped array directly). Replace `currentAccounts()`/`reloadCategories()` bodies to call these. This avoids semaphores on the main actor.

`AccountRepository` add:
```swift
    func all() -> [Account] {
        let d = FetchDescriptor<AccountEntity>(sortBy: [SortDescriptor(\.id, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }
```
`CategoryRepository` add:
```swift
    func byType(_ type: CategoryType) -> [Category] {
        let raw = type.rawValue
        let d = FetchDescriptor<CategoryEntity>(predicate: #Predicate { $0.type == raw },
            sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }
    func all() -> [Category] {
        let d = FetchDescriptor<CategoryEntity>(sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
        return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
    }
```
Then in the VM: `accounts = container.accountRepo.all()` and `categories = container.categoryRepo.byType(target)`.

- [ ] **Step 4: AddEditView** — port `AddEditTransactionScreen.kt` to SwiftUI. Read that Kotlin file for layout. Requirements:
  - Title "Thêm giao dịch" / "Sửa giao dịch".
  - Segmented `Picker` for type: Chi (expense) / Thu (income) / Chuyển khoản (transfer) → `vm.onTypeChange`.
  - Amount: large `TextField` with `.keyboardType(.numberPad)`, binding filters digits; show currency symbol.
  - INCOME/EXPENSE: a category grid (LazyVGrid of icon+name chips, selected highlighted with `psyColors.primary`); an account picker (Menu or horizontal chips).
  - TRANSFER: from-account + to-account pickers (must differ), no category, "🔄".
  - Date: `DatePicker` (date only).
  - Note: `TextField`.
  - Photo: `PhotosPicker` → load `Data` → `vm.attachPhoto(data:)`; show thumbnail with remove button; show `photoErrorMessage` if any.
  - Toolbar: Save (disabled unless `vm.canSave`) → `vm.save { dismiss() }`; if editing, a Delete button → `vm.delete { dismiss() }`.
  - Use `@Environment(\.dismiss)`.

- [ ] **Step 5: Build** (build command above). Expected `** BUILD SUCCEEDED **`. (Screenshot happens via Home in Task 2.)
- [ ] **Step 6: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Data/Photo/PhotoStorage.swift ios/Psy/Features/AddEdit ios/Psy/Info.plist ios/Psy/Data/Repositories/AccountRepository.swift ios/Psy/Data/Repositories/CategoryRepository.swift
git commit -m "feat(ios): Add/Edit transaction screen + PhotoStorage"
```

---

## Task 2: Home screen (canonical VM example) + wire tab

**Files:**
- Create: `ios/Psy/Features/Home/HomeViewModel.swift`
- Create: `ios/Psy/Features/Home/HomeView.swift`
- Modify: `ios/Psy/App/RootView.swift` (replace Home stub; pass `container`)

- [ ] **Step 1: HomeViewModel (THE canonical pattern — copy exactly)**

`ios/Psy/Features/Home/HomeViewModel.swift`:
```swift
import Foundation
import Combine
import PsyCore

@MainActor @Observable
final class HomeViewModel {
    private let container: AppContainer
    let calendar: Calendar
    private var cancellables = Set<AnyCancellable>()

    var monthLabel = ""
    var incomeMinor: Int64 = 0
    var expenseMinor: Int64 = 0
    var netMinor: Int64 = 0
    var currency: Currency = .vnd
    var days: [DayGroup] = []
    var loading = true

    init(container: AppContainer) {
        self.container = container
        var c = Calendar(identifier: .gregorian); c.timeZone = .current
        self.calendar = c
        start()
    }

    private func start() {
        let now = Date()
        let month = PsyMonth.current(calendar, now: now)
        monthLabel = month.label
        let start = month.startMillis(calendar), end = month.endMillis(calendar)
        let cal = calendar
        let c = container

        c.ledgerRepo.observeAll()
            .map { ledgers -> AnyPublisher<(Currency, HomeResult)?, Never> in
                guard let ledger = ledgers.first else { return Just(nil).eraseToAnyPublisher() }
                let currency = Currency.of(ledger.currency)
                return Publishers.CombineLatest3(
                    c.transactionRepo.observeBetween(ledgerId: ledger.id, start: start, end: end),
                    c.categoryRepo.observeAll(),
                    c.accountRepo.observeAll()
                )
                .map { txns, cats, accts in
                    (currency, HomeEngine.build(transactions: txns, categories: cats, accounts: accts, calendar: cal, now: now))
                }
                .eraseToAnyPublisher()
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] result in
                guard let self else { return }
                if let (currency, r) = result {
                    self.currency = currency
                    self.incomeMinor = r.incomeMinor
                    self.expenseMinor = r.expenseMinor
                    self.netMinor = r.netMinor
                    self.days = r.days
                }
                self.loading = false
            }
            .store(in: &cancellables)
    }
}
```

- [ ] **Step 2: HomeView** — port `HomeScreen.kt`. Read it for layout. Requirements:
  - `NavigationStack`. Title "Psy" or month; a summary header card showing Thu (income, green +), Chi (expense, red −), Còn lại (net) using `MoneyText` with `vm.currency`.
  - A `List`/`ScrollView` of `DayGroup` sections: section header = `dateLabel`; each `TxRow` shows icon, title, account (and `→ toAccountName` for transfers), note, and amount colored by type (income green, expense red/onSurface, transfer neutral). Tapping a row opens `AddEditView(container:txId:)` in a `.sheet` (edit).
  - Toolbar: a "+" button → `.sheet` `AddEditView(container:txId:0)` (new). A gear/menu button → `Menu` with NavigationLinks "Quản lý tài khoản" → `ManageAccountsView(container:)` and "Quản lý danh mục" → `ManageCategoriesView(container:)` (these exist after Task 3; for this task you may temporarily comment the two links, then re-enable in Task 3 — OR implement Task 3 first. Prefer: implement Task 3 before wiring these links.)
  - Empty state when `days` is empty: a friendly "Chưa có giao dịch" placeholder.
  - Background `psyColors.background`.

- [ ] **Step 3: RootView** — replace the Home stub:
```swift
            HomeView(container: container)
                .tabItem { Label("Trang chủ", systemImage: "house.fill") }
```

- [ ] **Step 4: Build + screenshot** (use the screenshot helper with name `home`). Expected: month summary + (after adding a tx via "+") a grouped list. The seeder provides categories/accounts but no transactions, so first add one via the "+" sheet to populate, then screenshot both the add sheet and the home list if practical.
- [ ] **Step 5: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Features/Home ios/Psy/App/RootView.swift
git commit -m "feat(ios): Home screen (summary + grouped transactions)"
```

---

## Task 3: Manage Accounts + Manage Categories + IconColorPicker

**Files:**
- Create: `ios/Psy/UI/Components/IconColorPicker.swift`
- Create: `ios/Psy/Features/Manage/ManageAccountsViewModel.swift`, `ManageAccountsView.swift`
- Create: `ios/Psy/Features/Manage/ManageCategoriesViewModel.swift`, `ManageCategoriesView.swift`

**Context:** Port `ManageAccountsScreen.kt`, `ManageCategoriesScreen.kt`, `IconColorPicker.kt` + their ViewModels. CRUD with an editor sheet. VMs follow the canonical pattern (subscribe to `accountRepo.observeAll()` / `categoryRepo.observeAll()`; category VM also splits by type or shows all with a type toggle in the editor). Account create/edit uses name + type + icon (emoji) + color (from the palette in IconColorPicker). Category adds type (income/expense) + sortOrder (append at end = current count). Use `accountRepo.upsert`, `categoryRepo.upsert/delete`.

- [ ] **Step 1:** Implement IconColorPicker (emoji picker + color swatches from a palette matching Android's `IconColorPicker.kt`).
- [ ] **Step 2:** Implement the two VMs (canonical pattern; expose `items`, editor draft state, `save()/delete()`), reading the Kotlin VMs for the exact fields.
- [ ] **Step 3:** Implement the two Views (list + add/edit sheet + swipe-to-delete), porting the Kotlin screens.
- [ ] **Step 4: Build + screenshot** (names `manage-accounts`, `manage-categories`). Re-enable the Home toolbar Menu links to these screens (from Task 2 Step 2).
- [ ] **Step 5: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Features/Manage ios/Psy/UI/Components/IconColorPicker.swift ios/Psy/Features/Home/HomeView.swift
git commit -m "feat(ios): Manage accounts + categories with icon/color picker"
```

---

## Task 4: Calendar screen + wire tab

**Files:**
- Create: `ios/Psy/Features/Calendar/CalendarViewModel.swift`, `CalendarView.swift`
- Modify: `ios/Psy/App/RootView.swift`

**Context:** Port `CalendarScreen.kt` + `CalendarViewModel`. Use `CalendarEngine`. Month-navigating VM (monthSubject). State: `monthLabel` (PsyMonth.label), `weeks: [[DayCell?]]`, `dayRows: [TxRow]`, `selectedDay: Date?`, `currency`. `prevMonth/nextMonth` reset `selectedDay = nil`. `selectDay(_:)` sets it.

- [ ] **Step 1: CalendarViewModel** — canonical pattern with month subject. Inner combine of `transactionRepo.observeBetween(monthStart,monthEnd)`, `categoryRepo.observeAll()`, `accountRepo.observeAll()`, plus a `selectedDaySubject`. Map → `CalendarEngine.build`.
- [ ] **Step 2: CalendarView** — `MonthSelector` header; a 7-column Monday-start grid (Mon..Sun headers "T2..CN"); each `DayCell` shows the day number, with small income(green)/expense(red) dots when nonzero, today highlighted with `psyColors.primary`; tapping a day selects it; below the grid a list of `dayRows` for the selected day (same row style as Home). Read `CalendarScreen.kt`.
- [ ] **Step 3: RootView** — replace Calendar stub with `CalendarView(container: container)`.
- [ ] **Step 4: Build + screenshot** (name `calendar`).
- [ ] **Step 5: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Features/Calendar ios/Psy/App/RootView.swift
git commit -m "feat(ios): Calendar screen (Monday grid + day transactions)"
```

---

## Task 5: Stats screen (Swift Charts) + wire tab

**Files:**
- Create: `ios/Psy/Features/Stats/StatsViewModel.swift`, `StatsView.swift`
- Create: `ios/Psy/UI/Components/DonutChart.swift`, `ios/Psy/UI/Components/TrendChart.swift`
- Modify: `ios/Psy/App/RootView.swift`

**Context:** Port `StatsScreen.kt` + charts (`DonutChart.kt`, `TrendBars.kt`) using **Swift Charts**. Use `StatsEngine`. Month-navigating VM. Also `pieMode: TxType` and `accountFilter: Int64?` subjects. The window query is `observeBetween(trendStart, monthEnd)` where `trendStart = month.adding(-5).startMillis`.

- [ ] **Step 1: StatsViewModel** — canonical pattern with three subjects (month, pieMode, accountFilter). State mirrors `StatsResult` fields + `currency` + `monthLabel` + `pieMode` + `selectedAccountId` + `accounts` (for the filter chips, from `accountRepo.observeAll()`). Methods: `prevMonth/nextMonth`, `setPieMode(_:)`, `selectAccount(_:)`.
- [ ] **Step 2: DonutChart** — Swift Charts `SectorMark(angle:.value(amount), innerRadius:.ratio(0.6))` over `[PieSlice]`, `.foregroundStyle(Color(argb: slice.color))`. Center label optional.
- [ ] **Step 3: TrendChart** — Swift Charts grouped `BarMark` over `[MonthBars]` (income green, expense pink) by month label.
- [ ] **Step 4: StatsView** — port `StatsScreen.kt`: `MonthSelector`; summary row (Thu/Chi/Còn lại/TB ngày using `summary` + `currency`); a Thu/Chi segmented toggle for `pieMode`; the donut + a legend/top-list (`top` with percent); the 6-month trend chart; the account-comparison section (`accountBreakdown` cards; tapping one calls `selectAccount`, tapping again / "Tất cả" clears). Read `StatsScreen.kt` for the exact arrangement.
- [ ] **Step 5: RootView** — replace Stats stub with `StatsView(container: container)`.
- [ ] **Step 6: Build + screenshot** (name `stats`).
- [ ] **Step 7: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Features/Stats ios/Psy/UI/Components/DonutChart.swift ios/Psy/UI/Components/TrendChart.swift ios/Psy/App/RootView.swift
git commit -m "feat(ios): Stats screen with Swift Charts donut + trend + account breakdown"
```

---

## Task 6: Budget screen + wire tab

**Files:**
- Create: `ios/Psy/UI/Components/BudgetProgress.swift`
- Create: `ios/Psy/Features/Budget/BudgetViewModel.swift`, `BudgetView.swift`
- Modify: `ios/Psy/App/RootView.swift`

**Context:** Port `BudgetScreen.kt` + `BudgetProgress.kt` + `BudgetViewModel`. Use `BudgetEngine`. Month-navigating VM. Editor sub-state (add total / add category / edit / remove) lives in the VM (mirror `BudgetViewModel`'s editor fields + `setBudget`/`removeBudget`). The editor's `canSave` and amount filtering use the same digit rules.

- [ ] **Step 1: BudgetProgress** — a rounded progress bar (port `BudgetProgress.kt`): fill = min(percent,1), over-budget (percent>1) tinted warning; shows spent / limit text.
- [ ] **Step 2: BudgetViewModel** — canonical pattern with month subject. State: `monthLabel`, `currency`, `total: TotalBudget?`, `categoryBudgets: [CategoryBudgetItem]`, `availableCategories: [Category]`, plus editor fields (`editorOpen`, `editorMode` {total,category}, `editorCategoryId`, `draftAmountText`, `isEditing`, `canSave`). Methods: `prevMonth/nextMonth`, `startAddTotal/startAddCategory/startEdit(_:)/closeEditor/save/remove`, `onAmountChange`. `save` calls `budgetRepo.setBudget(ledgerId,categoryId,amount)`; `remove` calls `budgetRepo.removeBudget`.
- [ ] **Step 3: BudgetView** — port `BudgetScreen.kt`: `MonthSelector`; total budget card with `BudgetProgress` (or "Đặt ngân sách tổng" button if none); per-category list with `BudgetProgress` rows (tap to edit); a "+" to add a category budget; an editor `.sheet` (mode-aware: amount field + category picker for category mode).
- [ ] **Step 4: RootView** — replace Budget stub with `BudgetView(container: container)`.
- [ ] **Step 5: Build + screenshot** (name `budget`).
- [ ] **Step 6: Commit**
```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Features/Budget ios/Psy/UI/Components/BudgetProgress.swift ios/Psy/App/RootView.swift
git commit -m "feat(ios): Budget screen (total + per-category with progress + editor)"
```

---

## Self-Review (completed during authoring)

**Spec coverage:** Home §4.1 (Task 2), AddEdit §4.5 (Task 1), Manage §4.6 (Task 3), Calendar §4.3 (Task 4), Stats §4.2 (Task 5), Budget §4.4 (Task 6). All consume PsyCore engines (Plan 4a) + repositories (Plan 2). Photo attach (§4.5) via PhotoStorage.

**Placeholder scan:** none — VMs are fully specified; Views reference the exact Android Compose file to port plus explicit requirements. The Home↔Manage link ordering note (Task 2/3) is a sequencing instruction, not a placeholder.

**Type consistency:** All VMs follow the canonical pattern; repo method names match Plan 2 (`observeAll`, `observeBetween`, `observeByType`, `upsert`, `delete`, `setBudget`, `removeBudget`) plus the new synchronous `all()`/`byType(_:)` helpers added in Task 1 Step 3a. Engine calls match Plan 4a signatures. View init `init(container:)` consistent; RootView passes `container` to every tab.

---

## Execution Handoff

After Plan 4b, the offline app is feature-complete. **Plan 5** adds Google auth, Keychain, backup/restore sync, app lock, Settings/Appearance, and the AppRoot login/lock gate.
