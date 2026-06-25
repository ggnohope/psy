import SwiftUI

/// App settings list. Ports `SettingsScreen.kt`: appearance, lock, manage, logout.
struct SettingsView: View {
    let container: AppContainer
    let appVM: AppViewModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.psyColors) private var psyColors
    @State private var showLogoutDialog = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text("Cài đặt")
                    .font(PsyFont.headlineMedium)
                    .foregroundStyle(psyColors.text)
                    .padding(.top, 8)

                if let email = container.tokenStore.email, !email.isEmpty {
                    EyebrowLabel(text: email)
                }

                // Settings card
                card {
                    NavigationLink {
                        AppearanceView(settings: container.settingsStore)
                    } label: {
                        settingsRow(
                            title: "Giao diện",
                            iconName: "palette",
                            tint: psyColors.blue,
                            tileBg: psyColors.blueSoft
                        )
                    }
                    hairline
                    NavigationLink {
                        LockSettingsView(settings: container.settingsStore)
                    } label: {
                        settingsRow(
                            title: "Khoá ứng dụng",
                            iconName: "lock",
                            tint: psyColors.teal,
                            tileBg: psyColors.tealSoft
                        )
                    }
                    hairline
                    NavigationLink {
                        ManageCategoriesView(container: container)
                    } label: {
                        settingsRow(
                            title: "Quản lý danh mục",
                            iconName: "list",
                            tint: psyColors.amber,
                            tileBg: psyColors.amberSoft
                        )
                    }
                    hairline
                    NavigationLink {
                        ManageAccountsView(container: container)
                    } label: {
                        settingsRow(
                            title: "Quản lý tài khoản",
                            iconName: "user",
                            tint: psyColors.green,
                            tileBg: psyColors.greenSoft
                        )
                    }
                }

                // Logout card
                card {
                    Button {
                        showLogoutDialog = true
                    } label: {
                        settingsRow(
                            title: "Đăng xuất",
                            iconName: "log-out",
                            tint: psyColors.red,
                            tileBg: psyColors.redSoft,
                            labelColor: psyColors.red,
                            showChevron: false
                        )
                    }
                }

                EyebrowLabel(text: "PSY · v2.0")
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 4)
            }
            .padding(.horizontal, 22)
            .padding(.bottom, 32)
        }
        .background(psyColors.bg.ignoresSafeArea())
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

    // MARK: - Building blocks

    /// Grouped card container: surface fill, hairline border, radius 14.
    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .background(psyColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: PsyRadius.card))
        .overlay(
            RoundedRectangle(cornerRadius: PsyRadius.card)
                .stroke(psyColors.hair, lineWidth: 1)
        )
    }

    private var hairline: some View {
        Rectangle()
            .fill(psyColors.hair)
            .frame(height: 1)
            .padding(.leading, 60)
    }

    /// One tappable row: 38pt colored tile + label + trailing chevron.
    private func settingsRow(
        title: String,
        iconName: String,
        tint: Color,
        tileBg: Color,
        labelColor: Color? = nil,
        showChevron: Bool = true
    ) -> some View {
        HStack(spacing: 12) {
            IconTile(iconName: iconName, tint: tint, bg: tileBg, size: 38)
            Text(title)
                .font(PsyFont.bodyLarge)
                .foregroundStyle(labelColor ?? psyColors.text)
            Spacer()
            if showChevron {
                LucideIcon(name: "chevron-right", size: 18, tint: psyColors.text3)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}
