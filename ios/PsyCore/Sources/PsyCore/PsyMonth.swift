import Foundation

/// A calendar month, with epoch-millis boundaries computed against an injected Calendar
/// (the Calendar carries the timezone — mirrors ZoneId.systemDefault() in the Android VMs).
public struct PsyMonth: Hashable, Sendable {
    public var year: Int
    public var month: Int // 1...12
    public init(year: Int, month: Int) { self.year = year; self.month = month }

    public static func current(_ cal: Calendar, now: Date) -> PsyMonth {
        let c = cal.dateComponents([.year, .month], from: now)
        return PsyMonth(year: c.year!, month: c.month!)
    }

    public func adding(_ months: Int, _ cal: Calendar) -> PsyMonth {
        let base = cal.date(from: DateComponents(year: year, month: month, day: 1))!
        let shifted = cal.date(byAdding: .month, value: months, to: base)!
        let c = cal.dateComponents([.year, .month], from: shifted)
        return PsyMonth(year: c.year!, month: c.month!)
    }

    /// Epoch millis at the start of day of day 1 (half-open range start).
    public func startMillis(_ cal: Calendar) -> Int64 {
        let day1 = cal.date(from: DateComponents(year: year, month: month, day: 1))!
        return Int64(cal.startOfDay(for: day1).timeIntervalSince1970 * 1000)
    }

    /// Epoch millis at the start of next month (half-open range end).
    public func endMillis(_ cal: Calendar) -> Int64 { adding(1, cal).startMillis(cal) }

    public func lengthOfMonth(_ cal: Calendar) -> Int {
        let day1 = cal.date(from: DateComponents(year: year, month: month, day: 1))!
        return cal.range(of: .day, in: .month, for: day1)!.count
    }

    public func atDay(_ day: Int, _ cal: Calendar) -> Date {
        cal.startOfDay(for: cal.date(from: DateComponents(year: year, month: month, day: day))!)
    }

    public var label: String { String(format: "%02d/%04d", month, year) }      // MM/yyyy
    public var shortLabel: String { "\(month)/\(year % 100)" }                   // M/yy (trend)
}
