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
