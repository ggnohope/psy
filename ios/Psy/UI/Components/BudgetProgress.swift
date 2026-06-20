import SwiftUI

/// Rounded progress bar for budgets. Ports `BudgetProgress.kt`:
/// fill = min(spent/limit, 1.0); over-budget (spent > limit) is tinted a warning color
/// (`CandyColor.pinkDeep`), otherwise the accent `primary`. The track is a faint violet.
struct BudgetProgress: View {
    let spentMinor: Int64
    let limitMinor: Int64
    @Environment(\.psyColors) private var psyColors

    private var fraction: Double {
        guard limitMinor > 0 else { return 0 }
        return min(max(Double(spentMinor) / Double(limitMinor), 0), 1)
    }

    private var fillColor: Color {
        (spentMinor > limitMinor && limitMinor > 0) ? CandyColor.pinkDeep : psyColors.primary
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(CandyColor.violet.opacity(0.15))
                Capsule()
                    .fill(fillColor)
                    .frame(width: geo.size.width * fraction)
            }
        }
        .frame(height: 12)
    }
}
