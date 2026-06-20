import SwiftUI

/// Resolved theme colors for the current accent + light/dark, exposed via Environment.
/// Mirrors Compose's MaterialTheme color scheme so views read semantic colors, not raw ones.
struct PsyColors {
    let primary: Color
    let secondary: Color
    let tertiary: Color
    let surface: Color
    let onSurface: Color
    let background: Color
    let onBackground: Color

    static func resolve(accent: AccentPalette, dark: Bool) -> PsyColors {
        let a = accent.colors
        if dark {
            return PsyColors(
                primary: a.primary, secondary: a.secondary, tertiary: a.tertiary,
                surface: CandyColor.surfaceDark, onSurface: CandyColor.onSurfaceDark,
                background: CandyColor.surfaceDark, onBackground: CandyColor.onSurfaceDark
            )
        } else {
            return PsyColors(
                primary: a.primary, secondary: a.secondary, tertiary: a.tertiary,
                surface: CandyColor.surfaceLight, onSurface: CandyColor.onSurfaceLight,
                background: CandyColor.surfaceLight, onBackground: CandyColor.onSurfaceLight
            )
        }
    }
}

private struct PsyColorsKey: EnvironmentKey {
    static let defaultValue = PsyColors.resolve(accent: .candyViolet, dark: false)
}

extension EnvironmentValues {
    var psyColors: PsyColors {
        get { self[PsyColorsKey.self] }
        set { self[PsyColorsKey.self] = newValue }
    }
}

/// Applies the Candy Pop theme: resolves colors for the chosen accent/mode, injects them
/// into the environment, and tints the SwiftUI accent. Wrap the app root in this.
struct PsyTheme: ViewModifier {
    let mode: ThemeMode
    let accent: AccentPalette
    @Environment(\.colorScheme) private var systemScheme

    private var isDark: Bool {
        switch mode {
        case .system: systemScheme == .dark
        case .light:  false
        case .dark:   true
        }
    }

    func body(content: Content) -> some View {
        let colors = PsyColors.resolve(accent: accent, dark: isDark)
        content
            .environment(\.psyColors, colors)
            .tint(colors.primary)
            .preferredColorScheme(mode == .system ? nil : (isDark ? .dark : .light))
            .background(colors.background.ignoresSafeArea())
    }
}

extension View {
    func psyTheme(mode: ThemeMode, accent: AccentPalette) -> some View {
        modifier(PsyTheme(mode: mode, accent: accent))
    }
}
