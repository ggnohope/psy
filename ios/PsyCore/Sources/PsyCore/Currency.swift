/// Minimal currency metadata for formatting. v1 supports VND + USD.
public struct Currency: Hashable, Sendable {
    public let code: String
    public let symbol: String
    public let fractionDigits: Int

    public init(code: String, symbol: String, fractionDigits: Int) {
        self.code = code; self.symbol = symbol; self.fractionDigits = fractionDigits
    }

    public static let vnd = Currency(code: "VND", symbol: "đ", fractionDigits: 0)
    public static let usd = Currency(code: "USD", symbol: "$", fractionDigits: 2)

    public static func of(_ code: String) -> Currency {
        switch code {
        case "USD": return .usd
        default: return .vnd
        }
    }
}
