import SwiftUI

extension Color {
    /// Build a Color from an ARGB-packed Int64 (0xAARRGGBB), matching Android's packed Long colors.
    init(argb: Int64) {
        let a = Double((argb >> 24) & 0xFF) / 255.0
        let r = Double((argb >> 16) & 0xFF) / 255.0
        let g = Double((argb >> 8) & 0xFF) / 255.0
        let b = Double(argb & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

enum CandyColor {
    static let violet     = Color(argb: 0xFFA18CFF)
    static let sky        = Color(argb: 0xFF7FD8FF)
    static let pink       = Color(argb: 0xFFFF8FD6)
    static let pinkDeep   = Color(argb: 0xFFFF5FA2)
    static let green      = Color(argb: 0xFF22C55E)

    static let surfaceLight   = Color(argb: 0xFFF4F0FF)
    static let onSurfaceLight  = Color(argb: 0xFF2B2640)
    static let surfaceDark     = Color(argb: 0xFF1C1830)
    static let onSurfaceDark   = Color(argb: 0xFFEDE9FF)
}

/// Accent color triplet (primary, secondary, tertiary) — mirrors AccentColors.
struct AccentColors {
    let primary: Color
    let secondary: Color
    let tertiary: Color
}

enum AccentPalette: String, CaseIterable, Codable {
    case candyViolet = "CANDY_VIOLET"
    case candyPink   = "CANDY_PINK"
    case candyMint   = "CANDY_MINT"

    var colors: AccentColors {
        switch self {
        case .candyViolet: AccentColors(primary: CandyColor.violet, secondary: CandyColor.sky, tertiary: CandyColor.pink)
        case .candyPink:   AccentColors(primary: CandyColor.pink, secondary: CandyColor.pinkDeep, tertiary: CandyColor.violet)
        case .candyMint:   AccentColors(primary: CandyColor.green, secondary: CandyColor.sky, tertiary: CandyColor.violet)
        }
    }
}

enum ThemeMode: String, CaseIterable, Codable {
    case system = "SYSTEM"
    case light  = "LIGHT"
    case dark   = "DARK"
}
