import SwiftUI

/// Theme mode + accent palette pickers. Ports `AppearanceScreen.kt`.
/// Live: `SettingsStore` is `@Observable`, so changes re-render the themed app immediately.
struct AppearanceView: View {
    @Bindable var settings: SettingsStore
    @Environment(\.psyColors) private var psyColors

    private let modes: [(ThemeMode, String)] = [
        (.system, "Theo hệ thống"),
        (.light, "Sáng"),
        (.dark, "Tối"),
    ]

    var body: some View {
        List {
            Section("Chế độ") {
                ForEach(modes, id: \.0) { mode, label in
                    Button {
                        settings.themeMode = mode
                    } label: {
                        HStack {
                            Text(label)
                                .foregroundStyle(psyColors.onSurface)
                            Spacer()
                            if settings.themeMode == mode {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(psyColors.primary)
                            }
                        }
                    }
                }
            }

            Section("Màu chủ đạo") {
                HStack(spacing: 20) {
                    ForEach(AccentPalette.allCases, id: \.self) { accent in
                        AccentSwatch(
                            accent: accent,
                            selected: settings.accent == accent,
                            ringColor: psyColors.onSurface
                        ) {
                            settings.accent = accent
                        }
                    }
                    Spacer()
                }
                .padding(.vertical, 4)
            }
        }
        .navigationTitle("Giao diện")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct AccentSwatch: View {
    let accent: AccentPalette
    let selected: Bool
    let ringColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Circle()
                .fill(accent.colors.primary)
                .frame(width: 44, height: 44)
                .overlay(
                    Circle()
                        .stroke(ringColor, lineWidth: selected ? 3 : 0)
                )
        }
        .buttonStyle(.plain)
    }
}
