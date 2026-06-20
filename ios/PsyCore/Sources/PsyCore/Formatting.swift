public extension Currency {
    /// Formats a minor-unit amount with this currency's fraction digits + symbol suffix.
    func format(_ amountMinor: Int64) -> String {
        Money.formatMinor(amountMinor, fractionDigits: fractionDigits, suffix: symbol)
    }
}
