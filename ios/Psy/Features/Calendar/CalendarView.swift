import SwiftUI
import PsyCore

/// Calendar screen: month switcher, a Monday-start day grid with income/expense dots,
/// and the selected day's transaction list. Ports CalendarScreen.kt. HostGuardIQ re-skin.
struct CalendarView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var psyColors
    @State private var vm: CalendarViewModel

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: CalendarViewModel(container: container))
    }

    private static let weekdayHeaders = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    titleBlock

                    MonthSwitcher(label: vm.monthLabel, onPrev: vm.prevMonth, onNext: vm.nextMonth)

                    calendarCard

                    dayDivider

                    daySection

                    Spacer(minLength: 80)
                }
                .padding(22)
            }
            .background(psyColors.bg.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    // MARK: - Title

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            EyebrowLabel(text: "Dòng thời gian")
            Text("Lịch")
                .font(PsyFont.headlineMedium)
                .foregroundStyle(psyColors.text)
        }
    }

    // MARK: - Calendar card

    private var calendarCard: some View {
        VStack(spacing: 10) {
            weekdayHeader
            if !vm.loading {
                grid
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(psyColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(psyColors.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Self.weekdayHeaders, id: \.self) { label in
                Text(label)
                    .font(PsyFont.mono(11))
                    .foregroundStyle(psyColors.text3)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - Grid

    private var grid: some View {
        VStack(spacing: 4) {
            ForEach(Array(vm.weeks.enumerated()), id: \.offset) { _, week in
                HStack(spacing: 4) {
                    ForEach(Array(week.enumerated()), id: \.offset) { _, cell in
                        if let cell {
                            dayCell(cell)
                                .frame(maxWidth: .infinity)
                        } else {
                            Color.clear
                                .aspectRatio(1, contentMode: .fit)
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }

    private func dayCell(_ cell: DayCell) -> some View {
        let isSelected = vm.selectedDay.map { Calendar.current.isDate($0, inSameDayAs: cell.date) } ?? false
        return Button {
            vm.selectDay(cell.date)
        } label: {
            VStack(spacing: 3) {
                Text("\(cell.day)")
                    .font(PsyFont.bodyMedium)
                    .fontWeight(isSelected || cell.isToday ? .bold : .regular)
                    .foregroundStyle(isSelected ? psyColors.blue : psyColors.text)
                HStack(spacing: 3) {
                    if cell.expenseMinor > 0 {
                        Circle().fill(psyColors.red).frame(width: 5, height: 5)
                    }
                    if cell.incomeMinor > 0 {
                        Circle().fill(psyColors.green).frame(width: 5, height: 5)
                    }
                }
                .frame(height: 5)
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(isSelected ? psyColors.blueSoft : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 9))
            .overlay(
                RoundedRectangle(cornerRadius: 9)
                    .stroke(isSelected ? psyColors.blue : .clear, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Day divider

    private var dayDivider: some View {
        HStack(spacing: 10) {
            Text(dividerLabel)
                .font(PsyFont.mono(11))
                .foregroundStyle(psyColors.text3)
                .fixedSize()
            Rectangle().fill(psyColors.hair).frame(height: 1)
        }
    }

    private var dividerLabel: String {
        if let sel = vm.selectedDay {
            return "Giao dịch · \(Self.dayMonthLabel(sel))"
        }
        return "Giao dịch"
    }

    private static func dayMonthLabel(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "dd/MM"
        return f.string(from: date)
    }

    // MARK: - Selected-day transaction list

    private var daySection: some View {
        Group {
            if vm.selectedDay != nil, !vm.dayRows.isEmpty {
                VStack(spacing: 10) {
                    ForEach(vm.dayRows) { row in
                        txRow(row)
                    }
                }
            } else {
                EmptyStateView(
                    iconName: "calendar",
                    title: "Không có giao dịch",
                    caption: "Chọn ngày khác hoặc thêm giao dịch."
                )
            }
        }
    }

    // MARK: - Transaction row (shared, same mapping as Home)

    private func txRow(_ row: TxRow) -> some View {
        let isTransfer = row.type == .transfer
        let isIncome = row.type == .income
        let name = isTransfer ? "\(row.accountName) → \(row.toAccountName ?? "—")" : row.title
        let meta = row.note.trimmingCharacters(in: .whitespaces).isEmpty ? row.timeLabel : row.note
        return TransactionRowView(
            iconName: row.icon,
            iconTint: psyColors.blue,
            iconBg: psyColors.blue.opacity(0.14),
            name: name,
            meta: meta,
            amount: amountSign(row.type) + vm.currency.format(row.amountMinor),
            isIncome: isIncome,
            account: row.accountName,
            isFund: row.isFund
        )
    }

    private func amountSign(_ type: TxType) -> String {
        switch type {
        case .expense: return "-"
        case .income: return "+"
        case .transfer: return ""
        }
    }
}
