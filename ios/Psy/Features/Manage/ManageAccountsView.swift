import SwiftUI
import PsyCore

/// Manage accounts: list + add/edit editor sheet. Ports ManageAccountsScreen.kt.
struct ManageAccountsView: View {
    @Environment(\.psyColors) private var psyColors
    @State private var vm: ManageAccountsViewModel
    @State private var editorOpen = false

    init(container: AppContainer) {
        _vm = State(initialValue: ManageAccountsViewModel(container: container))
    }

    var body: some View {
        Group {
            if vm.items.isEmpty {
                emptyState
            } else {
                list
            }
        }
        .background(psyColors.background)
        .navigationTitle("Quản lý tài khoản")
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
            AccountEditorSheet(vm: vm) { editorOpen = false }
        }
    }

    private var emptyState: some View {
        VStack {
            Spacer()
            Text("Chưa có tài khoản nào.\nNhấn + để thêm mới.")
                .font(PsyFont.bodyMedium)
                .multilineTextAlignment(.center)
                .foregroundStyle(psyColors.onSurface.opacity(0.6))
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var list: some View {
        List {
            ForEach(vm.items) { account in
                AccountRow(account: account)
                    .listRowBackground(psyColors.background)
                    .listRowSeparator(.hidden)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        vm.startEdit(account)
                        editorOpen = true
                    }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
}

private struct AccountRow: View {
    @Environment(\.psyColors) private var psyColors
    let account: Account

    var body: some View {
        HStack(spacing: 12) {
            Text(account.icon)
                .font(.system(size: 22))
                .frame(width: 44, height: 44)
                .background(Circle().fill(Color(argb: account.color).opacity(0.25)))
            VStack(alignment: .leading, spacing: 2) {
                Text(account.name)
                    .font(PsyFont.titleMedium)
                    .foregroundStyle(psyColors.onSurface)
                Text(account.type.vietnamese)
                    .font(PsyFont.labelSmall)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))
            }
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: CandyShape.small).fill(psyColors.onSurface.opacity(0.05)))
    }
}

private struct AccountEditorSheet: View {
    @Environment(\.psyColors) private var psyColors
    @Bindable var vm: ManageAccountsViewModel
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TextField("Tên tài khoản", text: $vm.draftName)
                        .textFieldStyle(.roundedBorder)

                    Text("Loại tài khoản").font(PsyFont.bodyMedium).foregroundStyle(psyColors.onSurface.opacity(0.7))
                    WrapLayout(spacing: 8) {
                        ForEach(AccountType.allCases, id: \.self) { type in
                            typeChip(type)
                        }
                    }

                    Text("Biểu tượng").font(PsyFont.bodyMedium).foregroundStyle(psyColors.onSurface.opacity(0.7))
                    IconPicker(selected: vm.draftIcon) { vm.draftIcon = $0 }

                    Text("Màu sắc").font(PsyFont.bodyMedium).foregroundStyle(psyColors.onSurface.opacity(0.7))
                    ColorPicker(selected: vm.draftColor) { vm.draftColor = $0 }
                }
                .padding(16)
            }
            .background(psyColors.background)
            .navigationTitle(vm.editingId == nil ? "Thêm tài khoản" : "Sửa tài khoản")
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

    private func typeChip(_ type: AccountType) -> some View {
        let isSelected = vm.draftType == type
        return Text(type.vietnamese)
            .font(.system(size: 13))
            .fontWeight(isSelected ? .bold : .regular)
            .foregroundStyle(isSelected ? Color.white : psyColors.onSurface)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(isSelected ? psyColors.primary : psyColors.onSurface.opacity(0.06))
            .clipShape(Capsule())
            .contentShape(Capsule())
            .onTapGesture { vm.draftType = type }
    }
}

extension AccountType {
    var vietnamese: String {
        switch self {
        case .cash: return "Tiền mặt"
        case .bank: return "Ngân hàng"
        case .credit: return "Tín dụng"
        case .asset: return "Tài sản"
        }
    }
}
