import SwiftUI
import PsyCore

/// Manage accounts: list + add/edit editor sheet. Ports ManageAccountsScreen.kt.
/// HostGuardIQ re-skin — tokens + Lucide icons, no emoji as iconography.
struct ManageAccountsView: View {
    @Environment(\.psyColors) private var psyColors
    @State private var vm: ManageAccountsViewModel
    @State private var editorOpen = false

    init(container: AppContainer) {
        _vm = State(initialValue: ManageAccountsViewModel(container: container))
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            psyColors.bg.ignoresSafeArea()

            Group {
                if vm.items.isEmpty {
                    EmptyStateView(
                        iconName: "wallet",
                        title: "Chưa có tài khoản",
                        caption: "Thêm tài khoản đầu tiên."
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    list
                }
            }

            // Floating add button.
            Button {
                vm.startAdd()
                editorOpen = true
            } label: {
                LucideIcon(name: "plus", size: 24, tint: .white)
                    .frame(width: 56, height: 56)
                    .background(Circle().fill(psyColors.blue))
            }
            .padding(22)
        }
        .navigationTitle("Quản lý tài khoản")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $editorOpen) {
            AccountEditorSheet(vm: vm) { editorOpen = false }
        }
    }

    private var list: some View {
        ScrollView {
            VStack(spacing: 12) {
                ForEach(vm.items) { account in
                    AccountRow(account: account)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            vm.startEdit(account)
                            editorOpen = true
                        }
                }
            }
            .padding(22)
            .padding(.bottom, 80) // clear the FAB
        }
    }
}

private struct AccountRow: View {
    @Environment(\.psyColors) private var psyColors
    let account: Account

    var body: some View {
        HStack(spacing: 13) {
            IconTile(
                iconName: account.icon,
                tint: Color(argb: account.color),
                bg: Color(argb: account.color).opacity(0.14),
                size: 48
            )
            VStack(alignment: .leading, spacing: 3) {
                Text(account.name)
                    .font(PsyFont.bodyLarge.weight(.semibold))
                    .foregroundStyle(psyColors.text)
                Text(account.type.rawValue)
                    .font(PsyFont.mono(11))
                    .tracking(1.2)
                    .foregroundStyle(psyColors.text3)
            }
            Spacer()
            LucideIcon(name: "chevron-right", size: 20, tint: psyColors.text3)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(psyColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(psyColors.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct AccountEditorSheet: View {
    @Environment(\.psyColors) private var psyColors
    @Bindable var vm: ManageAccountsViewModel
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 8) {
                        EyebrowLabel(text: "Tên tài khoản")
                        TextField("Tên tài khoản", text: $vm.draftName)
                            .font(PsyFont.bodyLarge)
                            .foregroundStyle(psyColors.text)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 12)
                            .background(psyColors.surface)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(psyColors.hair, lineWidth: 1))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        EyebrowLabel(text: "Loại tài khoản")
                        WrapLayout(spacing: 8) {
                            ForEach(AccountType.allCases, id: \.self) { type in
                                typeChip(type)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Toggle(isOn: $vm.draftIsFund) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Quỹ")
                                    .font(PsyFont.bodyLarge.weight(.semibold))
                                    .foregroundStyle(psyColors.text)
                                Text("Không tính vào thu/chi & ngân sách")
                                    .font(PsyFont.bodyMedium)
                                    .foregroundStyle(psyColors.text3)
                            }
                        }
                        .tint(psyColors.blue)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        EyebrowLabel(text: "Biểu tượng")
                        IconPicker(selected: vm.draftIcon) { vm.draftIcon = $0 }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        EyebrowLabel(text: "Màu sắc")
                        ColorPicker(selected: vm.draftColor) { vm.draftColor = $0 }
                    }

                    HStack(spacing: 12) {
                        Button { onDone() } label: {
                            Text("Huỷ")
                                .font(PsyFont.bodyLarge)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 13)
                                .foregroundStyle(psyColors.text2)
                                .background(psyColors.surface)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(psyColors.hair, lineWidth: 1))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        Button { vm.save(); onDone() } label: {
                            Text("Lưu")
                                .font(PsyFont.bodyLarge.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 13)
                                .foregroundStyle(.white)
                                .background(saveDisabled ? psyColors.text3 : psyColors.blue)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(saveDisabled)
                    }
                    .padding(.top, 4)
                }
                .padding(22)
            }
            .background(psyColors.bg.ignoresSafeArea())
            .navigationTitle(vm.editingId == nil ? "Thêm tài khoản" : "Sửa tài khoản")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var saveDisabled: Bool {
        vm.draftName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func typeChip(_ type: AccountType) -> some View {
        let isSelected = vm.draftType == type
        return Text(type.vietnamese)
            .font(PsyFont.bodyMedium.weight(isSelected ? .semibold : .regular))
            .foregroundStyle(isSelected ? Color.white : psyColors.text2)
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .background(isSelected ? psyColors.blue : psyColors.sunken)
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
