import SwiftUI
import Charts
import PsyCore

/// 6-month income/expense trend, drawn with Swift Charts grouped BarMark over [MonthBars].
/// Ports TrendBars.kt: per month two bars — Thu (green) and Chi (pink) — plus a small legend.
struct TrendChart: View {
    let trend: [MonthBars]
    @Environment(\.psyColors) private var psyColors

    private let incomeLabel = "Thu"
    private let expenseLabel = "Chi"

    /// Flattened (month, series, amount) rows for the grouped bar chart.
    private struct Bar: Identifiable {
        let id = UUID()
        let month: String
        let series: String
        let amount: Int64
    }

    private var bars: [Bar] {
        trend.flatMap { m in
            [Bar(month: m.label, series: incomeLabel, amount: m.incomeMinor),
             Bar(month: m.label, series: expenseLabel, amount: m.expenseMinor)]
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            // Legend
            HStack(spacing: 6) {
                legendDot(CandyColor.green); Text(incomeLabel).font(PsyFont.labelSmall)
                Spacer().frame(width: 12)
                legendDot(CandyColor.pinkDeep); Text(expenseLabel).font(PsyFont.labelSmall)
            }
            .foregroundStyle(psyColors.onSurface.opacity(0.7))

            Chart(bars) { bar in
                BarMark(
                    x: .value("month", bar.month),
                    y: .value("amount", bar.amount)
                )
                .foregroundStyle(by: .value("series", bar.series))
                .position(by: .value("series", bar.series))
                .cornerRadius(4)
            }
            .chartForegroundStyleScale([
                incomeLabel: CandyColor.green,
                expenseLabel: CandyColor.pinkDeep,
            ])
            .chartLegend(.hidden)
            .chartYAxis(.hidden)
            .chartXAxis {
                AxisMarks(values: .automatic) { _ in
                    AxisValueLabel()
                }
            }
            .frame(height: 150)
        }
    }

    private func legendDot(_ color: Color) -> some View {
        Circle().fill(color).frame(width: 10, height: 10)
    }
}
