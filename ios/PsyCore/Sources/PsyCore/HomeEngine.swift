import Foundation

public struct HomeResult: Sendable {
    public let incomeMinor: Int64
    public let expenseMinor: Int64
    public let netMinor: Int64
    public let days: [DayGroup]
}

public enum HomeEngine {
    public static func build(transactions: [Transaction], categories: [Category], accounts: [Account],
                             calendar: Calendar, now: Date) -> HomeResult {
        let catMap = Dictionary(categories.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let accMap = Dictionary(accounts.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })

        let today = calendar.startOfDay(for: now)
        let yesterday = calendar.date(byAdding: .day, value: -1, to: today)!

        // Bucket by start-of-day, preserving input order within each day.
        var order: [Date] = []
        var buckets: [Date: [Transaction]] = [:]
        for tx in transactions {
            let day = calendar.startOfDay(for: Date(timeIntervalSince1970: Double(tx.date) / 1000))
            if buckets[day] == nil { order.append(day) }
            buckets[day, default: []].append(tx)
        }
        let sortedDays = order.sorted(by: >)

        let df = DateFormatter()
        df.calendar = calendar
        df.timeZone = calendar.timeZone
        df.dateFormat = "dd/MM/yyyy"

        let days: [DayGroup] = sortedDays.map { day in
            let label: String = day == today ? "Hôm nay" : (day == yesterday ? "Hôm qua" : df.string(from: day))
            let rows = buckets[day]!.map { TxRowBuilder.make($0, categories: catMap, accounts: accMap) }
            return DayGroup(dateLabel: label, items: rows)
        }

        var income: Int64 = 0, expense: Int64 = 0
        for tx in transactions {
            switch tx.type {
            case .income: income += tx.amountMinor
            case .expense: expense += tx.amountMinor
            case .transfer: break
            }
        }
        return HomeResult(incomeMinor: income, expenseMinor: expense, netMinor: income - expense, days: days)
    }
}
