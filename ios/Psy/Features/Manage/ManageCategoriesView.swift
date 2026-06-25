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
            tabToggle
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            if vm.items.isEmpty {
                emptyState
            } else {
                list
            }
        }
        .background(psyColors.background)
        .navigationTitle("Quản lý danh mục")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    vm.startAdd()
                    editorOpen = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $editorOpen) {
            CategoryEditorSheet(vm: vm) { editorOpen = false }
        }
    }

    private var tabToggle: some View {
        HStack(spacing: 0) {
            tab(.expense, "Chi")
            tab(.income, "Thu")
        }
        .background(psyColors.onSurface.opacity(0.06))
        .clipShape(Capsule())
    }

    private func tab(_ type: CategoryType, _ label: String) -> some View {
        let isSelected = vm.type == type
        return Text(label)
            .font(PsyFont.bodyMedium)
            .fontWeight(isSelected ? .bold : .regular)
            .foregroundStyle(isSelected ? Color.white : psyColors.onSurface.opacity(0.6))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(isSelected ? psyColors.primary : Color.clear)
            .clipShape(Capsule())
            .contentShape(Rectangle())
            .onTapGesture { vm.selectTab(type) }
    }

    private var emptyState: some View {
        VStack {
            Spacer()
            Text("Chưa có danh mục nào.\nNhấn + để thêm mới.")
                .font(PsyFont.bodyMedium)
                .multilineTextAlignment(.center)
                .foregroundStyle(psyColors.onSurface.opacity(0.6))
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var list: some View {
        List {
            ForEach(vm.items) { group in
                CategoryRow(group: group)
                    .listRowBackground(psyColors.background)
                    .listRowSeparator(.hidden)
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
        HStack(spacing: 12) {
            LucideIcon(name: group.icon, size: 20, tint: Color(argb: group.color))
                .frame(width: 40, height: 40)
                .background(RoundedRectangle(cornerRadius: 11).fill(Color(argb: group.color).opacity(0.14)))
            Text(group.name)
                .font(PsyFont.titleMedium)
                .foregroundStyle(psyColors.onSurface)
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: CandyShape.small).fill(psyColors.surface))
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

                    Text("Biểu tượng").font(PsyFont.bodyMedium).foregroundStyle(psyColors.onSurface.opacity(0.7))
                    IconPicker(selected: vm.draftIcon) { vm.draftIcon = $0 }

                    Text("Màu sắc").font(PsyFont.bodyMedium).foregroundStyle(psyColors.onSurface.opacity(0.7))
                    ColorPicker(selected: vm.draftColor) { vm.draftColor = $0 }
                }
                .padding(16)
            }
            .background(psyColors.background)
            .navigationTitle(vm.editingId == nil ? "Thêm danh mục" : "Sửa danh mục")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Huỷ") { onDone() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") { vm.save(); onDone() }
                        .disabled(vm.draftName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
