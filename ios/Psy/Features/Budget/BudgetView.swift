import SwiftUI
import PsyCore

/// Budget screen: month selector, total budget card (or set-up button), per-category
/// budget list with progress bars, an add-category action, and a mode-aware editor sheet.
/// Ports `BudgetScreen.kt` with Candy Pop styling.
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
                VStack(alignment: .leading, spacing: 12) {
                    MonthSelector(label: vm.monthLabel, onPrev: vm.prevMonth, onNext: vm.nextMonth)
                        .frame(maxWidth: .infinity)

                    totalSection

                    Text("Ngân sách theo danh mục")
                        .font(PsyFont.titleMedium)
                        .fontWeight(.semibold)
                        .foregroundStyle(psyColors.onSurface)

                    if vm.categoryBudgets.isEmpty && vm.total == nil {
                        Text("Chưa có ngân sách nào. Hãy thêm ngân sách để theo dõi chi tiêu!")
                            .font(PsyFont.bodyMedium)
                            .foregroundStyle(psyColors.onSurface.opacity(0.6))
                    }

                    ForEach(vm.categoryBudgets) { item in
                        categoryCard(item)
                    }

                    if !vm.availableGroups.isEmpty || !vm.categoryBudgets.isEmpty {
                        Button(action: vm.startAddCategory) {
                            Text("＋ Thêm ngân sách danh mục")
                                .font(PsyFont.bodyMedium)
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(
                                    RoundedRectangle(cornerRadius: CandyShape.medium)
                                        .stroke(psyColors.primary.opacity(vm.availableGroups.isEmpty ? 0.3 : 1), lineWidth: 1.5)
                                )
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(psyColors.primary.opacity(vm.availableGroups.isEmpty ? 0.4 : 1))
                        .disabled(vm.availableGroups.isEmpty)
                    }

                    Spacer(minLength: 16)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .background(psyColors.background.ignoresSafeArea())
            .navigationTitle("Ngân sách")
            .sheet(isPresented: $vm.editorOpen) { editorSheet }
        }
    }

    // MARK: - Total budget

    @ViewBuilder
    private var totalSection: some View {
        if let total = vm.total {
            Button { vm.startEdit(total.budget) } label: {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Ngân sách tổng")
                        .font(PsyFont.titleMedium)
                        .fontWeight(.bold)
                        .foregroundStyle(psyColors.onSurface)
                    BudgetProgress(spentMinor: total.spentMinor, limitMinor: total.limitMinor)
                    Text("Đã chi \(fmt(total.spentMinor)) / \(fmt(total.limitMinor)) (\(Int(total.percent * 100))%)")
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(psyColors.onSurface.opacity(0.7))
                    if total.spentMinor > total.limitMinor {
                        Text("⚠️ Vượt \(fmt(total.spentMinor - total.limitMinor))")
                            .font(PsyFont.labelSmall)
                            .fontWeight(.semibold)
                            .foregroundStyle(CandyColor.pinkDeep)
                    } else {
                        Text("Còn lại \(fmt(total.limitMinor - total.spentMinor))")
                            .font(PsyFont.labelSmall)
                            .foregroundStyle(psyColors.onSurface.opacity(0.7))
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(psyColors.surface)
                .clipShape(RoundedRectangle(cornerRadius: CandyShape.medium))
            }
            .buttonStyle(.plain)
        } else {
            Button(action: vm.startAddTotal) {
                Text("＋ Đặt ngân sách tổng")
                    .font(PsyFont.bodyMedium)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: CandyShape.medium)
                            .stroke(psyColors.primary, lineWidth: 1.5)
                    )
            }
            .buttonStyle(.plain)
            .foregroundStyle(psyColors.primary)
        }
    }

    // MARK: - Category card

    private func categoryCard(_ item: CategoryBudgetItem) -> some View {
        Button { vm.startEdit(item.budget) } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    LucideIcon(name: item.group?.icon ?? "circle-dollar-sign", size: 20,
                               tint: Color(argb: item.group?.color ?? 0xFF0A7CF6))
                    Text(item.group?.name ?? "Danh mục")
                        .font(PsyFont.titleMedium)
                        .fontWeight(.medium)
                        .foregroundStyle(psyColors.onSurface)
                }
                BudgetProgress(spentMinor: item.spentMinor, limitMinor: item.limitMinor)
                Text("Đã chi \(fmt(item.spentMinor)) / \(fmt(item.limitMinor)) (\(Int(item.percent * 100))%)")
                    .font(PsyFont.labelSmall)
                    .foregroundStyle(psyColors.onSurface.opacity(0.7))
                if item.spentMinor > item.limitMinor {
                    Text("⚠️ Vượt \(fmt(item.spentMinor - item.limitMinor))")
                        .font(PsyFont.labelSmall)
                        .fontWeight(.semibold)
                        .foregroundStyle(CandyColor.pinkDeep)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(psyColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: CandyShape.medium))
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
                            .foregroundStyle(psyColors.onSurface.opacity(0.7))
                        FlexibleChips(groups: vm.availableGroups,
                                      selectedId: vm.editorGroupId) { vm.selectEditorGroup($0) }
                    }

                    Text("Số tiền (\(vm.currency.symbol))")
                        .font(PsyFont.bodyMedium)
                        .foregroundStyle(psyColors.onSurface.opacity(0.7))
                    TextField("0", text: Binding(
                        get: { vm.draftAmountText },
                        set: { vm.onAmountChange($0) }
                    ))
                    .keyboardType(.numberPad)
                    .font(PsyFont.headlineMedium)
                    .foregroundStyle(psyColors.onSurface)
                    .padding(12)
                    .background(psyColors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: CandyShape.small))

                    Button(action: vm.save) {
                        Text("Lưu")
                            .font(PsyFont.titleMedium)
                            .fontWeight(.bold)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(vm.canSave ? psyColors.primary : psyColors.onSurface.opacity(0.2))
                            .clipShape(RoundedRectangle(cornerRadius: CandyShape.medium))
                    }
                    .buttonStyle(.plain)
                    .disabled(!vm.canSave)

                    if vm.isEditing {
                        Button(action: vm.remove) {
                            Text("Xoá ngân sách này")
                                .font(PsyFont.bodyMedium)
                                .fontWeight(.semibold)
                                .foregroundStyle(CandyColor.pinkDeep)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        }
                        .buttonStyle(.plain)
                    }

                    Spacer(minLength: 0)
                }
                .padding(24)
            }
            .background(psyColors.background.ignoresSafeArea())
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
                                   tint: selected ? .white : psyColors.onSurface.opacity(0.8))
                        Text(group.name)
                            .font(PsyFont.bodyMedium)
                            .foregroundStyle(selected ? .white : psyColors.onSurface.opacity(0.8))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(selected ? psyColors.primary : psyColors.onSurface.opacity(0.08))
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
