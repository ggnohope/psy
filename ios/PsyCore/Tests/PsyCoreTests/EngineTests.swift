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
    private func cat(_ id: Int64, _ name: String, _ t: CategoryType) -> PsyCore.Category { PsyCore.Category(id: id, name: name, icon: "🍜", color: 0xFFFF8FD6, type: t, sortOrder: 0) }
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
