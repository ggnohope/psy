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
