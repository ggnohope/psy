/// Formatting helpers for amounts stored as minor units (cents, đồng).
/// Integer arithmetic throughout so precision is never lost (amounts are Int64 minor units).
public enum Money {
    /// Renders a minor-unit amount as a grouped decimal string with a currency suffix.
    /// Always shows exactly `fractionDigits` decimal places.
    public static func formatMinor(_ amountMinor: Int64, fractionDigits: Int, suffix: String) -> String {
        var divisor: Int64 = 1
        for _ in 0..<fractionDigits { divisor *= 10 }

        let absAmount = amountMinor < 0 ? -amountMinor : amountMinor
        let whole = absAmount / divisor
        let frac = absAmount % divisor

        let groupedWhole = groupedDecimal(whole)
        let sign = amountMinor < 0 ? "-" : ""

        if fractionDigits > 0 {
            let fracStr = leftPad(String(frac), to: fractionDigits, with: "0")
            return "\(sign)\(groupedWhole).\(fracStr) \(suffix)"
        } else {
            return "\(sign)\(groupedWhole) \(suffix)"
        }
    }

    /// Groups a non-negative integer with commas every 3 digits (DecimalFormat "#,##0", US).
    private static func groupedDecimal(_ value: Int64) -> String {
        let digits = Array(String(value))
        var out = ""
        for (i, c) in digits.enumerated() {
            if i > 0 && (digits.count - i) % 3 == 0 { out.append(",") }
            out.append(c)
        }
        return out
    }

    private static func leftPad(_ s: String, to length: Int, with pad: Character) -> String {
        s.count >= length ? s : String(repeating: pad, count: length - s.count) + s
    }
}
