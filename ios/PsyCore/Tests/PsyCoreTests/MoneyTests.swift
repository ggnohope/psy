import XCTest
@testable import PsyCore

final class MoneyTests: XCTestCase {
    func testVndGrouping() {
        XCTAssertEqual(Money.formatMinor(1_234_567, fractionDigits: 0, suffix: "đ"), "1,234,567 đ")
    }
    func testVndZero() {
        XCTAssertEqual(Money.formatMinor(0, fractionDigits: 0, suffix: "đ"), "0 đ")
    }
    func testVndNegative() {
        XCTAssertEqual(Money.formatMinor(-5000, fractionDigits: 0, suffix: "đ"), "-5,000 đ")
    }
    func testUsdTwoDecimals() {
        XCTAssertEqual(Money.formatMinor(1234, fractionDigits: 2, suffix: "$"), "12.34 $")
    }
    func testUsdPadsFraction() {
        XCTAssertEqual(Money.formatMinor(5, fractionDigits: 2, suffix: "$"), "0.05 $")
    }
    func testUsdWholeKeepsTwoDecimals() {
        XCTAssertEqual(Money.formatMinor(100, fractionDigits: 2, suffix: "$"), "1.00 $")
    }
}
