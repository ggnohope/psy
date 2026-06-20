import SwiftUI

/// Quicksand typography mirroring CandyTypography in Type.kt.
/// Weights come from the variable font; sizes/weights match the Android styles.
enum PsyFont {
    private static let family = "Quicksand"

    static let headlineMedium = Font.custom(family, size: 24).weight(.heavy)   // ExtraBold
    static let titleMedium    = Font.custom(family, size: 16).weight(.bold)
    static let bodyMedium     = Font.custom(family, size: 14).weight(.medium)
    static let labelSmall     = Font.custom(family, size: 11).weight(.semibold)
}
