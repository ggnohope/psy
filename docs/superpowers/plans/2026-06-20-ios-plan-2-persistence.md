# iOS Port — Plan 2: Persistence & Repositories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the offline-first data layer: SwiftData entities, an autoincrement `IdAllocator`, a Combine `DataChangeBus`, the five repositories (mirroring Android repository interfaces), the snapshot serializer (byte-compatible with the Android backup blob), and the default-data seeder + composition root.

**Architecture:** `PsyCore` (Swift 6 package) gains the pure `Codable` snapshot DTOs (CLI round-trip tested via `swift test`). The app target (`Psy`, Swift 5 language mode) gains SwiftData `@Model` entities, `@MainActor` repositories that mirror Android's per-DAO Kotlin `Flow`s using Combine publishers fed by a `DataChangeBus`, a `SnapshotManager`, the seeder, and an `AppContainer` composition root (the Hilt-graph analog).

**Tech Stack:** SwiftData, Combine, Swift Package Manager (PsyCore), XcodeGen.

**Language mode decision:** The app target is set to `SWIFT_VERSION = 5.0`. SwiftData's `ModelContext` is non-`Sendable`; Swift 6 strict concurrency rejects capturing it in Combine `@Sendable` operator closures. Swift 5 mode is the standard, low-friction choice for app targets and is required for the Combine-mirrors-Flow approach in the spec. `PsyCore` remains Swift 6 (pure value types).

**Reference (source of truth):**
- `android/.../data/db/dao/*.kt` — query semantics (sort orders, half-open range, find-total/by-category)
- `android/.../data/repo/*Impl.kt` — repository logic (esp. `BudgetRepositoryImpl.setBudget`)
- `android/.../data/db/mapper/Mappers.kt` — entity↔domain mapping (enum `.name` ↔ `valueOf`)
- `android/.../data/backup/{SnapshotDto,SnapshotManager}.kt` — blob format + import/export order
- `android/.../data/seed/DefaultDataSeeder.kt` — default ledger/accounts/categories
- `docs/superpowers/specs/2026-06-20-ios-port-design.md` §3 (ID strategy), §4 (repos)

**Prerequisites:** Plan 1 merged/landed (PsyCore + app scaffold exist on branch `feature/ios-port`). Build verification uses `cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -target Psy -sdk iphonesimulator -configuration Debug build` (runtime-independent compile). PsyCore tasks verify with `cd ios/PsyCore && swift test`.

---

## File Structure

```
ios/PsyCore/Sources/PsyCore/
  SnapshotDTO.swift              # Codable DTOs, custom encode emits explicit nulls (Android-compatible)
ios/PsyCore/Tests/PsyCoreTests/
  SnapshotDTOTests.swift         # round-trip + explicit-null encoding tests (swift test)

ios/Psy/Data/Persistence/
  Entities.swift                 # @Model: LedgerEntity, AccountEntity, CategoryEntity, TransactionEntity, BudgetEntity
  ModelContainerFactory.swift    # makeModelContainer(inMemory:)
  Mappers.swift                  # Entity ↔ PsyCore domain struct
  IdAllocator.swift              # next id = max(existing)+1 per entity
  DataChangeBus.swift            # per-table CurrentValueSubject<Void,Never>
ios/Psy/Data/Repositories/
  LedgerRepository.swift
  AccountRepository.swift
  CategoryRepository.swift
  TransactionRepository.swift
  BudgetRepository.swift
ios/Psy/Data/Backup/
  SnapshotManager.swift          # export/import/wipe/isLocalEmpty
ios/Psy/Data/Seed/
  DefaultDataSeeder.swift
ios/Psy/App/
  AppContainer.swift             # composition root: container + bus + ids + repos + seeder + snapshotManager
ios/PsyTests/
  SnapshotManagerTests.swift     # in-memory SwiftData round-trip + IdAllocator (runs on simulator)
```

---

## Task 1: Snapshot DTOs + round-trip tests (PsyCore, TDD)

**Files:**
- Create: `ios/PsyCore/Tests/PsyCoreTests/SnapshotDTOTests.swift`
- Create: `ios/PsyCore/Sources/PsyCore/SnapshotDTO.swift`

**Context:** The backup blob is shared byte-for-byte with Android (kotlinx.serialization). Property names must match exactly, and **nullable fields must serialize as explicit `null`** (kotlinx encodes nulls; Swift's synthesized Codable omits nil). We therefore hand-write `encode(to:)` for the DTOs with optionals (`TransactionDTO`, `BudgetDTO`) using `encode` (not `encodeIfPresent`) so nil becomes JSON `null`.

- [ ] **Step 1: Write failing tests**

`ios/PsyCore/Tests/PsyCoreTests/SnapshotDTOTests.swift`:
```swift
import XCTest
@testable import PsyCore

final class SnapshotDTOTests: XCTestCase {

    /// A blob shaped exactly like Android's kotlinx output (explicit nulls, version field).
    private let androidBlob = """
    {"version":1,"ledgers":[{"id":1,"name":"Sổ của tôi","icon":"wallet","currency":"VND","createdAt":1000}],"accounts":[{"id":1,"name":"Tiền mặt","type":"CASH","icon":"💵","color":-14370978}],"categories":[{"id":1,"name":"Ăn uống","icon":"🍜","color":-7340074,"type":"EXPENSE","sortOrder":0}],"transactions":[{"id":1,"ledgerId":1,"type":"EXPENSE","amountMinor":50000,"categoryId":1,"accountId":1,"toAccountId":null,"note":"phở","date":1700000000000,"createdAt":1700000000000,"updatedAt":1700000000000,"photoUri":null}],"budgets":[{"id":1,"ledgerId":1,"categoryId":null,"amountMinor":1000000}]}
    """

    func testDecodeAndroidBlob() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidBlob.utf8))
        XCTAssertEqual(dto.version, 1)
        XCTAssertEqual(dto.transactions.first?.toAccountId, nil)
        XCTAssertEqual(dto.transactions.first?.categoryId, 1)
        XCTAssertEqual(dto.budgets.first?.categoryId, nil)
        XCTAssertEqual(dto.accounts.first?.type, "CASH")
        XCTAssertEqual(dto.accounts.first?.color, -14370978)
    }

    func testRoundTripPreservesData() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidBlob.utf8))
        let reEncoded = try JSONEncoder().encode(dto)
        let dto2 = try JSONDecoder().decode(SnapshotDTO.self, from: reEncoded)
        XCTAssertEqual(dto, dto2)
    }

    func testNilOptionalsEncodeAsExplicitNull() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidBlob.utf8))
        let json = String(decoding: try JSONEncoder().encode(dto), as: UTF8.self)
        XCTAssertTrue(json.contains("\"photoUri\":null"), "photoUri nil must serialize as explicit null")
        XCTAssertTrue(json.contains("\"toAccountId\":null"), "toAccountId nil must serialize as explicit null")
        XCTAssertTrue(json.contains("\"categoryId\":null"), "budget categoryId nil must serialize as explicit null")
    }
}
```

- [ ] **Step 2: Run tests, confirm fail**

Run: `cd ios/PsyCore && swift test`
Expected: FAIL — `cannot find 'SnapshotDTO' in scope`.

- [ ] **Step 3: Implement the DTOs**

`ios/PsyCore/Sources/PsyCore/SnapshotDTO.swift`:
```swift
import Foundation

/// Top-level backup snapshot. Field names + null handling must match the Android
/// kotlinx.serialization blob exactly so the shared backend blob round-trips both ways.
public struct SnapshotDTO: Codable, Equatable, Sendable {
    public var version: Int
    public var ledgers: [LedgerDTO]
    public var accounts: [AccountDTO]
    public var categories: [CategoryDTO]
    public var transactions: [TransactionDTO]
    public var budgets: [BudgetDTO]

    public init(version: Int = 1, ledgers: [LedgerDTO], accounts: [AccountDTO],
                categories: [CategoryDTO], transactions: [TransactionDTO], budgets: [BudgetDTO]) {
        self.version = version; self.ledgers = ledgers; self.accounts = accounts
        self.categories = categories; self.transactions = transactions; self.budgets = budgets
    }
}

public struct LedgerDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var currency: String
    public var createdAt: Int64
    public init(id: Int64, name: String, icon: String, currency: String, createdAt: Int64) {
        self.id = id; self.name = name; self.icon = icon; self.currency = currency; self.createdAt = createdAt
    }
}

public struct AccountDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var type: String
    public var icon: String
    public var color: Int64
    public init(id: Int64, name: String, type: String, icon: String, color: Int64) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color
    }
}

public struct CategoryDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var color: Int64
    public var type: String
    public var sortOrder: Int
    public init(id: Int64, name: String, icon: String, color: Int64, type: String, sortOrder: Int) {
        self.id = id; self.name = name; self.icon = icon; self.color = color; self.type = type; self.sortOrder = sortOrder
    }
}

/// Has optionals → custom encode emits explicit `null` to match kotlinx output.
public struct TransactionDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var type: String
    public var amountMinor: Int64
    public var categoryId: Int64?
    public var accountId: Int64
    public var toAccountId: Int64?
    public var note: String
    public var date: Int64
    public var createdAt: Int64
    public var updatedAt: Int64
    public var photoUri: String?

    public init(id: Int64, ledgerId: Int64, type: String, amountMinor: Int64, categoryId: Int64?,
                accountId: Int64, toAccountId: Int64?, note: String, date: Int64,
                createdAt: Int64, updatedAt: Int64, photoUri: String?) {
        self.id = id; self.ledgerId = ledgerId; self.type = type; self.amountMinor = amountMinor
        self.categoryId = categoryId; self.accountId = accountId; self.toAccountId = toAccountId
        self.note = note; self.date = date; self.createdAt = createdAt; self.updatedAt = updatedAt; self.photoUri = photoUri
    }

    enum CodingKeys: String, CodingKey {
        case id, ledgerId, type, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(ledgerId, forKey: .ledgerId)
        try c.encode(type, forKey: .type)
        try c.encode(amountMinor, forKey: .amountMinor)
        try c.encode(categoryId, forKey: .categoryId)        // explicit null when nil
        try c.encode(accountId, forKey: .accountId)
        try c.encode(toAccountId, forKey: .toAccountId)      // explicit null when nil
        try c.encode(note, forKey: .note)
        try c.encode(date, forKey: .date)
        try c.encode(createdAt, forKey: .createdAt)
        try c.encode(updatedAt, forKey: .updatedAt)
        try c.encode(photoUri, forKey: .photoUri)            // explicit null when nil
    }
}

/// Has optional categoryId → custom encode emits explicit `null`.
public struct BudgetDTO: Codable, Equatable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var categoryId: Int64?
    public var amountMinor: Int64
    public init(id: Int64, ledgerId: Int64, categoryId: Int64?, amountMinor: Int64) {
        self.id = id; self.ledgerId = ledgerId; self.categoryId = categoryId; self.amountMinor = amountMinor
    }

    enum CodingKeys: String, CodingKey { case id, ledgerId, categoryId, amountMinor }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(ledgerId, forKey: .ledgerId)
        try c.encode(categoryId, forKey: .categoryId)        // explicit null when nil
        try c.encode(amountMinor, forKey: .amountMinor)
    }
}
```

- [ ] **Step 4: Run tests, confirm pass**

Run: `cd ios/PsyCore && swift test`
Expected: PASS — all SnapshotDTO + Money tests green.

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/SnapshotDTO.swift ios/PsyCore/Tests/PsyCoreTests/SnapshotDTOTests.swift
git commit -m "feat(ios): snapshot DTOs with Android-compatible null encoding + tests"
```

---

## Task 2: SwiftData entities + model container

**Files:**
- Create: `ios/Psy/Data/Persistence/Entities.swift`
- Create: `ios/Psy/Data/Persistence/ModelContainerFactory.swift`
- Modify: `ios/project.yml` (set app target `SWIFT_VERSION: "5.0"`)

- [ ] **Step 1: Set the app target to Swift 5 language mode**

In `ios/project.yml`, under `settings.base`, change `SWIFT_VERSION: "6.0"` to:
```yaml
    SWIFT_VERSION: "5.0"
```
(Rationale in the plan header. PsyCore's own Package.swift stays swift-tools 6.0.)

- [ ] **Step 2: Define the @Model entities (tables mirror Room: ledgers/accounts/categories/transactions/budgets)**

`ios/Psy/Data/Persistence/Entities.swift`:
```swift
import Foundation
import SwiftData

@Model
final class LedgerEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var icon: String
    var currency: String
    var createdAt: Int64
    init(id: Int64, name: String, icon: String, currency: String, createdAt: Int64) {
        self.id = id; self.name = name; self.icon = icon; self.currency = currency; self.createdAt = createdAt
    }
}

@Model
final class AccountEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var type: String
    var icon: String
    var color: Int64
    init(id: Int64, name: String, type: String, icon: String, color: Int64) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color
    }
}

@Model
final class CategoryEntity {
    @Attribute(.unique) var id: Int64
    var name: String
    var icon: String
    var color: Int64
    var type: String
    var sortOrder: Int
    init(id: Int64, name: String, icon: String, color: Int64, type: String, sortOrder: Int) {
        self.id = id; self.name = name; self.icon = icon; self.color = color; self.type = type; self.sortOrder = sortOrder
    }
}

@Model
final class TransactionEntity {
    @Attribute(.unique) var id: Int64
    var ledgerId: Int64
    var type: String
    var amountMinor: Int64
    var categoryId: Int64?
    var accountId: Int64
    var toAccountId: Int64?
    var note: String
    var date: Int64
    var createdAt: Int64
    var updatedAt: Int64
    var photoUri: String?
    init(id: Int64, ledgerId: Int64, type: String, amountMinor: Int64, categoryId: Int64?,
         accountId: Int64, toAccountId: Int64?, note: String, date: Int64,
         createdAt: Int64, updatedAt: Int64, photoUri: String?) {
        self.id = id; self.ledgerId = ledgerId; self.type = type; self.amountMinor = amountMinor
        self.categoryId = categoryId; self.accountId = accountId; self.toAccountId = toAccountId
        self.note = note; self.date = date; self.createdAt = createdAt; self.updatedAt = updatedAt; self.photoUri = photoUri
    }
}

@Model
final class BudgetEntity {
    @Attribute(.unique) var id: Int64
    var ledgerId: Int64
    var categoryId: Int64?
    var amountMinor: Int64
    init(id: Int64, ledgerId: Int64, categoryId: Int64?, amountMinor: Int64) {
        self.id = id; self.ledgerId = ledgerId; self.categoryId = categoryId; self.amountMinor = amountMinor
    }
}
```

- [ ] **Step 3: Container factory**

`ios/Psy/Data/Persistence/ModelContainerFactory.swift`:
```swift
import Foundation
import SwiftData

enum ModelContainerFactory {
    static let schema = Schema([
        LedgerEntity.self, AccountEntity.self, CategoryEntity.self,
        TransactionEntity.self, BudgetEntity.self,
    ])

    static func make(inMemory: Bool = false) -> ModelContainer {
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: inMemory)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
    }
}
```

- [ ] **Step 4: Verify compile**

Run: `cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -target Psy -sdk iphonesimulator -configuration Debug build`
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/project.yml ios/Psy/Data/Persistence/Entities.swift ios/Psy/Data/Persistence/ModelContainerFactory.swift
git commit -m "feat(ios): SwiftData entities + model container (Swift 5 app mode)"
```

---

## Task 3: Id allocator, change bus, mappers

**Files:**
- Create: `ios/Psy/Data/Persistence/IdAllocator.swift`
- Create: `ios/Psy/Data/Persistence/DataChangeBus.swift`
- Create: `ios/Psy/Data/Persistence/Mappers.swift`

- [ ] **Step 1: IdAllocator (autoincrement: max(existing)+1, mirrors Room autoGenerate)**

`ios/Psy/Data/Persistence/IdAllocator.swift`:
```swift
import Foundation
import SwiftData

/// Allocates monotonically increasing Int64 ids per entity table, mirroring Room's
/// autoincrement. Single-user UI app → all access is on the main actor, no races.
@MainActor
final class IdAllocator {
    private let context: ModelContext
    init(context: ModelContext) { self.context = context }

    func nextId<T: PersistentModel>(_ type: T.Type, idKeyPath: KeyPath<T, Int64>,
                                    sortDescending: SortDescriptor<T>) -> Int64 {
        var d = FetchDescriptor<T>(sortBy: [sortDescending])
        d.fetchLimit = 1
        let maxId = (try? context.fetch(d))?.first?[keyPath: idKeyPath] ?? 0
        return maxId + 1
    }
}
```

- [ ] **Step 2: DataChangeBus (per-table publisher; CurrentValueSubject seeded so subscribers fetch immediately — mirrors Room Flow's initial emit)**

`ios/Psy/Data/Persistence/DataChangeBus.swift`:
```swift
import Foundation
import Combine

enum PsyTable {
    case ledgers, accounts, categories, transactions, budgets
}

/// Mirrors Room's per-DAO Flow emissions: each table has a subject that fires on every write.
/// Seeded with an initial value so a new subscriber immediately performs its first fetch.
@MainActor
final class DataChangeBus {
    private var subjects: [PsyTable: CurrentValueSubject<Void, Never>] = [:]

    func subject(_ table: PsyTable) -> CurrentValueSubject<Void, Never> {
        if let existing = subjects[table] { return existing }
        let created = CurrentValueSubject<Void, Never>(())
        subjects[table] = created
        return created
    }

    func notify(_ table: PsyTable) {
        subject(table).send(())
    }
}
```

- [ ] **Step 3: Entity ↔ domain mappers (mirror Mappers.kt; enum `.name` ↔ rawValue)**

`ios/Psy/Data/Persistence/Mappers.swift`:
```swift
import Foundation
import PsyCore

extension LedgerEntity {
    func toDomain() -> Ledger { Ledger(id: id, name: name, icon: icon, currency: currency, createdAt: createdAt) }
    func apply(_ d: Ledger) { name = d.name; icon = d.icon; currency = d.currency; createdAt = d.createdAt }
    convenience init(from d: Ledger, id: Int64) {
        self.init(id: id, name: d.name, icon: d.icon, currency: d.currency, createdAt: d.createdAt)
    }
}

extension AccountEntity {
    func toDomain() -> Account {
        Account(id: id, name: name, type: AccountType(rawValue: type) ?? .cash, icon: icon, color: color)
    }
    func apply(_ d: Account) { name = d.name; type = d.type.rawValue; icon = d.icon; color = d.color }
    convenience init(from d: Account, id: Int64) {
        self.init(id: id, name: d.name, type: d.type.rawValue, icon: d.icon, color: d.color)
    }
}

extension CategoryEntity {
    func toDomain() -> Category {
        Category(id: id, name: name, icon: icon, color: color,
                 type: CategoryType(rawValue: type) ?? .expense, sortOrder: sortOrder)
    }
    func apply(_ d: Category) { name = d.name; icon = d.icon; color = d.color; type = d.type.rawValue; sortOrder = d.sortOrder }
    convenience init(from d: Category, id: Int64) {
        self.init(id: id, name: d.name, icon: d.icon, color: d.color, type: d.type.rawValue, sortOrder: d.sortOrder)
    }
}

extension TransactionEntity {
    func toDomain() -> Transaction {
        Transaction(id: id, ledgerId: ledgerId, type: TxType(rawValue: type) ?? .expense,
                    amountMinor: amountMinor, categoryId: categoryId, accountId: accountId,
                    toAccountId: toAccountId, note: note, date: date, createdAt: createdAt,
                    updatedAt: updatedAt, photoUri: photoUri)
    }
    func apply(_ d: Transaction) {
        ledgerId = d.ledgerId; type = d.type.rawValue; amountMinor = d.amountMinor
        categoryId = d.categoryId; accountId = d.accountId; toAccountId = d.toAccountId
        note = d.note; date = d.date; createdAt = d.createdAt; updatedAt = d.updatedAt; photoUri = d.photoUri
    }
    convenience init(from d: Transaction, id: Int64) {
        self.init(id: id, ledgerId: d.ledgerId, type: d.type.rawValue, amountMinor: d.amountMinor,
                  categoryId: d.categoryId, accountId: d.accountId, toAccountId: d.toAccountId,
                  note: d.note, date: d.date, createdAt: d.createdAt, updatedAt: d.updatedAt, photoUri: d.photoUri)
    }
}

extension BudgetEntity {
    func toDomain() -> Budget { Budget(id: id, ledgerId: ledgerId, categoryId: categoryId, amountMinor: amountMinor) }
    func apply(_ d: Budget) { ledgerId = d.ledgerId; categoryId = d.categoryId; amountMinor = d.amountMinor }
    convenience init(from d: Budget, id: Int64) {
        self.init(id: id, ledgerId: d.ledgerId, categoryId: d.categoryId, amountMinor: d.amountMinor)
    }
}
```

- [ ] **Step 4: Verify compile** (same command as Task 2 Step 4). Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Data/Persistence/IdAllocator.swift ios/Psy/Data/Persistence/DataChangeBus.swift ios/Psy/Data/Persistence/Mappers.swift
git commit -m "feat(ios): IdAllocator, DataChangeBus, entity-domain mappers"
```

---

## Task 4: Repositories

**Files:**
- Create: `ios/Psy/Data/Repositories/LedgerRepository.swift`
- Create: `ios/Psy/Data/Repositories/AccountRepository.swift`
- Create: `ios/Psy/Data/Repositories/CategoryRepository.swift`
- Create: `ios/Psy/Data/Repositories/TransactionRepository.swift`
- Create: `ios/Psy/Data/Repositories/BudgetRepository.swift`

**Context:** Each repository mirrors the same-named Android interface. `observe*` returns `AnyPublisher<…, Never>` driven by `DataChangeBus` — equivalent to Room's `Flow`. Writes go through SwiftData then `bus.notify(table)`. `upsert` honors an explicit non-zero id (used by snapshot import); id 0 means "allocate new". All are `@MainActor` (SwiftData main-context access). A private `fetchOne(id:)` helper finds an existing row for in-place update.

- [ ] **Step 1: LedgerRepository (sort by createdAt ASC; mirrors LedgerDao)**

`ios/Psy/Data/Repositories/LedgerRepository.swift`:
```swift
import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class LedgerRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[Ledger], Never> {
        bus.subject(.ledgers).map { [context] _ in
            let d = FetchDescriptor<LedgerEntity>(sortBy: [SortDescriptor(\.createdAt, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func firstOrNull() -> Ledger? {
        var d = FetchDescriptor<LedgerEntity>(sortBy: [SortDescriptor(\.createdAt, order: .forward)])
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first?.toDomain()
    }

    @discardableResult
    func upsert(_ ledger: Ledger) -> Int64 {
        let id = ledger.id != 0 ? ledger.id
            : ids.nextId(LedgerEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(ledger) }
        else { context.insert(LedgerEntity(from: ledger, id: id)) }
        try? context.save()
        bus.notify(.ledgers)
        return id
    }

    private func fetchOne(_ id: Int64) -> LedgerEntity? {
        var d = FetchDescriptor<LedgerEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
```

- [ ] **Step 2: AccountRepository (sort by id ASC; mirrors AccountDao)**

`ios/Psy/Data/Repositories/AccountRepository.swift`:
```swift
import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class AccountRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[Account], Never> {
        bus.subject(.accounts).map { [context] _ in
            let d = FetchDescriptor<AccountEntity>(sortBy: [SortDescriptor(\.id, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func count() -> Int { (try? context.fetchCount(FetchDescriptor<AccountEntity>())) ?? 0 }

    @discardableResult
    func upsert(_ account: Account) -> Int64 {
        let id = account.id != 0 ? account.id
            : ids.nextId(AccountEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(account) }
        else { context.insert(AccountEntity(from: account, id: id)) }
        try? context.save()
        bus.notify(.accounts)
        return id
    }

    private func fetchOne(_ id: Int64) -> AccountEntity? {
        var d = FetchDescriptor<AccountEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
```

- [ ] **Step 3: CategoryRepository (sort by sortOrder ASC; observeByType; mirrors CategoryDao)**

`ios/Psy/Data/Repositories/CategoryRepository.swift`:
```swift
import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class CategoryRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll() -> AnyPublisher<[Category], Never> {
        bus.subject(.categories).map { [context] _ in
            let d = FetchDescriptor<CategoryEntity>(sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func observeByType(_ type: CategoryType) -> AnyPublisher<[Category], Never> {
        let raw = type.rawValue
        return bus.subject(.categories).map { [context] _ in
            let d = FetchDescriptor<CategoryEntity>(
                predicate: #Predicate { $0.type == raw },
                sortBy: [SortDescriptor(\.sortOrder, order: .forward)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func count() -> Int { (try? context.fetchCount(FetchDescriptor<CategoryEntity>())) ?? 0 }

    @discardableResult
    func upsert(_ category: Category) -> Int64 {
        let id = category.id != 0 ? category.id
            : ids.nextId(CategoryEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(category) }
        else { context.insert(CategoryEntity(from: category, id: id)) }
        try? context.save()
        bus.notify(.categories)
        return id
    }

    func delete(_ category: Category) {
        if let existing = fetchOne(category.id) { context.delete(existing); try? context.save(); bus.notify(.categories) }
    }

    private func fetchOne(_ id: Int64) -> CategoryEntity? {
        var d = FetchDescriptor<CategoryEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
```

- [ ] **Step 4: TransactionRepository (half-open range, sort date DESC then id DESC; mirrors TransactionDao)**

`ios/Psy/Data/Repositories/TransactionRepository.swift`:
```swift
import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class TransactionRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    /// Half-open range [start, end): callers pass end = start of the next period.
    func observeBetween(ledgerId: Int64, start: Int64, end: Int64) -> AnyPublisher<[Transaction], Never> {
        bus.subject(.transactions).map { [context] _ in
            let d = FetchDescriptor<TransactionEntity>(
                predicate: #Predicate { $0.ledgerId == ledgerId && $0.date >= start && $0.date < end },
                sortBy: [SortDescriptor(\.date, order: .reverse), SortDescriptor(\.id, order: .reverse)])
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    func getById(_ id: Int64) -> Transaction? { fetchOne(id)?.toDomain() }

    @discardableResult
    func upsert(_ tx: Transaction) -> Int64 {
        let id = tx.id != 0 ? tx.id
            : ids.nextId(TransactionEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing = fetchOne(id) { existing.apply(tx) }
        else { context.insert(TransactionEntity(from: tx, id: id)) }
        try? context.save()
        bus.notify(.transactions)
        return id
    }

    func delete(_ tx: Transaction) {
        if let existing = fetchOne(tx.id) { context.delete(existing); try? context.save(); bus.notify(.transactions) }
    }

    private func fetchOne(_ id: Int64) -> TransactionEntity? {
        var d = FetchDescriptor<TransactionEntity>(predicate: #Predicate { $0.id == id })
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
```

- [ ] **Step 5: BudgetRepository (find total/by-category then upsert with existing id; mirrors BudgetRepositoryImpl.setBudget)**

`ios/Psy/Data/Repositories/BudgetRepository.swift`:
```swift
import Foundation
import Combine
import SwiftData
import PsyCore

@MainActor
final class BudgetRepository {
    private let context: ModelContext
    private let bus: DataChangeBus
    private let ids: IdAllocator
    init(context: ModelContext, bus: DataChangeBus, ids: IdAllocator) {
        self.context = context; self.bus = bus; self.ids = ids
    }

    func observeAll(ledgerId: Int64) -> AnyPublisher<[Budget], Never> {
        bus.subject(.budgets).map { [context] _ in
            let d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.ledgerId == ledgerId })
            return ((try? context.fetch(d)) ?? []).map { $0.toDomain() }
        }.eraseToAnyPublisher()
    }

    /// Upserts the single total (categoryId == nil) or per-category budget, reusing the
    /// existing row's id so there is at most one budget per (ledger, category|total).
    func setBudget(ledgerId: Int64, categoryId: Int64?, amountMinor: Int64) {
        let existing = findExisting(ledgerId: ledgerId, categoryId: categoryId)
        let id = existing?.id ?? ids.nextId(BudgetEntity.self, idKeyPath: \.id, sortDescending: SortDescriptor(\.id, order: .reverse))
        if let existing { existing.amountMinor = amountMinor }
        else { context.insert(BudgetEntity(id: id, ledgerId: ledgerId, categoryId: categoryId, amountMinor: amountMinor)) }
        try? context.save()
        bus.notify(.budgets)
    }

    func removeBudget(_ budget: Budget) {
        var d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.id == budget.id })
        d.fetchLimit = 1
        if let existing = (try? context.fetch(d))?.first {
            context.delete(existing); try? context.save(); bus.notify(.budgets)
        }
    }

    private func findExisting(ledgerId: Int64, categoryId: Int64?) -> BudgetEntity? {
        var d: FetchDescriptor<BudgetEntity>
        if let categoryId {
            d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.ledgerId == ledgerId && $0.categoryId == categoryId })
        } else {
            d = FetchDescriptor<BudgetEntity>(predicate: #Predicate { $0.ledgerId == ledgerId && $0.categoryId == nil })
        }
        d.fetchLimit = 1
        return (try? context.fetch(d))?.first
    }
}
```

- [ ] **Step 6: Verify compile** (same command as Task 2 Step 4). Expected: `** BUILD SUCCEEDED **`.

> If a `#Predicate` with `Int64` comparison fails to compile, report it — SwiftData predicates sometimes need `Int` not `Int64`; the fallback is to store ids as `Int` in entities. Do NOT silently switch; report as DONE_WITH_CONCERNS so the controller can decide.

- [ ] **Step 7: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Data/Repositories
git commit -m "feat(ios): five SwiftData repositories with Combine publishers"
```

---

## Task 5: SnapshotManager

**Files:**
- Create: `ios/Psy/Data/Backup/SnapshotManager.swift`

**Context:** Mirrors `SnapshotManager.kt`. `export` reads every table → `SnapshotDTO` → JSON string. `import` decodes and replaces all content (delete children→parents, insert parents→children). `wipeLocal` clears all. `isLocalEmpty` = no ledgers. After import/wipe it must `bus.notify` every table so observers refresh. Uses raw fetch/insert/delete on the context directly (not the repositories) to do a full bulk replace.

- [ ] **Step 1: Implement SnapshotManager**

`ios/Psy/Data/Backup/SnapshotManager.swift`:
```swift
import Foundation
import SwiftData
import PsyCore

@MainActor
final class SnapshotManager {
    private let context: ModelContext
    private let bus: DataChangeBus
    init(context: ModelContext, bus: DataChangeBus) {
        self.context = context; self.bus = bus
    }

    func isLocalEmpty() -> Bool {
        ((try? context.fetchCount(FetchDescriptor<LedgerEntity>())) ?? 0) == 0
    }

    func export() throws -> String {
        let snapshot = SnapshotDTO(
            ledgers: fetchAll(LedgerEntity.self).map { LedgerDTO(id: $0.id, name: $0.name, icon: $0.icon, currency: $0.currency, createdAt: $0.createdAt) },
            accounts: fetchAll(AccountEntity.self).map { AccountDTO(id: $0.id, name: $0.name, type: $0.type, icon: $0.icon, color: $0.color) },
            categories: fetchAll(CategoryEntity.self).map { CategoryDTO(id: $0.id, name: $0.name, icon: $0.icon, color: $0.color, type: $0.type, sortOrder: $0.sortOrder) },
            transactions: fetchAll(TransactionEntity.self).map { TransactionDTO(id: $0.id, ledgerId: $0.ledgerId, type: $0.type, amountMinor: $0.amountMinor, categoryId: $0.categoryId, accountId: $0.accountId, toAccountId: $0.toAccountId, note: $0.note, date: $0.date, createdAt: $0.createdAt, updatedAt: $0.updatedAt, photoUri: $0.photoUri) },
            budgets: fetchAll(BudgetEntity.self).map { BudgetDTO(id: $0.id, ledgerId: $0.ledgerId, categoryId: $0.categoryId, amountMinor: $0.amountMinor) }
        )
        let data = try JSONEncoder().encode(snapshot)
        return String(decoding: data, as: UTF8.self)
    }

    func importBlob(_ jsonStr: String) throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(jsonStr.utf8))
        deleteAllRows()
        for l in dto.ledgers { context.insert(LedgerEntity(id: l.id, name: l.name, icon: l.icon, currency: l.currency, createdAt: l.createdAt)) }
        for a in dto.accounts { context.insert(AccountEntity(id: a.id, name: a.name, type: a.type, icon: a.icon, color: a.color)) }
        for c in dto.categories { context.insert(CategoryEntity(id: c.id, name: c.name, icon: c.icon, color: c.color, type: c.type, sortOrder: c.sortOrder)) }
        for t in dto.transactions { context.insert(TransactionEntity(id: t.id, ledgerId: t.ledgerId, type: t.type, amountMinor: t.amountMinor, categoryId: t.categoryId, accountId: t.accountId, toAccountId: t.toAccountId, note: t.note, date: t.date, createdAt: t.createdAt, updatedAt: t.updatedAt, photoUri: t.photoUri)) }
        for b in dto.budgets { context.insert(BudgetEntity(id: b.id, ledgerId: b.ledgerId, categoryId: b.categoryId, amountMinor: b.amountMinor)) }
        try context.save()
        notifyAll()
    }

    func wipeLocal() {
        deleteAllRows()
        try? context.save()
        notifyAll()
    }

    // MARK: - helpers
    private func fetchAll<T: PersistentModel>(_ type: T.Type) -> [T] {
        (try? context.fetch(FetchDescriptor<T>())) ?? []
    }

    private func deleteAllRows() {
        // Children before parents (consistent with FK semantics, though SwiftData has no FKs here).
        try? context.delete(model: TransactionEntity.self)
        try? context.delete(model: BudgetEntity.self)
        try? context.delete(model: CategoryEntity.self)
        try? context.delete(model: AccountEntity.self)
        try? context.delete(model: LedgerEntity.self)
    }

    private func notifyAll() {
        bus.notify(.ledgers); bus.notify(.accounts); bus.notify(.categories)
        bus.notify(.transactions); bus.notify(.budgets)
    }
}
```

- [ ] **Step 2: Verify compile** (same command as Task 2 Step 4). Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 3: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Data/Backup/SnapshotManager.swift
git commit -m "feat(ios): SnapshotManager export/import/wipe (shared blob format)"
```

---

## Task 6: Default data seeder + AppContainer composition root

**Files:**
- Create: `ios/Psy/Data/Seed/DefaultDataSeeder.swift`
- Create: `ios/Psy/App/AppContainer.swift`

**Context:** `DefaultDataSeeder` mirrors `DefaultDataSeeder.kt` exactly (same names, icons, palette, sort orders). `AppContainer` is the Hilt-graph analog: it owns the `ModelContainer`, exposes the main `ModelContext`, the bus, the id allocator, all repositories, the seeder, and the snapshot manager. It is the single object injected into the SwiftUI environment (used by Plan 4/5).

- [ ] **Step 1: DefaultDataSeeder (port of DefaultDataSeeder.kt — exact strings/icons/colors)**

`ios/Psy/Data/Seed/DefaultDataSeeder.swift`:
```swift
import Foundation
import PsyCore

@MainActor
final class DefaultDataSeeder {
    private let ledgerRepo: LedgerRepository
    private let accountRepo: AccountRepository
    private let categoryRepo: CategoryRepository
    init(ledgerRepo: LedgerRepository, accountRepo: AccountRepository, categoryRepo: CategoryRepository) {
        self.ledgerRepo = ledgerRepo; self.accountRepo = accountRepo; self.categoryRepo = categoryRepo
    }

    func seedIfEmpty(now: Int64) {
        if ledgerRepo.firstOrNull() == nil {
            ledgerRepo.upsert(Ledger(name: "Sổ của tôi", icon: "wallet", currency: "VND", createdAt: now))
        }
        if accountRepo.count() == 0 {
            accountRepo.upsert(Account(name: "Tiền mặt", type: .cash, icon: "💵", color: 0xFF22C55E))
            accountRepo.upsert(Account(name: "Ngân hàng", type: .bank, icon: "🏦", color: 0xFF7FD8FF))
        }
        if categoryRepo.count() == 0 {
            let palette: [Int64] = [
                0xFFFF8FD6, 0xFFA18CFF, 0xFF7FD8FF, 0xFFFFB86B, 0xFF6BCB77,
                0xFFFF6B6B, 0xFFB088F9, 0xFF4D96FF, 0xFFFF5FA2, 0xFF22C55E,
            ]
            let expense: [(String, String)] = [
                ("Ăn uống", "🍜"), ("Di chuyển", "🚌"), ("Mua sắm", "🛍️"), ("Hoá đơn", "🧾"),
                ("Giải trí", "🎮"), ("Sức khoẻ", "💊"), ("Khác", "📦"),
            ]
            let income: [(String, String)] = [("Lương", "💰"), ("Thưởng", "🎁"), ("Khác", "📦")]
            for (i, item) in expense.enumerated() {
                categoryRepo.upsert(Category(name: item.0, icon: item.1, color: palette[i % palette.count], type: .expense, sortOrder: i))
            }
            for (i, item) in income.enumerated() {
                categoryRepo.upsert(Category(name: item.0, icon: item.1, color: palette[i % palette.count], type: .income, sortOrder: i))
            }
        }
    }
}
```

> Note: `0xFF22C55E` etc. are positive `Int64` literals here (top byte 0xFF, fits in Int64). The Android side stores the same ARGB value; `Color(argb:)` masks per byte so sign does not matter for rendering. The snapshot blob will carry these as positive numbers from iOS and negative from Android for the same color — both decode to identical bytes via `& 0xFF`. Acceptable (color round-trips correctly).

- [ ] **Step 2: AppContainer (composition root)**

`ios/Psy/App/AppContainer.swift`:
```swift
import Foundation
import SwiftData

/// Composition root (Hilt-graph analog). Owns the SwiftData container and wires every
/// repository/manager against the shared main ModelContext + DataChangeBus.
@MainActor
final class AppContainer {
    let modelContainer: ModelContainer
    let context: ModelContext
    let bus: DataChangeBus
    let ids: IdAllocator

    let ledgerRepo: LedgerRepository
    let accountRepo: AccountRepository
    let categoryRepo: CategoryRepository
    let transactionRepo: TransactionRepository
    let budgetRepo: BudgetRepository

    let snapshotManager: SnapshotManager
    let seeder: DefaultDataSeeder

    init(inMemory: Bool = false) {
        modelContainer = ModelContainerFactory.make(inMemory: inMemory)
        context = modelContainer.mainContext
        bus = DataChangeBus()
        ids = IdAllocator(context: context)

        ledgerRepo = LedgerRepository(context: context, bus: bus, ids: ids)
        accountRepo = AccountRepository(context: context, bus: bus, ids: ids)
        categoryRepo = CategoryRepository(context: context, bus: bus, ids: ids)
        transactionRepo = TransactionRepository(context: context, bus: bus, ids: ids)
        budgetRepo = BudgetRepository(context: context, bus: bus, ids: ids)

        snapshotManager = SnapshotManager(context: context, bus: bus)
        seeder = DefaultDataSeeder(ledgerRepo: ledgerRepo, accountRepo: accountRepo, categoryRepo: categoryRepo)
    }
}
```

- [ ] **Step 3: Verify compile** (same command as Task 2 Step 4). Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Data/Seed/DefaultDataSeeder.swift ios/Psy/App/AppContainer.swift
git commit -m "feat(ios): default data seeder + AppContainer composition root"
```

---

## Task 7: SnapshotManager round-trip + IdAllocator integration test (runs on simulator)

**Files:**
- Create: `ios/PsyTests/SnapshotManagerTests.swift`

**Context:** This unit test uses an in-memory `AppContainer(inMemory: true)` to verify the full SwiftData path end-to-end. It REQUIRES a booted/available iOS Simulator runtime. If the runtime is not yet installed, write the test, confirm it compiles, and report the run as deferred.

- [ ] **Step 1: Write the integration test**

`ios/PsyTests/SnapshotManagerTests.swift`:
```swift
import XCTest
import PsyCore
@testable import Psy

@MainActor
final class SnapshotManagerTests: XCTestCase {

    func testSeedThenExportImportRoundTrip() throws {
        let c = AppContainer(inMemory: true)
        c.seeder.seedIfEmpty(now: 1000)

        // Seeded shape: 1 ledger, 2 accounts, 10 categories (7 expense + 3 income).
        XCTAssertEqual(c.accountRepo.count(), 2)
        XCTAssertEqual(c.categoryRepo.count(), 10)
        XCTAssertFalse(c.snapshotManager.isLocalEmpty())

        let blob = try c.snapshotManager.export()

        // Import into a fresh container reproduces identical counts.
        let c2 = AppContainer(inMemory: true)
        XCTAssertTrue(c2.snapshotManager.isLocalEmpty())
        try c2.snapshotManager.importBlob(blob)
        XCTAssertEqual(c2.accountRepo.count(), 2)
        XCTAssertEqual(c2.categoryRepo.count(), 10)
        XCTAssertNotNil(c2.ledgerRepo.firstOrNull())
    }

    func testIdAllocatorIncrements() throws {
        let c = AppContainer(inMemory: true)
        let id1 = c.accountRepo.upsert(Account(name: "A", type: .cash, icon: "💵", color: 0xFF22C55E))
        let id2 = c.accountRepo.upsert(Account(name: "B", type: .bank, icon: "🏦", color: 0xFF7FD8FF))
        XCTAssertEqual(id1, 1)
        XCTAssertEqual(id2, 2)
    }

    func testWipeLocalClearsEverything() throws {
        let c = AppContainer(inMemory: true)
        c.seeder.seedIfEmpty(now: 1000)
        c.snapshotManager.wipeLocal()
        XCTAssertTrue(c.snapshotManager.isLocalEmpty())
        XCTAssertEqual(c.accountRepo.count(), 0)
        XCTAssertEqual(c.categoryRepo.count(), 0)
    }
}
```

- [ ] **Step 2: Run the test (if a simulator runtime is available)**

Run:
```bash
cd ios && xcodegen generate
xcodebuild -project Psy.xcodeproj -scheme Psy \
  -destination 'platform=iOS Simulator,name=iPhone 16' test
```
Expected: `** TEST SUCCEEDED **`, SnapshotManagerTests 3/3 pass.

> If no runtime: run the compile-only check `xcodebuild -project Psy.xcodeproj -target PsyTests -sdk iphonesimulator build` and report the test run as deferred until the runtime is installed.

- [ ] **Step 3: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyTests/SnapshotManagerTests.swift
git commit -m "test(ios): SnapshotManager round-trip + IdAllocator integration tests"
```

---

## Self-Review (completed during authoring)

**Spec coverage (Plan 2 portion):**
- §2 Data/Persistence, Data/Repositories, Data/Backup, Data/Seed, App/AppContainer → Tasks 2–6.
- §3 ID strategy (Int64 explicit, max+1), enum rawValue mapping, ARGB color, photoUri optional → Tasks 2,3, seeder note.
- §3 snapshot byte-compatibility (field names, explicit nulls) → Task 1 (CLI-tested).
- §4 repository behaviors (sort orders, half-open range, setBudget upsert, spent/transfer rules are VM-side later) → Task 4.
- §9 risks: snapshot compatibility (Task 1 tests), ID allocation (Task 3 + Task 7 test), Combine-vs-@Query (repos), Swift-mode for SwiftData/Combine (header decision).

**Deferred to later plans:** AuthRepository/BackupRepository/SettingsRepository (Plan 5 — they need Keychain/network); all ViewModels + Views (Plan 4/5); wiring AppContainer into PsyApp's @main and SwiftUI environment (Plan 4 Task 1, when the first real screen replaces the placeholder).

**Placeholder scan:** none — all steps have concrete code/commands. Two explicit "report, don't guess" guards (Task 4 Int64 predicate; Task 7 runtime) are intentional escalation points, not placeholders.

**Type consistency:** Repository method names mirror the Android interfaces (`observeAll`, `observeByType`, `observeBetween`, `firstOrNull`, `count`, `upsert`, `delete`, `setBudget`, `removeBudget`). Entity ↔ domain via `toDomain()`/`apply(_:)`/`init(from:id:)` used consistently across Mappers, repos, SnapshotManager. `DataChangeBus.notify(_:)`/`subject(_:)`, `IdAllocator.nextId(_:idKeyPath:sortDescending:)`, `AppContainer` property names match across Tasks 3–7. DTO names (`SnapshotDTO`, `LedgerDTO`…) consistent between Task 1 and Task 5.

---

## Execution Handoff

Plan 2 complete. After executing, the next plan is **Plan 3 (Backend multi-audience)** — small and independent — or **Plan 4 (Offline features)** which depends on Plans 1+2.
