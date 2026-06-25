import SwiftUI
import Charts
import PsyCore

/// Donut chart over [PieSlice], drawn with Swift Charts SectorMark.
/// Slice colors come from the engine's fixed palette (PieSlice.color, ARGB). Ports DonutChart.kt
/// (a stroked ring with a center label). When there are no slices, shows a muted placeholder ring.
struct DonutChart: View {
    let slices: [PieSlice]
    var centerLabel: String = ""
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        ZStack {
            if slices.isEmpty {
                // Muted placeholder ring (mirrors Compose's grey full-circle arc).
                Circle()
                    .stroke(psyColors.sunken, lineWidth: 36)
                    .padding(18)
            } else {
                Chart(slices) { slice in
                    SectorMark(
                        angle: .value("amount", slice.amountMinor),
                        innerRadius: .ratio(0.62),
                        angularInset: 1.5
                    )
                    .cornerRadius(2)
                    .foregroundStyle(Color(argb: slice.color))
                }
                .chartLegend(.hidden)
            }

            if !centerLabel.isEmpty {
                Text(centerLabel)
                    .font(PsyFont.titleMedium)
                    .foregroundStyle(psyColors.text)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(width: 200, height: 200)
    }
}
