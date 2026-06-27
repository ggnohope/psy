import SwiftUI
import PsyCore

/// Manage categories — 2-level: groups (with color/type) each containing leaf categories.
/// Mirrors ManageCategoriesScreen.kt: Chi/Thu tabs, group cards with child rows, add group (FAB),
/// add leaf per group, plus group + leaf editor sheets.
struct ManageCategoriesView: View {
    @Environment(\.psyColors) private var psyColors
    @State private var vm: ManageCategoriesViewModel
    @State private var pendingDeleteGroup: CategoryGroup?
    @State private var pendingDeleteLeaf: PsyCore.Category?

    init(container: AppContainer) {
        _vm = State(initialValue: ManageCategoriesViewModel(container: container))
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            psyColors.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    SegmentedControl(options: ["Chi", "Thu"],
                                     selectedIndex: vm.type == .expense ? 0 : 1,
                                     onSelect: { vm.selectTab($0 == 0 ? .expense : .income) })

                    if vm.groups.isEmpty {
                        EmptyStateView(iconName: "list", title: "Chưa có danh mục",
                                       caption: "Thêm nhóm danh mục đầu tiên.")
                            .padding(.top, 40)
                    } else {
                        ForEach(vm.groups) { gwl in
                            groupCard(gwl)
                        }
                    }
                    Spacer(minLength: 80)
                }
                .padding(.horizontal, 22)
                .padding(.top, 12)
            }

            // FAB → add group
            Button { vm.startAddGroup() } label: {
                LucideIcon(name: "plus", size: 26, tint: .white)
                    .frame(width: 56, height: 56)
                    .background(psyColors.blue)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: psyColors.blue.opacity(0.3), radius: 12, y: 6)
            }
            .padding(22)
        }
        .navigationTitle("Quản lý danh mục")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $vm.groupEditorOpen) { groupEditorSheet }
        .sheet(isPresented: $vm.leafEditorOpen) { leafEditorSheet }
        // Confirm group deletion — mirrors Android's cascade-warning AlertDialog.
        .confirmationDialog(
            "Xoá nhóm",
            isPresented: Binding(get: { pendingDeleteGroup != nil },
                                 set: { if !$0 { pendingDeleteGroup = nil } }),
            titleVisibility: .visible,
            presenting: pendingDeleteGroup
        ) { group in
            Button("Xoá", role: .destructive) {
                vm.deleteGroup(group)
                pendingDeleteGroup = nil
            }
            Button("Huỷ", role: .cancel) { pendingDeleteGroup = nil }
        } message: { group in
            Text("Xoá nhóm «\(group.name)» và tất cả mục con của nó? "
                + "Các giao dịch cũ thuộc nhóm này sẽ mất danh mục.")
        }
        // Confirm leaf deletion.
        .confirmationDialog(
            "Xoá mục",
            isPresented: Binding(get: { pendingDeleteLeaf != nil },
                                 set: { if !$0 { pendingDeleteLeaf = nil } }),
            titleVisibility: .visible,
            presenting: pendingDeleteLeaf
        ) { leaf in
            Button("Xoá", role: .destructive) {
                vm.deleteLeaf(leaf)
                pendingDeleteLeaf = nil
            }
            Button("Huỷ", role: .cancel) { pendingDeleteLeaf = nil }
        } message: { leaf in
            Text("Xoá mục «\(leaf.name)»?")
        }
    }

    // MARK: - Group card

    private func groupCard(_ gwl: GroupWithLeaves) -> some View {
        let g = gwl.group
        return VStack(spacing: 0) {
            // Header
            HStack(spacing: 12) {
                IconTile(iconName: g.icon, tint: Color(argb: g.color), bg: Color(argb: g.color).opacity(0.14), size: 42)
                Text(g.name).font(PsyFont.bodyLarge.weight(.semibold)).foregroundStyle(psyColors.text)
                Spacer()
                iconButton("pencil", tint: psyColors.text3) { vm.startEditGroup(g) }
                iconButton("trash-2", tint: psyColors.red) { pendingDeleteGroup = g }
            }
            .padding(14)

            Rectangle().fill(psyColors.hair).frame(height: 1)

            // Leaf rows
            ForEach(gwl.leaves) { leaf in
                HStack(spacing: 10) {
                    LucideIcon(name: leaf.icon, size: 18, tint: psyColors.text2)
                    Text(leaf.name).font(PsyFont.bodyMedium).foregroundStyle(psyColors.text)
                    Spacer()
                    iconButton("pencil", tint: psyColors.text3) { vm.startEditLeaf(leaf) }
                    iconButton("trash-2", tint: psyColors.red) { pendingDeleteLeaf = leaf }
                }
                .padding(.horizontal, 14).padding(.vertical, 10)
            }

            // Add leaf
            Button { vm.startAddLeaf(groupId: g.id) } label: {
                HStack(spacing: 6) {
                    LucideIcon(name: "plus", size: 16, tint: psyColors.blue)
                    Text("Thêm mục").font(PsyFont.bodyMedium.weight(.semibold)).foregroundStyle(psyColors.blue)
                    Spacer()
                }
                .padding(.horizontal, 14).padding(.vertical, 12)
            }
            .buttonStyle(.plain)
        }
        .background(psyColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(psyColors.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func iconButton(_ name: String, tint: Color, action: @escaping () -> Void) -> some View {
        // Icon visual stays 18; the tappable frame is 44×44 to meet the a11y minimum.
        Button(action: action) {
            LucideIcon(name: name, size: 18, tint: tint)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Editors

    private var groupEditorSheet: some View {
        editorSheet(
            title: vm.editingGroupId == nil ? "Thêm nhóm" : "Sửa nhóm",
            name: Binding(get: { vm.groupDraftName }, set: { vm.groupDraftName = $0 }),
            icon: Binding(get: { vm.groupDraftIcon }, set: { vm.groupDraftIcon = $0 }),
            color: Binding(get: { vm.groupDraftColor }, set: { vm.groupDraftColor = $0 }),
            showColor: true,
            canSave: vm.canSaveGroup,
            onSave: vm.saveGroup,
            onCancel: { vm.groupEditorOpen = false }
        )
    }

    private var leafEditorSheet: some View {
        editorSheet(
            title: vm.editingLeafId == nil ? "Thêm mục" : "Sửa mục",
            name: Binding(get: { vm.leafDraftName }, set: { vm.leafDraftName = $0 }),
            icon: Binding(get: { vm.leafDraftIcon }, set: { vm.leafDraftIcon = $0 }),
            color: .constant(0),
            showColor: false,
            canSave: vm.canSaveLeaf,
            onSave: vm.saveLeaf,
            onCancel: { vm.leafEditorOpen = false }
        )
    }

    /// Preview tint for the editor's IconTile. Groups use their own color;
    /// leaves (no own color) fall back to a fixed blue accent argb.
    private func previewColor(showColor: Bool, color: Int64) -> Int64 {
        showColor ? color : 0xFF0A7CF6
    }

    @ViewBuilder
    private func editorSheet(title: String, name: Binding<String>, icon: Binding<String>,
                             color: Binding<Int64>, showColor: Bool, canSave: Bool,
                             onSave: @escaping () -> Void, onCancel: @escaping () -> Void) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                    Text(title)
                        .font(PsyFont.titleLarge)
                        .foregroundStyle(psyColors.text)

                    HStack(spacing: 13) {
                        IconTile(iconName: icon.wrappedValue,
                                 tint: Color(argb: previewColor(showColor: showColor, color: color.wrappedValue)),
                                 bg: Color(argb: previewColor(showColor: showColor, color: color.wrappedValue)).opacity(0.14),
                                 size: 48)
                        Text(name.wrappedValue.isEmpty ? "Tên" : name.wrappedValue)
                            .font(PsyFont.bodyLarge.weight(.semibold))
                            .foregroundStyle(name.wrappedValue.isEmpty ? psyColors.text3 : psyColors.text)
                        Spacer()
                    }

                    EyebrowLabel(text: "Tên")
                    PsyTextField("Tên", text: name)
                    EyebrowLabel(text: "Biểu tượng")
                    IconPicker(selected: icon.wrappedValue) { icon.wrappedValue = $0 }
                    if showColor {
                        EyebrowLabel(text: "Màu sắc")
                        ColorPicker(selected: color.wrappedValue) { color.wrappedValue = $0 }
                    }

                    HStack(spacing: 12) {
                        Button { onCancel() } label: {
                            Text("Huỷ")
                                .font(PsyFont.bodyLarge)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 13)
                                .foregroundStyle(psyColors.text2)
                                .background(psyColors.surface)
                                .overlay(RoundedRectangle(cornerRadius: PsyRadius.button).stroke(psyColors.hair, lineWidth: 1))
                                .clipShape(RoundedRectangle(cornerRadius: PsyRadius.button))
                        }
                        Button { onSave() } label: {
                            Text("Lưu")
                                .font(PsyFont.bodyLarge.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 13)
                                .foregroundStyle(.white)
                                .background(psyColors.blue.opacity(canSave ? 1 : 0.4))
                                .clipShape(RoundedRectangle(cornerRadius: PsyRadius.button))
                        }
                        .disabled(!canSave)
                    }
                    .padding(.top, 4)
            }
            .padding(22)
        }
        .background(psyColors.bg)
    }
}
