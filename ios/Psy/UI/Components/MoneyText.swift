import SwiftUI
import PsyCore

/// Renders a minor-unit amount using a Currency. Optional sign prefix for income(+)/expense(-).
struct MoneyText: View {
    let amountMinor: Int64
    var currency: Currency = .vnd
    var prefix: String = ""
    var body: some View {
        Text(prefix + currency.format(amountMinor))
    }
}
