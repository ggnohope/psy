import Foundation

public enum AddEditLogic {
    /// Default date for a new transaction = NOW as a full epoch-millis timestamp (not midnight).
    /// Mirrors Android `nowEpochMillis()` prefill so the logged time-of-day is preserved.
    public static func defaultDate(now: Date) -> Int64 {
        Int64((now.timeIntervalSince1970 * 1000).rounded())
    }

    /// amountMinor = (typed integer) * 10^fractionDigits. Non-digits ignored.
    public static func amountMinor(typed: String, fractionDigits: Int) -> Int64 {
        let digits = typed.filter(\.isNumber)
        let value = Int64(digits) ?? 0
        var mult: Int64 = 1
        for _ in 0..<fractionDigits { mult *= 10 }
        return value * mult
    }

    /// Reverse: minor → typed whole-unit string (used when editing).
    public static func typedString(amountMinor: Int64, fractionDigits: Int) -> String {
        var div: Int64 = 1
        for _ in 0..<fractionDigits { div *= 10 }
        return div > 0 ? String(amountMinor / div) : "0"
    }

    public static func canSave(amountText: String, type: TxType, categoryId: Int64?, accountId: Int64?, toAccountId: Int64?) -> Bool {
        let amount = Int64(amountText.filter(\.isNumber)) ?? 0
        guard amount > 0 else { return false }
        switch type {
        case .income, .expense: return categoryId != nil && accountId != nil
        case .transfer: return accountId != nil && toAccountId != nil && accountId != toAccountId
        }
    }
}
