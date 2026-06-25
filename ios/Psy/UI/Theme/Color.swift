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

}
