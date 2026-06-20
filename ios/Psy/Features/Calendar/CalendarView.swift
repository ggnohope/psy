import SwiftUI
import PsyCore

/// Calendar screen: month selector, a Monday-start day grid with income/expense dots,
/// and the selected day's transaction list. Ports CalendarScreen.kt.
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
                VStack(spacing: 0) {
                    MonthSelector(label: vm.monthLabel, onPrev: vm.prevMonth, onNext: vm.nextMonth)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)

                    weekdayHeader
                    Spacer().frame(height: 4)

                    if !vm.loading {
                        grid
                    }

                    Divider().padding(.top, 8)

                    dayList

                    Spacer(minLength: 80)
                }
            }
            .background(psyColors.background.ignoresSafeArea())
            .navigationTitle("Lịch")
        }
    }

    // MARK: - Weekday header (Monday-first)

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Self.weekdayHeaders, id: \.self) { label in
                Text(label)
                    .font(PsyFont.labelSmall)
                    .foregroundStyle(psyColors.onSurface.opacity(0.55))
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 4)
    }

    // MARK: - Grid

    private var grid: some View {
        VStack(spacing: 0) {
            ForEach(Array(vm.weeks.enumerated()), id: \.offset) { _, week in
                HStack(spacing: 0) {
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
        .padding(.horizontal, 4)
    }

    private func dayCell(_ cell: DayCell) -> some View {
        let isSelected = vm.selectedDay.map { Calendar.current.isDate($0, inSameDayAs: cell.date) } ?? false
        return Button {
            vm.selectDay(cell.date)
        } label: {
            VStack(spacing: 2) {
                Text("\(cell.day)")
                    .font(PsyFont.bodyMedium)
                    .fontWeight(cell.isToday ? .bold : .regular)
                    .foregroundStyle(cell.isToday ? psyColors.primary : psyColors.onSurface)
                HStack(spacing: 2) {
                    if cell.expenseMinor > 0 {
                        Circle().fill(CandyColor.pinkDeep).frame(width: 5, height: 5)
                    }
                    if cell.incomeMinor > 0 {
                        Circle().fill(CandyColor.green).frame(width: 5, height: 5)
                    }
                }
                .frame(height: 5)
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(cellBackground(cell: cell, isSelected: isSelected))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(isSelected ? CandyColor.violet : .clear, lineWidth: 2)
            )
            .padding(2)
        }
        .buttonStyle(.plain)
    }

    private func cellBackground(cell: DayCell, isSelected: Bool) -> Color {
        if isSelected { return CandyColor.violet.opacity(0.18) }
        if cell.isToday { return psyColors.primary.opacity(0.12) }
        return .clear
    }

    // MARK: - Selected-day transaction list

    private var dayList: some View {
        Group {
            if let sel = vm.selectedDay {
                Text("Giao dịch ngày \(Self.dayMonthLabel(sel))")
                    .font(PsyFont.titleMedium)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)

                if vm.dayRows.isEmpty {
                    emptyState("Không có giao dịch")
                } else {
                    ForEach(vm.dayRows) { row in
                        txRow(row)
                    }
                }
            } else {
                emptyState("Chưa chọn ngày")
            }
        }
    }

    private static func dayMonthLabel(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "dd/MM"
        return f.string(from: date)
    }

    // MARK: - Transaction row (same styling as Home, non-interactive)

    private func txRow(_ row: TxRow) -> some View {
        HStack(spacing: 0) {
            ZStack {
                Circle().fill(iconBackground(row.type))
                Text(row.icon).font(.system(size: 22))
            }
            .frame(width: 44, height: 44)

            Spacer().frame(width: 12)

            VStack(alignment: .leading, spacing: 2) {
                if row.type == .transfer {
                    Text("\(row.accountName) → \(row.toAccountName ?? "—")")
                        .font(PsyFont.bodyMedium)
                        .fontWeight(.semibold)
                        .foregroundStyle(psyColors.onSurface)
                } else {
                    Text(row.title)
                        .font(PsyFont.bodyMedium)
                        .fontWeight(.semibold)
                        .foregroundStyle(psyColors.onSurface)
                }
                if !row.note.trimmingCharacters(in: .whitespaces).isEmpty {
                    Text(row.note)
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(psyColors.onSurface.opacity(0.6))
                        .lineLimit(1)
                }
                if row.type != .transfer {
                    Text(row.accountName)
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(psyColors.onSurface.opacity(0.45))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Spacer().frame(width: 8)

            MoneyText(amountMinor: row.amountMinor, currency: vm.currency, prefix: amountSign(row.type))
                .font(PsyFont.bodyMedium)
                .fontWeight(.bold)
                .foregroundStyle(amountColor(row.type))
        }
        .padding(12)
        .background(psyColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: CandyShape.medium))
        .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }

    private func iconBackground(_ type: TxType) -> Color {
        switch type {
        case .expense: return CandyColor.pinkDeep.opacity(0.15)
        case .income: return CandyColor.green.opacity(0.15)
        case .transfer: return psyColors.onSurface.opacity(0.08)
        }
    }

    private func amountSign(_ type: TxType) -> String {
        switch type {
        case .expense: return "-"
        case .income: return "+"
        case .transfer: return ""
        }
    }

    private func amountColor(_ type: TxType) -> Color {
        switch type {
        case .expense: return CandyColor.pinkDeep
        case .income: return CandyColor.green
        case .transfer: return psyColors.onSurface
        }
    }

    private func emptyState(_ message: String) -> some View {
        Text(message)
            .font(PsyFont.bodyMedium)
            .foregroundStyle(psyColors.onSurface.opacity(0.4))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
    }
}
