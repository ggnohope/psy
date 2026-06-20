import SwiftUI
import PsyCore

/// Home screen: month summary card + grouped transaction list. Ports HomeScreen.kt.
/// Canonical example of a ViewModel-backed feature screen.
struct HomeView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var psyColors
    @State private var vm: HomeViewModel

    @State private var showAdd = false
    @State private var editTxId: Int64?

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: HomeViewModel(container: container))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    balanceCard

                    if vm.days.isEmpty && !vm.loading {
                        emptyState
                    } else {
                        ForEach(vm.days) { dayGroup in
                            sectionHeader(dayGroup.dateLabel)
                            ForEach(dayGroup.items) { row in
                                txRow(row)
                            }
                        }
                    }

                    Spacer(minLength: 80)
                }
            }
            .background(psyColors.background.ignoresSafeArea())
            .navigationTitle("Psy")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showAdd = true } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Thêm giao dịch")
                }
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        NavigationLink {
                            ManageAccountsView(container: container)
                        } label: {
                            Label("Quản lý tài khoản", systemImage: "creditcard")
                        }
                        NavigationLink {
                            ManageCategoriesView(container: container)
                        } label: {
                            Label("Quản lý danh mục", systemImage: "square.grid.2x2")
                        }
                    } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("Cài đặt")
                }
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

    // MARK: - Balance card

    private var balanceCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(vm.monthLabel)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.85))
            Spacer().frame(height: 4)
            MoneyText(amountMinor: vm.netMinor, currency: vm.currency)
                .font(.system(size: 32, weight: .bold))
                .foregroundStyle(Color.white)
            Spacer().frame(height: 12)
            HStack(spacing: 24) {
                summaryItem(title: "Thu nhập", prefix: "+", amount: vm.incomeMinor)
                summaryItem(title: "Chi tiêu", prefix: "-", amount: vm.expenseMinor)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: [CandyColor.violet, CandyColor.sky],
                startPoint: .leading, endPoint: .trailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: CandyShape.large))
        .padding(16)
    }

    private func summaryItem(title: String, prefix: String, amount: Int64) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.75))
            MoneyText(amountMinor: amount, currency: vm.currency, prefix: prefix)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.white)
        }
    }

    // MARK: - Section header

    private func sectionHeader(_ label: String) -> some View {
        Text(label)
            .font(PsyFont.titleMedium)
            .foregroundStyle(psyColors.onSurface.opacity(0.6))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
    }

    // MARK: - Transaction row

    private func txRow(_ row: TxRow) -> some View {
        Button {
            editTxId = row.id
        } label: {
            HStack(spacing: 0) {
                // Tinted emoji icon circle
                ZStack {
                    Circle().fill(iconBackground(row.type))
                    Text(row.icon).font(.system(size: 22))
                }
                .frame(width: 44, height: 44)

                Spacer().frame(width: 12)

                // Title + note + account (or "from → to" for transfer)
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

                // Photo thumbnail
                if let uri = row.photoUri, let img = UIImage(contentsOfFile: uri) {
                    Spacer().frame(width: 8)
                    Image(uiImage: img)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 30, height: 30)
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                }

                Spacer().frame(width: 8)

                // Trailing amount with sign + color
                MoneyText(amountMinor: row.amountMinor, currency: vm.currency, prefix: amountSign(row.type))
                    .font(PsyFont.bodyMedium)
                    .fontWeight(.bold)
                    .foregroundStyle(amountColor(row.type))
            }
            .padding(12)
            .background(psyColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: CandyShape.medium))
            .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
        }
        .buttonStyle(.plain)
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

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: 0) {
            Text("🌸").font(.system(size: 48))
            Spacer().frame(height: 12)
            Text("Chưa có giao dịch nào tháng này")
                .font(PsyFont.titleMedium)
                .foregroundStyle(psyColors.onSurface.opacity(0.5))
            Spacer().frame(height: 4)
            Text("Nhấn + để thêm giao dịch đầu tiên!")
                .font(PsyFont.bodyMedium)
                .foregroundStyle(psyColors.onSurface.opacity(0.35))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 64)
    }
}
