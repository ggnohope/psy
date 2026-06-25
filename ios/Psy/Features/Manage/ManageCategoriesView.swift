import SwiftUI
import PsyCore

/// Manage categories: type tab + list + add/edit editor sheet + swipe-to-delete.
/// Ports ManageCategoriesScreen.kt.
struct ManageCategoriesView: View {
    @Environment(\.psyColors) private var psyColors
    @State private var vm: ManageCategoriesViewModel
    @State private var editorOpen = false

    init(container: AppContainer) {
        _vm = State(initialValue: ManageCategoriesViewModel(container: container))
    }

    var body: some View {
        VStack(spacing: 0) {
            SegmentedControl(
                options: ["Chi", "Thu"],
                selectedIndex: vm.type == .expense ? 0 : 1,
                onSelect: { vm.selectTab($0 == 0 ? .expense : .income) }
            )
            .padding(.horizontal, 22)
            .padding(.vertical, 12)
            if vm.items.isEmpty {
                Spacer()
                EmptyStateView(
                    iconName: "list",
                    title: "Chưa có danh mục",
                    caption: "Thêm nhóm danh mục đầu tiên."
                )
                Spacer()
            } else {
                list
            }
        }
        .background(psyColors.bg)
        .navigationTitle("Quản lý danh mục")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    vm.startAdd()
                    editorOpen = true
                } label: {
                    LucideIcon(name: "plus", size: 20, tint: psyColors.blue)
                }
            }
        }
        .sheet(isPresented: $editorOpen) {
            CategoryEditorSheet(vm: vm) { editorOpen = false }
        }
    }

    private var list: some View {
        List {
            ForEach(vm.items) { group in
                CategoryRow(group: group)
                    .listRowBackground(psyColors.bg)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 5, leading: 22, bottom: 5, trailing: 22))
                    .contentShape(Rectangle())
                    .onTapGesture {
                        vm.startEdit(group)
                        editorOpen = true
                    }
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            vm.delete(group)
                        } label: {
                            Label("Xoá", systemImage: "trash")
                        }
                    }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
}

private struct CategoryRow: View {
    @Environment(\.psyColors) private var psyColors
    let group: CategoryGroup

    var body: some View {
        HStack(spacing: 13) {
            IconTile(
                iconName: group.icon,
                tint: Color(argb: group.color),
                bg: Color(argb: group.color).opacity(0.14),
                size: 44
            )
            Text(group.name)
                .font(PsyFont.bodyLarge.weight(.semibold))
                .foregroundStyle(psyColors.text)
            Spacer()
            LucideIcon(name: "chevron-right", size: 18, tint: psyColors.text3)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(psyColors.surface)
        .overlay(RoundedRectangle(cornerRadius: PsyRadius.card).stroke(psyColors.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: PsyRadius.card))
    }
}

private struct CategoryEditorSheet: View {
    @Environment(\.psyColors) private var psyColors
    @Bindable var vm: ManageCategoriesViewModel
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TextField("Tên danh mục", text: $vm.draftName)
                        .textFieldStyle(.roundedBorder)

                    Text("Biểu tượng").font(PsyFont.bodyMedium).foregroundStyle(psyColors.text2)
                    IconPicker(selected: vm.draftIcon) { vm.draftIcon = $0 }

                    Text("Màu sắc").font(PsyFont.bodyMedium).foregroundStyle(psyColors.text2)
                    ColorPicker(selected: vm.draftColor) { vm.draftColor = $0 }
                }
                .padding(.horizontal, 22)
                .padding(.vertical, 16)
            }
            .background(psyColors.bg)
            .navigationTitle(vm.editingId == nil ? "Thêm danh mục" : "Sửa danh mục")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Huỷ") { onDone() }
                        .foregroundStyle(psyColors.text2)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") { vm.save(); onDone() }
                        .foregroundStyle(psyColors.blue)
                        .fontWeight(.semibold)
                        .disabled(vm.draftName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
