import SwiftUI
import PsyCore

/// ‹ MM/yyyy › selector used by Stats / Calendar / Budget.
struct MonthSelector: View {
    let label: String
    let onPrev: () -> Void
    let onNext: () -> Void
    @Environment(\.psyColors) private var colors
    var body: some View {
        HStack(spacing: 16) {
            Button(action: onPrev) { LucideIcon(name: "chevron-left", size: 20, tint: colors.text) }
            Text(label).font(PsyFont.titleMedium).frame(minWidth: 90)
            Button(action: onNext) { LucideIcon(name: "chevron-right", size: 20, tint: colors.text) }
        }
        .foregroundStyle(colors.text)
        .padding(.vertical, 6)
    }
}
