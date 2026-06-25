import SwiftUI
import PsyCore

/// Home screen: month summary hero + grouped transaction list. Ports HomeScreen.kt.
/// Canonical example of a ViewModel-backed feature screen. Re-skinned to HostGuardIQ.
struct HomeView: View {
    let container: AppContainer
    let appVM: AppViewModel
    @Environment(\.psyColors) private var psyColors
    @State private var vm: HomeViewModel

    @State private var showAdd = false
    @State private var showSettings = false
    @State private var editTxId: Int64?

    init(container: AppContainer, appVM: AppViewModel) {
        self.container = container
        self.appVM = appVM
        _vm = State(initialValue: HomeViewModel(container: container))
    }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                psyColors.bg.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        header
                        balanceHero

                        EyebrowLabel(text: "Hôm nay")

                        if vm.days.isEmpty && !vm.loading {
                            EmptyStateView(
                                iconName: "receipt",
                                title: "Chưa có giao dịch",
                                caption: "Thêm giao dịch đầu tiên của bạn hôm nay."
                            )
                        } else {
                            ForEach(vm.days) { dayGroup in
                                sectionHeader(dayGroup.dateLabel)
                                VStack(spacing: 10) {
                                    ForEach(dayGroup.items) { row in
                                        txRow(row)
                                    }
                                }
                            }
                        }

                        Spacer(minLength: 80)
                    }
                    .padding(.horizontal, 22)
                    .padding(.top, 8)
                }

                fab
            }
            .navigationBarHidden(true)
            .navigationDestination(isPresented: $showSettings) {
                SettingsView(container: container, appVM: appVM)
            }
            .sheet(isPresented: $showAdd) {
                AddEditView(container: container, txId: 0)
            }
            .sheet(item: Binding(
                get: { editTxId.map { EditTarget(id: $0) } },
                set: { editTxId = $0?.id }
            )) { target in
                AddEditView(container: container, txId: target.id)
            }
        }
    }

    private struct EditTarget: Identifiable { let id: Int64 }

    // MARK: - Header

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                EyebrowLabel(text: "Tổng quan")
                Text("Psy")
                    .font(PsyFont.display(28))
                    .foregroundStyle(psyColors.text)
            }
            Spacer()
            Button { showSettings = true } label: {
                RoundedRectangle(cornerRadius: 12)
                    .fill(psyColors.surface)
                    .frame(width: 42, height: 42)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(psyColors.hair, lineWidth: 1))
                    .overlay(LucideIcon(name: "settings", size: 20, tint: psyColors.text2))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Cài đặt")
        }
    }

    // MARK: - Balance hero

    private var balanceHero: some View {
        HeroCard {
            HStack {
                Text("Số dư · \(vm.monthLabel)")
                    .font(PsyFont.mono(11))
                    .foregroundStyle(Color(argb: 0xFFAEC4DA))
                Spacer()
                HStack(spacing: 5) {
                    Circle().fill(psyColors.teal).frame(width: 6, height: 6)
                    Text("LIVE")
                        .font(PsyFont.mono(11))
                        .foregroundStyle(psyColors.teal)
                }
            }

            Spacer().frame(height: 6)

            MoneyText(amountMinor: vm.netMinor, currency: vm.currency)
                .font(PsyFont.display(40))
                .foregroundStyle(Color.white)

            Spacer().frame(height: 16)

            HStack(spacing: 12) {
                statTile(title: "Thu nhập", icon: "arrow-up-right",
                         amount: vm.incomeMinor, prefix: "+", tint: psyColors.incomeTint)
                statTile(title: "Chi tiêu", icon: "arrow-down-right",
                         amount: vm.expenseMinor, prefix: "-", tint: psyColors.expenseTint)
            }
        }
    }

    private func statTile(title: String, icon: String, amount: Int64, prefix: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                LucideIcon(name: icon, size: 14, tint: tint)
                Text(title)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(argb: 0xFFAEC4DA))
            }
            MoneyText(amountMinor: amount, currency: vm.currency, prefix: prefix)
                .font(PsyFont.bodyLarge.weight(.semibold))
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.white.opacity(0.08))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.12), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Section header

    private func sectionHeader(_ label: String) -> some View {
        Text(label)
            .font(PsyFont.titleMedium)
            .foregroundStyle(psyColors.text2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 4)
    }

    // MARK: - Transaction row

    private func txRow(_ row: TxRow) -> some View {
        let name = row.type == .transfer
            ? "\(row.accountName) → \(row.toAccountName ?? "—")"
            : row.title
        let metaLeading = row.type == .transfer ? "Chuyển khoản" : row.title
        let meta = row.timeLabel.isEmpty ? metaLeading : "\(metaLeading) · \(row.timeLabel)"
        return Button {
            editTxId = row.id
        } label: {
            TransactionRowView(
                iconName: "receipt",
                iconTint: psyColors.blue,
                iconBg: psyColors.blue.opacity(0.14),
                name: name,
                meta: meta,
                amount: signedAmount(row),
                isIncome: row.type == .income,
                account: row.accountName
            )
        }
        .buttonStyle(.plain)
    }

    private func signedAmount(_ row: TxRow) -> String {
        let prefix: String
        switch row.type {
        case .income: prefix = "+"
        case .expense: prefix = "-"
        case .transfer: prefix = ""
        }
        return prefix + vm.currency.format(row.amountMinor)
    }

    // MARK: - FAB

    private var fab: some View {
        Button { showAdd = true } label: {
            RoundedRectangle(cornerRadius: 18)
                .fill(psyColors.blue)
                .frame(width: 60, height: 60)
                .overlay(LucideIcon(name: "plus", size: 26, tint: .white))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Thêm giao dịch")
        .padding(22)
    }
}
