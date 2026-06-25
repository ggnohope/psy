import SwiftUI

/// HostGuardIQ typography. Space Grotesk for display/numbers, IBM Plex Sans for body/UI,
/// IBM Plex Mono for eyebrows/time/codes. Mirrors Android's PsyTypography.
enum PsyFont {
    static let spaceGrotesk = "Space Grotesk"
    static let plexSans = "IBM Plex Sans"
    static let plexMono = "IBM Plex Mono"

    // Display / numbers
    static let headlineMedium = Font.custom(spaceGrotesk, size: 28).weight(.bold)   // screen titles
    static let titleLarge     = Font.custom(spaceGrotesk, size: 20).weight(.semibold)
    static let titleMedium    = Font.custom(spaceGrotesk, size: 18).weight(.semibold)
    static func display(_ size: CGFloat) -> Font { Font.custom(spaceGrotesk, size: size).weight(.bold) }

    // Body / UI
    static let bodyLarge      = Font.custom(plexSans, size: 16)
    static let bodyMedium     = Font.custom(plexSans, size: 14)
    static let labelLarge     = Font.custom(plexSans, size: 16).weight(.semibold)

    // Mono / eyebrows / time
    static let labelSmall     = Font.custom(plexMono, size: 11).weight(.semibold)
    static func mono(_ size: CGFloat) -> Font { Font.custom(plexMono, size: size).weight(.semibold) }
}
