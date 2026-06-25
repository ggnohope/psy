import SwiftUI
import PsyCore

/// Budget screen: month switcher, total budget card (or set-up button), per-group
/// budget list with progress bars, an add-category action, and a mode-aware editor sheet.
/// Ports `BudgetScreen.kt` with HostGuardIQ styling.
struct BudgetView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var psyColors
    @State private var vm: BudgetViewModel

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: BudgetViewModel(container: container))
    }

    private func fmt(_ minor: Int64) -> String { vm.currency.format(minor) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        EyebrowLabel(text: "Hạn mức")
                        Text("Ngân sách")
                            .font(PsyFont.headlineMedium)
                            .foregroundStyle(psyColors.text)
                    }

                    MonthSwitcher(label: vm.monthLabel, onPrev: vm.prevMonth, onNext: vm.nextMonth)

                    totalSection

                    if vm.categoryBudgets.isEmpty && vm.total == nil {
                        EmptyStateView(iconName: "wallet",
                                       title: "Chưa có ngân sách",
                                       caption: "Đặt hạn mức cho nhóm chi tiêu.")
                    }

                    ForEach(vm.categoryBudgets) { item in
                        categoryCard(item)
                    }

                    if !vm.availableGroups.isEmpty || !vm.categoryBudgets.isEmpty {
                        actionButton(title: "Thêm ngân sách danh mục",
                                     enabled: !vm.availableGroups.isEmpty,
                                     action: vm.startAddCategory)
                    }

                    Spacer(minLength: 8)
                }
                .padding(22)
            }
            .background(psyColors.bg.ignoresSafeArea())
            .navigationTitle("Ngân sách")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $vm.editorOpen) { editorSheet }
        }
    }

    // MARK: - Reusable add/set-up button

    private func actionButton(title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                LucideIcon(name: "plus", size: 18, tint: psyColors.blue)
                Text(title)
                    .font(PsyFont.labelLarge)
            }
            .foregroundStyle(psyColors.blue)
            .opacity(enabled ? 1 : 0.4)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(psyColors.blueSoft.opacity(enabled ? 1 : 0.5))
            .overlay(
                RoundedRectangle(cornerRadius: PsyRadius.card)
                    .stroke(psyColors.blue.opacity(enabled ? 1 : 0.3), lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: PsyRadius.card))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    // MARK: - Total budget

    @ViewBuilder
    private var totalSection: some View {
        if let total = vm.total {
            let over = total.spentMinor > total.limitMinor
            Button { vm.startEdit(total.budget) } label: {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(alignment: .firstTextBaseline) {
                        Text("Ngân sách tổng")
                            .font(PsyFont.titleMedium)
                            .fontWeight(.semibold)
                            .foregroundStyle(psyColors.text)
                        Spacer()
                        percentPill(total.percent, over: over)
                    }
                    BudgetProgress(spentMinor: total.spentMinor, limitMinor: total.limitMinor)
                    Text("Đã chi \(fmt(total.spentMinor)) / \(fmt(total.limitMinor))")
                        .font(PsyFont.bodyMedium)
                        .foregroundStyle(psyColors.text3)
                    if over {
                        HStack(spacing: 6) {
                            LucideIcon(name: "triangle-alert", size: 14, tint: psyColors.red)
                            Text("Vượt \(fmt(total.spentMinor - total.limitMinor))")
                                .font(PsyFont.bodyMedium)
                                .fontWeight(.semibold)
                                .foregroundStyle(psyColors.red)
                        }
                    } else {
                        Text("Còn lại \(fmt(total.limitMinor - total.spentMinor))")
                            .font(PsyFont.bodyMedium)
                            .foregroundStyle(psyColors.text3)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(psyColors.surface)
                .overlay(alignment: .leading) {
                    if over {
                        Rectangle().fill(psyColors.red).frame(width: 3)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
        } else {
            actionButtonSetup
        }
    }

    private var actionButtonSetup: some View {
        Button(action: vm.startAddTotal) {
            HStack(spacing: 8) {
                LucideIcon(name: "plus", size: 18, tint: psyColors.blue)
                Text("Đặt ngân sách tổng")
                    .font(PsyFont.labelLarge)
            }
            .foregroundStyle(psyColors.blue)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(psyColors.blueSoft)
            .overlay(
                RoundedRectangle(cornerRadius: PsyRadius.card)
                    .stroke(psyColors.blue, lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: PsyRadius.card))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Percent pill

    private func percentPill(_ percent: Double, over: Bool) -> some View {
        Text("\(Int(percent * 100))%")
            .font(PsyFont.mono(11))
            .foregroundStyle(over ? psyColors.red : psyColors.blue)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(over ? psyColors.redSoft : psyColors.blueSoft)
            .clipShape(Capsule())
    }

    // MARK: - Category card

    private func categoryCard(_ item: CategoryBudgetItem) -> some View {
        let over = item.spentMinor > item.limitMinor
        let argb = item.group?.color ?? 0xFF0A7CF6
        return Button { vm.startEdit(item.budget) } label: {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    IconTile(iconName: item.group?.icon ?? "circle-dollar-sign",
                             tint: Color(argb: argb),
                             bg: Color(argb: argb).opacity(0.14),
                             size: 36)
                    Text(item.group?.name ?? "Danh mục")
                        .font(PsyFont.bodyLarge)
                        .fontWeight(.semibold)
                        .foregroundStyle(psyColors.text)
                    Spacer()
                    percentPill(item.percent, over: over)
                }
                BudgetProgress(spentMinor: item.spentMinor, limitMinor: item.limitMinor)
                Text("Đã chi \(fmt(item.spentMinor)) / \(fmt(item.limitMinor))")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.text3)
                if over {
                    HStack(spacing: 6) {
                        LucideIcon(name: "triangle-alert", size: 14, tint: psyColors.red)
                        Text("Vượt \(fmt(item.spentMinor - item.limitMinor))")
                            .font(PsyFont.bodyMedium)
                            .fontWeight(.semibold)
                            .foregroundStyle(psyColors.red)
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(psyColors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(psyColors.hair, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Editor sheet

    private var editorSheet: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if vm.editorMode == .category && !vm.isEditing {
                        Text("Chọn danh mục")
                            .font(PsyFont.bodyMedium)
                            .foregroundStyle(psyColors.text2)
                        FlexibleChips(groups: vm.availableGroups,
                                      selectedId: vm.editorGroupId) { vm.selectEditorGroup($0) }
                    }

                    Text("Số tiền (\(vm.currency.symbol))")
                        .font(PsyFont.bodyMedium)
                        .foregroundStyle(psyColors.text2)
                    TextField("0", text: Binding(
                        get: { vm.draftAmountText },
                        set: { vm.onAmountChange($0) }
                    ))
                    .keyboardType(.numberPad)
                    .font(PsyFont.display(24))
                    .foregroundStyle(psyColors.text)
                    .padding(14)
                    .background(psyColors.surface)
                    .overlay(
                        RoundedRectangle(cornerRadius: PsyRadius.button)
                            .stroke(psyColors.hair, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: PsyRadius.button))

                    Button(action: vm.save) {
                        Text("Lưu")
                            .font(PsyFont.titleMedium)
                            .fontWeight(.bold)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(vm.canSave ? psyColors.blue : psyColors.text3.opacity(0.25))
                            .clipShape(RoundedRectangle(cornerRadius: PsyRadius.card))
                    }
                    .buttonStyle(.plain)
                    .disabled(!vm.canSave)

                    if vm.isEditing {
                        Button(action: vm.remove) {
                            HStack(spacing: 6) {
                                LucideIcon(name: "trash-2", size: 16, tint: psyColors.red)
                                Text("Xoá ngân sách này")
                                    .font(PsyFont.labelLarge)
                            }
                            .foregroundStyle(psyColors.red)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                        }
                        .buttonStyle(.plain)
                    }

                    Spacer(minLength: 0)
                }
                .padding(22)
            }
            .background(psyColors.bg.ignoresSafeArea())
            .navigationTitle(vm.editorMode == .total ? "Ngân sách tổng" : "Ngân sách danh mục")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Huỷ", action: vm.closeEditor)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

/// Simple wrapping chip row for the category picker (FlowRow analog).
private struct FlexibleChips: View {
    let groups: [CategoryGroup]
    let selectedId: Int64?
    let onSelect: (Int64) -> Void
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(groups) { group in
                let selected = selectedId == group.id
                Button { onSelect(group.id) } label: {
                    HStack(spacing: 6) {
                        LucideIcon(name: group.icon, size: 15,
                                   tint: selected ? .white : psyColors.text2)
                        Text(group.name)
                            .font(PsyFont.bodyMedium)
                            .foregroundStyle(selected ? .white : psyColors.text2)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(selected ? psyColors.blue : psyColors.sunken)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// Minimal flow layout for wrapping chips.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0; y += rowHeight + spacing; rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxWidth == .infinity ? x : maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let maxWidth = bounds.width
        var x: CGFloat = bounds.minX, y: CGFloat = bounds.minY, rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.minX + maxWidth, x > bounds.minX {
                x = bounds.minX; y += rowHeight + spacing; rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
