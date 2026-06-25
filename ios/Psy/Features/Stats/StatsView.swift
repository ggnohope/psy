import SwiftUI
import PsyCore

/// Stats screen: month selector, account filter, hero summary card, per-account comparison,
/// Chi/Thu pie-mode toggle, donut + legend, top list, 6-month trend.
/// HostGuardIQ re-skin — visuals only; behavior + StatsViewModel bindings unchanged.
struct StatsView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var psyColors
    @State private var vm: StatsViewModel

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: StatsViewModel(container: container))
    }

    private var pieTotal: Int64 { vm.slices.reduce(0) { $0 + $1.amountMinor } }

    // MARK: - Account filter mapping (Tất cả / Tiền mặt / Ngân hàng ↔ VM accountId)

    private var cashAccount: Account? { vm.accounts.first { $0.type == .cash } }
    private var bankAccount: Account? { vm.accounts.first { $0.type == .bank } }

    private var filterIndex: Int {
        guard let id = vm.selectedAccountId else { return 0 }
        if id == cashAccount?.id { return 1 }
        if id == bankAccount?.id { return 2 }
        return 0
    }

    private func selectFilter(_ index: Int) {
        switch index {
        case 1: vm.selectAccount(cashAccount?.id)
        case 2: vm.selectAccount(bankAccount?.id)
        default: vm.selectAccount(nil)
        }
    }

    private var hasData: Bool {
        !vm.slices.isEmpty || !vm.top.isEmpty || vm.summary.incomeMinor != 0 || vm.summary.expenseMinor != 0
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header

                    MonthSelector(label: vm.monthLabel, onPrev: vm.prevMonth, onNext: vm.nextMonth)
                        .frame(maxWidth: .infinity)

                    if !vm.accounts.isEmpty {
                        SegmentedControl(options: ["Tất cả", "Tiền mặt", "Ngân hàng"],
                                         selectedIndex: filterIndex,
                                         onSelect: selectFilter)
                    }

                    summaryCard

                    if hasData {
                        if vm.selectedAccountId == nil {
                            accountBreakdownSection
                        }

                        SegmentedControl(options: ["Chi tiêu", "Thu nhập"],
                                         selectedIndex: vm.pieMode == .expense ? 0 : 1,
                                         onSelect: { vm.setPieMode($0 == 0 ? .expense : .income) })

                        donutSection
                        topSection
                        trendSection
                    } else {
                        EmptyStateView(iconName: "chart-column",
                                       title: "Chưa có dữ liệu",
                                       caption: "Thêm giao dịch để xem thống kê.")
                    }

                    Spacer(minLength: 24)
                }
                .padding(.horizontal, 22)
                .padding(.vertical, 12)
            }
            .background(psyColors.bg.ignoresSafeArea())
            .navigationBarHidden(true)
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            EyebrowLabel(text: "Phân tích")
            Text("Thống kê")
                .font(PsyFont.headlineMedium)
                .foregroundStyle(psyColors.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Hero summary card (navy gradient, 2x2 grid)

    private var summaryCard: some View {
        HeroCard {
            VStack(spacing: 18) {
                HStack(spacing: 18) {
                    summaryItem("Thu", vm.summary.incomeMinor, tint: psyColors.incomeTint)
                    summaryItem("Chi", vm.summary.expenseMinor, tint: psyColors.expenseTint)
                }
                HStack(spacing: 18) {
                    summaryItem("Chênh lệch", vm.summary.netMinor, tint: .white)
                    summaryItem("TB ngày", vm.summary.avgPerDayMinor, tint: .white)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private func summaryItem(_ label: String, _ amountMinor: Int64, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased())
                .font(PsyFont.mono(11))
                .tracking(1.4)
                .foregroundStyle(.white.opacity(0.55))
            Text(vm.currency.format(amountMinor))
                .font(PsyFont.display(20))
                .foregroundStyle(tint)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Per-account comparison

    private var accountBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowLabel(text: "Theo tài khoản")

            if vm.accountBreakdown.isEmpty {
                Text("Kỳ này chưa có giao dịch theo tài khoản")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.text3)
            } else {
                let maxValue = max(1, vm.accountBreakdown.map { max($0.incomeMinor, $0.expenseMinor) }.max() ?? 1)
                ForEach(vm.accountBreakdown) { stat in
                    accountBreakdownRow(stat, maxValue: maxValue)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func accountBreakdownRow(_ stat: AccountStat, maxValue: Int64) -> some View {
        let isCash = vm.accounts.first { $0.id == stat.id }?.type == .cash
        let tileTint = isCash ? psyColors.green : psyColors.blue
        return Button { vm.selectAccount(stat.id) } label: {
            HStack(spacing: 13) {
                IconTile(iconName: stat.icon, tint: tileTint, bg: tileTint.opacity(0.14), size: 42)

                VStack(spacing: 7) {
                    HStack {
                        Text(stat.name)
                            .font(PsyFont.bodyLarge.weight(.semibold))
                            .lineLimit(1)
                            .foregroundStyle(psyColors.text)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(vm.currency.format(stat.netMinor))
                            .font(PsyFont.display(15))
                            .foregroundStyle(stat.netMinor >= 0 ? psyColors.green : psyColors.red)
                    }
                    barLine(fraction: Double(stat.incomeMinor) / Double(maxValue), color: psyColors.green, height: 6)
                    barLine(fraction: Double(stat.expenseMinor) / Double(maxValue), color: psyColors.red, height: 6)
                }
            }
            .padding(14)
            .background(psyColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(psyColors.hair, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    private func barLine(fraction: Double, color: Color, height: CGFloat) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(psyColors.sunken)
                Capsule().fill(color)
                    .frame(width: geo.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: height)
    }

    // MARK: - Donut + legend

    private var donutSection: some View {
        VStack(spacing: 14) {
            ZStack {
                DonutChart(slices: vm.slices, centerLabel: "")
                VStack(spacing: 2) {
                    Text((vm.pieMode == .expense ? "Chi tiêu" : "Thu nhập").uppercased())
                        .font(PsyFont.mono(11))
                        .tracking(1.2)
                        .foregroundStyle(psyColors.text3)
                    Text(pieTotal > 0 ? vm.currency.format(pieTotal) : "—")
                        .font(PsyFont.display(18))
                        .foregroundStyle(psyColors.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
            }
            .frame(maxWidth: .infinity)

            if vm.slices.isEmpty {
                Text("Không có dữ liệu cho tháng này")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.text3)
            } else {
                VStack(spacing: 8) {
                    ForEach(vm.slices) { slice in
                        let percent = pieTotal > 0 ? Int(Double(slice.amountMinor) / Double(pieTotal) * 100) : 0
                        HStack(spacing: 10) {
                            RoundedRectangle(cornerRadius: 3)
                                .fill(Color(argb: slice.color))
                                .frame(width: 10, height: 10)
                            Text(slice.name)
                                .font(PsyFont.bodyMedium)
                                .foregroundStyle(psyColors.text2)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text("\(percent)%")
                                .font(PsyFont.mono(11))
                                .foregroundStyle(psyColors.text3)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Top list

    private var topSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowLabel(text: vm.pieMode == .expense ? "Top chi tiêu" : "Top thu nhập")

            if vm.top.isEmpty {
                Text("Không có danh mục nào trong tháng này")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.text3)
            } else {
                ForEach(vm.top) { entry in
                    topEntryRow(entry)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func topEntryRow(_ entry: TopGroup) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color(argb: entry.color).opacity(0.14))
                    .frame(width: 28, height: 28)
                    .overlay(LucideIcon(name: entry.icon, size: 16, tint: Color(argb: entry.color)))
                Text(entry.name)
                    .font(PsyFont.bodyLarge.weight(.semibold))
                    .foregroundStyle(psyColors.text)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(vm.currency.format(entry.amountMinor))
                    .font(PsyFont.display(15))
                    .foregroundStyle(psyColors.text)
                Text("\(Int(entry.percentOfTotal * 100))%")
                    .font(PsyFont.mono(11))
                    .foregroundStyle(psyColors.text3)
            }
            barLine(fraction: entry.percentOfTotal, color: Color(argb: entry.color), height: 8)
        }
        .padding(14)
        .background(psyColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(psyColors.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    // MARK: - Trend

    private var trendSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowLabel(text: "Xu hướng 6 tháng")

            if vm.trend.isEmpty {
                Text("Không có dữ liệu xu hướng")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.text3)
            } else {
                TrendChart(trend: vm.trend)
                    .padding(14)
                    .background(psyColors.surface)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(psyColors.hair, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
