import SwiftUI

/// Full HostGuardIQ semantic token set, exposed via Environment. Mirrors Android's PsyColors.
struct PsyColors {
    let bg: Color
    let surface: Color
    let sunken: Color
    let hair: Color
    let text: Color
    let text2: Color
    let text3: Color
    let blue: Color        // primary (rebound by accent)
    let blueSoft: Color
    let amber: Color
    let amberSoft: Color
    let teal: Color
    let tealSoft: Color
    let green: Color
    let greenSoft: Color
    let red: Color
    let redSoft: Color
    let isDark: Bool

    // Brand grounds (both themes)
    var navy: Color { PsyColors.brandNavy }
    var navyDeep: Color { PsyColors.brandNavyDeep }
    var incomeTint: Color { PsyColors.brandIncomeTint }   // on navy
    var expenseTint: Color { PsyColors.brandExpenseTint } // on navy
    var heroGradient: LinearGradient {
        LinearGradient(colors: PsyColors.heroColors, startPoint: .topLeading, endPoint: .bottomTrailing)
    }
    var accentLine: LinearGradient {
        LinearGradient(colors: [blue, PsyColors.brandTeal], startPoint: .leading, endPoint: .trailing)
    }

    static let brandNavy = Color(argb: 0xFF0A2540)
    static let brandNavyDeep = Color(argb: 0xFF061A30)
    static let brandTeal = Color(argb: 0xFF19E3E0)
    static let brandIncomeTint = Color(argb: 0xFF7BE3B0)
    static let brandExpenseTint = Color(argb: 0xFFF8A09B)
    static let heroColors: [Color] = [Color(argb: 0xFF103458), Color(argb: 0xFF0A2540), Color(argb: 0xFF061A30)]

    // Hoisted palette constants — keeps resolve() a fast set of member references
    // (avoids the Swift type-checker timeout on a big inline initializer).
    private enum L {
        static let bg = Color(argb: 0xFFF7F9FC), surface = Color(argb: 0xFFFFFFFF), sunken = Color(argb: 0xFFEEF2F8)
        static let hair = Color(argb: 0xFFDDE5EF), text = Color(argb: 0xFF0A2540), text2 = Color(argb: 0xFF33455C)
        static let text3 = Color(argb: 0xFF5B6B80), amber = Color(argb: 0xFFF59E0B), amberSoft = Color(argb: 0xFFFEF0D4)
        static let teal = Color(argb: 0xFF0BB3B0), tealSoft = Color(argb: 0xFFDCF8F7), green = Color(argb: 0xFF1F9D62)
        static let greenSoft = Color(argb: 0xFFE6F6ED), red = Color(argb: 0xFFE0413A), redSoft = Color(argb: 0xFFFDECEC)
    }
    private enum D {
        static let bg = Color(argb: 0xFF061A30), surface = Color(argb: 0xFF0D2A48), sunken = Color(argb: 0xFF103458)
        static let hair = Color(argb: 0xFF1C486F), text = Color(argb: 0xFFEEF2F8), text2 = Color(argb: 0xFFAEC4DA)
        static let text3 = Color(argb: 0xFF7E96AE), amber = Color(argb: 0xFFFBB43D), amberSoft = Color(argb: 0x33FBB43D)
        static let teal = Color(argb: 0xFF19E3E0), tealSoft = Color(argb: 0x3319E3E0), green = Color(argb: 0xFF3CC987)
        static let greenSoft = Color(argb: 0x333CC987), red = Color(argb: 0xFFF06B65), redSoft = Color(argb: 0x33F06B65)
    }

    static func resolve(accent: AccentPalette, dark: Bool) -> PsyColors {
        if dark {
            return PsyColors(
                bg: D.bg, surface: D.surface, sunken: D.sunken, hair: D.hair, text: D.text, text2: D.text2,
                text3: D.text3, blue: accent.primary(dark: true), blueSoft: accent.soft(dark: true),
                amber: D.amber, amberSoft: D.amberSoft, teal: D.teal, tealSoft: D.tealSoft,
                green: D.green, greenSoft: D.greenSoft, red: D.red, redSoft: D.redSoft, isDark: true
            )
        } else {
            return PsyColors(
                bg: L.bg, surface: L.surface, sunken: L.sunken, hair: L.hair, text: L.text, text2: L.text2,
                text3: L.text3, blue: accent.primary(dark: false), blueSoft: accent.soft(dark: false),
                amber: L.amber, amberSoft: L.amberSoft, teal: L.teal, tealSoft: L.tealSoft,
                green: L.green, greenSoft: L.greenSoft, red: L.red, redSoft: L.redSoft, isDark: false
            )
        }
    }
}

private struct PsyColorsKey: EnvironmentKey {
    static let defaultValue = PsyColors.resolve(accent: .blue, dark: false)
}

extension EnvironmentValues {
    var psyColors: PsyColors {
        get { self[PsyColorsKey.self] }
        set { self[PsyColorsKey.self] = newValue }
    }
}

/// Applies the HostGuardIQ theme: resolves colors for the chosen accent/mode, injects them
/// into the environment, tints the SwiftUI accent, and sets the background. Wrap the app root.
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
            .tint(colors.blue)
            .preferredColorScheme(mode == .system ? nil : (isDark ? .dark : .light))
            .background(colors.bg.ignoresSafeArea())
    }
}

extension View {
    func psyTheme(mode: ThemeMode, accent: AccentPalette) -> some View {
        modifier(PsyTheme(mode: mode, accent: accent))
    }
}
