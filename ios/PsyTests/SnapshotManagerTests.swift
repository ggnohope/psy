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
