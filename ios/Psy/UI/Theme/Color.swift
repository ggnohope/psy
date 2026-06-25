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

enum ThemeMode: String, CaseIterable, Codable {
    case system = "SYSTEM"
    case light  = "LIGHT"
    case dark   = "DARK"
}

/// HostGuardIQ accent choices. Rebinds the primary (`blue`) token app-wide.
enum AccentPalette: String, CaseIterable, Codable {
    case blue  = "BLUE"
    case amber = "AMBER"
    case teal  = "TEAL"

    /// Primary hue per accent + light/dark.
    func primary(dark: Bool) -> Color {
        switch self {
        case .blue:  Color(argb: dark ? 0xFF3D97F8 : 0xFF0A7CF6)
        case .amber: Color(argb: dark ? 0xFFFBB43D : 0xFFF59E0B)
        case .teal:  Color(argb: dark ? 0xFF19E3E0 : 0xFF0BB3B0)
        }
    }

    /// Soft variant of the accent primary (active pills/tiles).
    func soft(dark: Bool) -> Color {
        switch self {
        case .blue:  Color(argb: dark ? 0x2E3D97F8 : 0xFFE8F2FE)
        case .amber: Color(argb: dark ? 0x33FBB43D : 0xFFFEF0D4)
        case .teal:  Color(argb: dark ? 0x3319E3E0 : 0xFFDCF8F7)
        }
    }

    // Legacy compatibility (used by AppearanceView swatch until re-skinned).
    var colors: AccentColors { AccentColors(primary: primary(dark: false), secondary: Color(argb: 0xFF0BB3B0), tertiary: Color(argb: 0xFFF59E0B)) }
}

struct AccentColors {
    let primary: Color
    let secondary: Color
    let tertiary: Color
}

/// TEMPORARY bridge during the HostGuardIQ redesign — old Candy color names remapped to
/// the closest HostGuardIQ token so existing views compile before they are re-skinned.
/// Remove once no view references CandyColor.
enum CandyColor {
    static let violet     = Color(argb: 0xFF0A7CF6) // → blue
    static let sky        = Color(argb: 0xFF0BB3B0) // → teal
    static let pink       = Color(argb: 0xFFF59E0B) // → amber
    static let pinkDeep   = Color(argb: 0xFFE0413A) // → red (expense/danger)
    static let green      = Color(argb: 0xFF1F9D62) // → green (income)

    static let surfaceLight   = Color(argb: 0xFFF7F9FC)
    static let onSurfaceLight  = Color(argb: 0xFF0A2540)
    static let surfaceDark     = Color(argb: 0xFF061A30)
    static let onSurfaceDark   = Color(argb: 0xFFEEF2F8)
}
