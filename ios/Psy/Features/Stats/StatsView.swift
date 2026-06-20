import SwiftUI
import PsyCore

/// Stats screen: month selector, account filter chips, gradient summary card,
/// per-account comparison, Chi/Thu pie-mode toggle, donut + legend, top list, 6-month trend.
/// Ports StatsScreen.kt with Candy Pop styling + Swift Charts.
struct StatsView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var psyColors
    @State private var vm: StatsViewModel

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: StatsViewModel(container: container))
    }

    private var pieTotal: Int64 { vm.slices.reduce(0) { $0 + $1.amountMinor } }
    private var centerLabel: String { pieTotal > 0 ? vm.currency.format(pieTotal) : "—" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    MonthSelector(label: vm.monthLabel, onPrev: vm.prevMonth, onNext: vm.nextMonth)
                        .frame(maxWidth: .infinity)

                    if !vm.accounts.isEmpty {
                        accountChipRow
                    }

                    summaryCard

                    if vm.selectedAccountId == nil {
                        accountBreakdownCard
                    }

                    pieModeToggle

                    donutSection

                    topSection

                    trendSection

                    Spacer(minLength: 24)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .background(psyColors.background.ignoresSafeArea())
            .navigationTitle("Thống kê")
        }
    }

    // MARK: - Account filter chips

    private var accountChipRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                accountChip(label: "Tất cả", selected: vm.selectedAccountId == nil) { vm.selectAccount(nil) }
                ForEach(vm.accounts) { account in
                    accountChip(label: "\(account.icon) \(account.name)",
                                selected: vm.selectedAccountId == account.id) { vm.selectAccount(account.id) }
                }
            }
        }
    }

    private func accountChip(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(PsyFont.bodyMedium)
                .lineLimit(1)
                .foregroundStyle(selected ? .white : psyColors.onSurface.opacity(0.7))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? psyColors.primary : psyColors.onSurface.opacity(0.08))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Summary card (gradient)

    private var summaryCard: some View {
        VStack(spacing: 12) {
            HStack {
                summaryItem("Thu", vm.summary.incomeMinor)
                summaryItem("Chi", vm.summary.expenseMinor)
            }
            HStack {
                summaryItem("Chênh lệch", vm.summary.netMinor)
                summaryItem("TB ngày", vm.summary.avgPerDayMinor)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(colors: [CandyColor.violet, CandyColor.sky],
                           startPoint: .leading, endPoint: .trailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func summaryItem(_ label: String, _ amountMinor: Int64) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(PsyFont.labelSmall)
                .foregroundStyle(.white.opacity(0.8))
            Text(vm.currency.format(amountMinor))
                .font(PsyFont.bodyMedium)
                .fontWeight(.bold)
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Per-account comparison card

    private var accountBreakdownCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("💜 Theo tài khoản")
                    .font(PsyFont.titleMedium)
                    .fontWeight(.semibold)
                    .foregroundStyle(psyColors.onSurface)
                Spacer()
                HStack(spacing: 4) {
                    legendDot(CandyColor.green)
                    Text("Thu").font(PsyFont.labelSmall).foregroundStyle(psyColors.onSurface.opacity(0.7))
                    Spacer().frame(width: 6)
                    legendDot(CandyColor.pinkDeep)
                    Text("Chi").font(PsyFont.labelSmall).foregroundStyle(psyColors.onSurface.opacity(0.7))
                }
            }

            if vm.accountBreakdown.isEmpty {
                Text("Kỳ này chưa có giao dịch theo tài khoản")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))
            } else {
                let maxValue = max(1, vm.accountBreakdown.map { max($0.incomeMinor, $0.expenseMinor) }.max() ?? 1)
                ForEach(vm.accountBreakdown) { stat in
                    accountBreakdownRow(stat, maxValue: maxValue)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(psyColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func accountBreakdownRow(_ stat: AccountStat, maxValue: Int64) -> some View {
        Button { vm.selectAccount(stat.id) } label: {
            HStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(argb: stat.color).opacity(0.18))
                    Text(stat.icon).font(PsyFont.bodyMedium)
                }
                .frame(width: 36, height: 36)

                VStack(spacing: 3) {
                    HStack {
                        Text(stat.name)
                            .font(PsyFont.bodyMedium)
                            .fontWeight(.medium)
                            .lineLimit(1)
                            .foregroundStyle(psyColors.onSurface)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(vm.currency.format(stat.netMinor))
                            .font(PsyFont.labelSmall)
                            .fontWeight(.bold)
                            .foregroundStyle(stat.netMinor >= 0 ? CandyColor.green : CandyColor.pinkDeep)
                    }
                    barLine(fraction: Double(stat.incomeMinor) / Double(maxValue), color: CandyColor.green)
                    barLine(fraction: Double(stat.expenseMinor) / Double(maxValue), color: CandyColor.pinkDeep)
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }

    private func barLine(fraction: Double, color: Color) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(psyColors.onSurface.opacity(0.08))
                Capsule().fill(color)
                    .frame(width: geo.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: 6)
    }

    // MARK: - Pie-mode toggle

    private var pieModeToggle: some View {
        HStack(spacing: 0) {
            toggleItem("Chi tiêu", selected: vm.pieMode == .expense) { vm.setPieMode(.expense) }
            toggleItem("Thu nhập", selected: vm.pieMode == .income) { vm.setPieMode(.income) }
        }
        .padding(4)
        .background(psyColors.onSurface.opacity(0.08))
        .clipShape(Capsule())
    }

    private func toggleItem(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(PsyFont.bodyMedium)
                .foregroundStyle(selected ? .white : psyColors.onSurface.opacity(0.7))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(selected ? psyColors.primary : .clear)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Donut + legend

    private var donutSection: some View {
        VStack(spacing: 8) {
            DonutChart(slices: vm.slices, centerLabel: centerLabel)
                .frame(maxWidth: .infinity)

            if vm.slices.isEmpty {
                Text("Không có dữ liệu cho tháng này")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))
            } else {
                VStack(spacing: 4) {
                    ForEach(vm.slices) { slice in
                        let percent = pieTotal > 0 ? Int(Double(slice.amountMinor) / Double(pieTotal) * 100) : 0
                        HStack {
                            Circle().fill(Color(argb: slice.color)).frame(width: 12, height: 12)
                            Text(slice.name)
                                .font(PsyFont.labelSmall)
                                .foregroundStyle(psyColors.onSurface)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text("\(percent)%")
                                .font(PsyFont.labelSmall)
                                .foregroundStyle(psyColors.onSurface.opacity(0.6))
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Top list

    private var topSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(vm.pieMode == .expense ? "Top chi tiêu" : "Top thu nhập")
                .font(PsyFont.titleMedium)
                .fontWeight(.semibold)
                .foregroundStyle(psyColors.onSurface)

            if vm.top.isEmpty {
                Text("Không có danh mục nào trong tháng này")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))
            } else {
                ForEach(vm.top) { entry in
                    topEntryRow(entry)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func topEntryRow(_ entry: TopEntry) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("\(entry.category.icon) \(entry.category.name)")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.onSurface)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(vm.currency.format(entry.amountMinor))
                    .font(PsyFont.labelSmall)
                    .fontWeight(.medium)
                    .foregroundStyle(psyColors.onSurface)
                Text("(\(Int(entry.percent * 100))%)")
                    .font(PsyFont.labelSmall)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))
            }
            barLine(fraction: entry.percent, color: Color(argb: entry.category.color))
                .frame(height: 8)
        }
    }

    // MARK: - Trend

    private var trendSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Xu hướng 6 tháng")
                .font(PsyFont.titleMedium)
                .fontWeight(.semibold)
                .foregroundStyle(psyColors.onSurface)

            if vm.trend.isEmpty {
                Text("Không có dữ liệu xu hướng")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))
            } else {
                TrendChart(trend: vm.trend)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func legendDot(_ color: Color) -> some View {
        Circle().fill(color).frame(width: 8, height: 8)
    }
}
