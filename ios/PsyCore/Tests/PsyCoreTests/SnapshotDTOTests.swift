import XCTest
@testable import PsyCore

final class SnapshotDTOTests: XCTestCase {

    func testDecodeAndroidBlob() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidV2Blob.utf8))
        XCTAssertEqual(dto.version, 2)
        XCTAssertEqual(dto.transactions.first?.toAccountId, nil)
        XCTAssertEqual(dto.transactions.first?.categoryId, 10)
        XCTAssertEqual(dto.budgets.first?.groupId, nil)
        XCTAssertEqual(dto.accounts.first?.type, "CASH")
        XCTAssertEqual(dto.accounts.first?.color, -14370978)
    }

    func testRoundTripPreservesData() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidV2Blob.utf8))
        let reEncoded = try JSONEncoder().encode(dto)
        let dto2 = try JSONDecoder().decode(SnapshotDTO.self, from: reEncoded)
        XCTAssertEqual(dto, dto2)
    }

    func testNilOptionalsEncodeAsExplicitNull() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidV2Blob.utf8))
        let json = String(decoding: try JSONEncoder().encode(dto), as: UTF8.self)
        XCTAssertTrue(json.contains("\"photoUri\":null"), "photoUri nil must serialize as explicit null")
        XCTAssertTrue(json.contains("\"toAccountId\":null"), "toAccountId nil must serialize as explicit null")
        XCTAssertTrue(json.contains("\"groupId\":null"), "budget groupId nil must serialize as explicit null")
    }

    // ── v2 (category hierarchy) ──────────────────────────────────────────────
    // Shaped exactly like Android's kotlinx v2 output: categoryGroups before
    // categories, categories reshaped to {id,groupId,name,icon,sortOrder},
    // budgets keyed by groupId. Guards Android↔iOS byte-compat (spec §10).
    private let androidV2Blob = """
    {"version":2,"ledgers":[{"id":1,"name":"Sổ của tôi","icon":"wallet","currency":"VND","createdAt":1000}],"accounts":[{"id":1,"name":"Tiền mặt","type":"CASH","icon":"💵","color":-14370978}],"categoryGroups":[{"id":1,"name":"Ăn uống","icon":"🍜","color":-7340074,"type":"EXPENSE","sortOrder":0}],"categories":[{"id":10,"groupId":1,"name":"Phở","icon":"🍜","sortOrder":0}],"transactions":[{"id":1,"ledgerId":1,"type":"EXPENSE","amountMinor":50000,"categoryId":10,"accountId":1,"toAccountId":null,"note":"phở","date":1700000000000,"createdAt":1700000000000,"updatedAt":1700000000000,"photoUri":null}],"budgets":[{"id":1,"ledgerId":1,"groupId":null,"amountMinor":1000000}]}
    """

    func testV2EncodesCategoryGroupsAndGroupIdNotColorType() throws {
        let dto = SnapshotDTO(
            ledgers: [LedgerDTO(id: 1, name: "L", icon: "w", currency: "VND", createdAt: 1)],
            accounts: [AccountDTO(id: 1, name: "Cash", type: "CASH", icon: "💵", color: -1)],
            categoryGroups: [CategoryGroupDTO(id: 1, name: "Food", icon: "🍜", color: -7340074, type: "EXPENSE", sortOrder: 0)],
            categories: [CategoryDTO(id: 10, groupId: 1, name: "Phở", icon: "🍜", sortOrder: 0)],
            transactions: [TransactionDTO(id: 1, ledgerId: 1, type: "EXPENSE", amountMinor: 50000, categoryId: 10,
                                          accountId: 1, toAccountId: nil, note: "", date: 1, createdAt: 1, updatedAt: 1, photoUri: nil)],
            budgets: [BudgetDTO(id: 1, ledgerId: 1, groupId: 5, amountMinor: 1000)]
        )
        XCTAssertEqual(dto.version, 2)
        let json = String(decoding: try JSONEncoder().encode(dto), as: UTF8.self)
        XCTAssertTrue(json.contains("\"categoryGroups\""), "snapshot must carry categoryGroups")
        XCTAssertTrue(json.contains("\"groupId\":1"), "category must carry groupId")
        XCTAssertTrue(json.contains("\"groupId\":5"), "budget must carry groupId")
        // The reshaped CategoryDTO must NOT carry color/type anymore (those moved to the group).
        // Inspect the actual category object's keys via JSONSerialization.
        let obj = try JSONSerialization.jsonObject(with: Data(json.utf8)) as! [String: Any]
        let cats = obj["categories"] as! [[String: Any]]
        let catKeys = Set(cats.first!.keys)
        XCTAssertEqual(catKeys, ["id", "groupId", "name", "icon", "sortOrder"],
                       "category DTO keys must be exactly the v2 set (no color/type)")
        XCTAssertFalse(catKeys.contains("color"), "category must NOT have color")
        XCTAssertFalse(catKeys.contains("type"), "category must NOT have type")
        // Top-level snapshot keys must include categoryGroups.
        XCTAssertTrue(Set(obj.keys).contains("categoryGroups"))
    }

    func testV2DecodeAndroidBlobRoundTrips() throws {
        let dto = try JSONDecoder().decode(SnapshotDTO.self, from: Data(androidV2Blob.utf8))
        XCTAssertEqual(dto.version, 2)
        XCTAssertEqual(dto.categoryGroups.first?.name, "Ăn uống")
        XCTAssertEqual(dto.categoryGroups.first?.type, "EXPENSE")
        XCTAssertEqual(dto.categories.first?.groupId, 1)
        XCTAssertEqual(dto.categories.first?.name, "Phở")
        XCTAssertEqual(dto.transactions.first?.categoryId, 10)
        XCTAssertEqual(dto.budgets.first?.groupId, nil)

        let reEncoded = try JSONEncoder().encode(dto)
        let dto2 = try JSONDecoder().decode(SnapshotDTO.self, from: reEncoded)
        XCTAssertEqual(dto, dto2)
    }
}
