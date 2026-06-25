import SwiftUI

/// Rounded progress bar for budgets. Ports `BudgetProgress.kt`:
/// fill = min(spent/limit, 1.0); over-budget (spent > limit) → red, otherwise green.
/// Track is the sunken token.
struct BudgetProgress: View {
    let spentMinor: Int64
    let limitMinor: Int64
    var height: CGFloat = 10
    var fillColorOverride: Color? = nil
    @Environment(\.psyColors) private var psyColors

    private var fraction: Double {
        guard limitMinor > 0 else { return 0 }
        return min(max(Double(spentMinor) / Double(limitMinor), 0), 1)
    }

    private var fillColor: Color {
        if let fillColorOverride { return fillColorOverride }
        return (spentMinor > limitMinor && limitMinor > 0) ? psyColors.red : psyColors.green
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(psyColors.sunken)
                Capsule()
                    .fill(fillColor)
                    .frame(width: geo.size.width * fraction)
            }
        }
        .frame(height: height)
    }
}
