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
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                modeCard
                accentSection
            }
            .padding(22)
        }
        .background(psyColors.bg)
        .navigationTitle("Giao diện")
        .navigationBarTitleDisplayMode(.inline)
    }

    // Mode list card — three radio rows split by hairlines.
    private var modeCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(modes.enumerated()), id: \.element.0) { index, entry in
                let (mode, label) = entry
                if index > 0 {
                    Rectangle()
                        .fill(psyColors.hair)
                        .frame(height: 1)
                }
                Button {
                    settings.themeMode = mode
                } label: {
                    HStack(spacing: 14) {
                        Text(label)
                            .font(PsyFont.bodyLarge)
                            .foregroundStyle(psyColors.text)
                        Spacer()
                        RadioDot(selected: settings.themeMode == mode)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .background(psyColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(psyColors.hair, lineWidth: 1)
        )
    }

    // Accent swatches.
    private var accentSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowLabel(text: "Màu nhấn")
            HStack(spacing: 14) {
                ForEach(AccentPalette.allCases, id: \.self) { accent in
                    AccentSwatch(
                        accent: accent,
                        selected: settings.accent == accent,
                        borderColor: psyColors.text
                    ) {
                        settings.accent = accent
                    }
                }
                Spacer()
            }
        }
    }
}

/// Radio indicator: selected = filled blue dot inside a blue ring; unselected = empty text3 ring.
private struct RadioDot: View {
    let selected: Bool
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        ZStack {
            Circle()
                .stroke(selected ? psyColors.blue : psyColors.text3, lineWidth: 2)
                .frame(width: 22, height: 22)
            if selected {
                Circle()
                    .fill(psyColors.blue)
                    .frame(width: 12, height: 12)
            }
        }
    }
}

private struct AccentSwatch: View {
    let accent: AccentPalette
    let selected: Bool
    let borderColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(accent.primary(dark: false))
                .frame(width: 58, height: 58)
                .overlay {
                    if selected {
                        LucideIcon(name: "check", size: 24, tint: .white)
                    }
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(borderColor, lineWidth: selected ? 3 : 0)
                )
        }
        .buttonStyle(.plain)
    }
}
