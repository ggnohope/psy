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
    private func grp(_ id: Int64, _ name: String, _ t: CategoryType) -> CategoryGroup { CategoryGroup(id: id, name: name, icon: "📁", color: 0xFFFF8FD6, type: t, sortOrder: 0) }
    /// Leaf in group `g` (color/type are derived from the group now).
    private func cat(_ id: Int64, _ name: String, group g: Int64) -> PsyCore.Category { PsyCore.Category(id: id, groupId: g, name: name, icon: "🍜", sortOrder: 0) }
    private func tx(_ id: Int64, _ type: TxType, _ amount: Int64, cat: Int64?, acc: Int64, to: Int64? = nil, at: Int64) -> Transaction {
        Transaction(id: id, ledgerId: 1, type: type, amountMinor: amount, categoryId: cat, accountId: acc, toAccountId: to, note: "", date: at, createdAt: at, updatedAt: at, photoUri: nil)
    }

    func testHomeExcludesTransferFromSums() {
        let txns = [
            tx(1, .income, 1000, cat: 10, acc: 1, at: millis(2026, 6, 10)),
            tx(2, .expense, 400, cat: 20, acc: 1, at: millis(2026, 6, 10)),
            tx(3, .transfer, 999, cat: nil, acc: 1, to: 2, at: millis(2026, 6, 10)),
        ]
        let r = HomeEngine.build(transactions: txns,
                                 categories: [cat(10, "Lương", group: 100), cat(20, "Ăn", group: 200)],
                                 groups: [grp(100, "Thu nhập", .income), grp(200, "Ăn uống", .expense)],
                                 accounts: [acc(1, "Tiền mặt"), acc(2, "Bank")], calendar: cal(),
                                 now: Date(timeIntervalSince1970: Double(millis(2026, 6, 15)) / 1000))
        XCTAssertEqual(r.incomeMinor, 1000)
        XCTAssertEqual(r.expenseMinor, 400)
        XCTAssertEqual(r.netMinor, 600)
        // Row exposes leaf name + parent group name + time-of-day (12:00 local from millis(...,12)).
        let foodRow = r.days.flatMap { $0.items }.first { $0.title == "Ăn" }
        XCTAssertEqual(foodRow?.groupName, "Ăn uống")
        XCTAssertEqual(foodRow?.timeLabel, "12:00")
    }

    func testStatsPiePaletteByIndexAndAvg() {
        let month = PsyMonth(year: 2026, month: 6)
        let txns = [
            tx(1, .expense, 600, cat: 20, acc: 1, at: millis(2026, 6, 2)),
            tx(2, .expense, 400, cat: 21, acc: 1, at: millis(2026, 6, 3)),
        ]
        // Two leaves in two separate groups → two pie slices (one per group).
        let cats = [cat(20, "Ăn", group: 200), cat(21, "Đi lại", group: 201)]
        let groups = [grp(200, "Ăn uống", .expense), grp(201, "Di chuyển", .expense)]
        let now = Date(timeIntervalSince1970: Double(millis(2026, 6, 10)) / 1000) // day 10 of current month
        let r = StatsEngine.build(windowTransactions: txns, categories: cats, groups: groups, accounts: [acc(1, "TM")],
                                  month: month, pieMode: .expense, accountFilter: nil, calendar: cal(), now: now)
        // Largest group slice first, palette index 0 then 1. Slice name = GROUP name.
        XCTAssertEqual(r.slices.first?.name, "Ăn uống")
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
        let r = StatsEngine.build(windowTransactions: txns, categories: [cat(20, "Ăn", group: 200)],
                                  groups: [grp(200, "Ăn uống", .expense)],
                                  accounts: [acc(1, "A"), acc(2, "B")], month: month, pieMode: .expense,
                                  accountFilter: 1, calendar: cal(), now: Date(timeIntervalSince1970: Double(millis(2026, 6, 10)) / 1000))
        XCTAssertEqual(r.summary.expenseMinor, 500)            // only account 1
        XCTAssertEqual(r.accountBreakdown.count, 2)            // breakdown still shows both
    }

    func testCalendarMondayGridLeading() {
        // June 2026: day 1 is a Monday → 0 leading nulls.
        let r = CalendarEngine.build(monthTransactions: [], month: PsyMonth(year: 2026, month: 6),
                                     categories: [], groups: [], accounts: [], selectedDay: nil, calendar: cal(),
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
        let budgets = [Budget(id: 1, ledgerId: 1, groupId: nil, amountMinor: 1000)]
        let r = BudgetEngine.build(monthTransactions: txns, budgets: budgets,
                                   categories: [cat(20, "Ăn", group: 200)], groups: [grp(200, "Ăn uống", .expense)])
        XCTAssertEqual(r.total?.spentMinor, 700)   // income + transfer excluded
        XCTAssertEqual(r.total?.percent ?? 0, 0.7, accuracy: 0.0001)
    }

    /// Per-group budget: spent = sum of EXPENSE tx whose leaf's groupId == budget.groupId.
    func testBudgetPerGroupSumsLeavesOfGroup() {
        let txns = [
            tx(1, .expense, 300, cat: 20, acc: 1, at: millis(2026, 6, 2)), // leaf 20 → group 200
            tx(2, .expense, 200, cat: 21, acc: 1, at: millis(2026, 6, 3)), // leaf 21 → group 200
            tx(3, .expense, 999, cat: 30, acc: 1, at: millis(2026, 6, 4)), // leaf 30 → group 300 (other)
        ]
        let cats = [cat(20, "Phở", group: 200), cat(21, "Cơm", group: 200), cat(30, "Xăng", group: 300)]
        let groups = [grp(200, "Ăn uống", .expense), grp(300, "Di chuyển", .expense)]
        let budgets = [Budget(id: 5, ledgerId: 1, groupId: 200, amountMinor: 1000)]
        let r = BudgetEngine.build(monthTransactions: txns, budgets: budgets, categories: cats, groups: groups)
        let item = r.categoryBudgets.first { $0.budget.groupId == 200 }
        XCTAssertEqual(item?.spentMinor, 500)               // 300 + 200, group 300 excluded
        XCTAssertEqual(item?.group?.name, "Ăn uống")
        XCTAssertEqual(item?.percent ?? 0, 0.5, accuracy: 0.0001)
        // Group 300 is unbudgeted → appears in availableGroups.
        XCTAssertTrue(r.availableGroups.contains { $0.id == 300 })
        XCTAssertFalse(r.availableGroups.contains { $0.id == 200 })
    }

    /// Stats top list: group totals + per-leaf percentInGroup, group percentOfTotal, divide guards.
    func testStatsTopByGroup() {
        let month = PsyMonth(year: 2026, month: 6)
        let txns = [
            tx(1, .expense, 600, cat: 20, acc: 1, at: millis(2026, 6, 2)), // group 200
            tx(2, .expense, 200, cat: 21, acc: 1, at: millis(2026, 6, 3)), // group 200
            tx(3, .expense, 200, cat: 30, acc: 1, at: millis(2026, 6, 4)), // group 300
        ]
        let cats = [cat(20, "Phở", group: 200), cat(21, "Cơm", group: 200), cat(30, "Xăng", group: 300)]
        let groups = [grp(200, "Ăn uống", .expense), grp(300, "Di chuyển", .expense)]
        let now = Date(timeIntervalSince1970: Double(millis(2026, 6, 10)) / 1000)
        let r = StatsEngine.build(windowTransactions: txns, categories: cats, groups: groups, accounts: [acc(1, "TM")],
                                  month: month, pieMode: .expense, accountFilter: nil, calendar: cal(), now: now)
        // pieTotal = 1000; group 200 = 800, group 300 = 200.
        let g200 = r.top.first { $0.groupId == 200 }
        XCTAssertEqual(r.top.first?.groupId, 200)            // sorted desc by amount
        XCTAssertEqual(g200?.amountMinor, 800)
        XCTAssertEqual(g200?.count, 2)
        XCTAssertEqual(g200?.percentOfTotal ?? 0, 0.8, accuracy: 0.0001)
        // Leaf percentInGroup: 600/800 and 200/800; largest leaf first.
        XCTAssertEqual(g200?.children.first?.name, "Phở")
        XCTAssertEqual(g200?.children.first?.percentInGroup ?? 0, 0.75, accuracy: 0.0001)
        XCTAssertEqual(g200?.children.last?.percentInGroup ?? 0, 0.25, accuracy: 0.0001)
        // Top color comes from palette index, NOT group.color.
        XCTAssertEqual(g200?.color, StatsEngine.piePalette[0])
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
