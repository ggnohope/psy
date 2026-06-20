import SwiftUI

/// App settings list. Ports `SettingsScreen.kt`: appearance, lock, manage, logout.
struct SettingsView: View {
    let container: AppContainer
    let appVM: AppViewModel

    @Environment(\.dismiss) private var dismiss
    @State private var showLogoutDialog = false

    var body: some View {
        List {
            if let email = container.tokenStore.email, !email.isEmpty {
                Section {
                    Label(email, systemImage: "person.circle.fill")
                }
            }

            Section {
                NavigationLink {
                    AppearanceView(settings: container.settingsStore)
                } label: {
                    Label("Giao diện", systemImage: "paintpalette")
                }
                NavigationLink {
                    LockSettingsView(settings: container.settingsStore)
                } label: {
                    Label("Khoá ứng dụng", systemImage: "lock")
                }
            }

            Section {
                NavigationLink {
                    ManageCategoriesView(container: container)
                } label: {
                    Label("Quản lý danh mục", systemImage: "list.bullet")
                }
                NavigationLink {
                    ManageAccountsView(container: container)
                } label: {
                    Label("Quản lý tài khoản", systemImage: "creditcard")
                }
            }

            Section {
                Button(role: .destructive) {
                    showLogoutDialog = true
                } label: {
                    Label("Đăng xuất", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .navigationTitle("Cài đặt")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Đăng xuất", isPresented: $showLogoutDialog) {
            Button("Huỷ", role: .cancel) {}
            Button("Đăng xuất", role: .destructive) {
                appVM.logout()
                dismiss()
            }
        } message: {
            Text("Đăng xuất sẽ sao lưu rồi xoá dữ liệu trên máy này. Tiếp tục?")
        }
    }
}
