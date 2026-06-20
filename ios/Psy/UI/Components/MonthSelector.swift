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
            Button(action: onPrev) { Image(systemName: "chevron.left") }
            Text(label).font(PsyFont.titleMedium).frame(minWidth: 90)
            Button(action: onNext) { Image(systemName: "chevron.right") }
        }
        .foregroundStyle(colors.onSurface)
        .padding(.vertical, 6)
    }
}
