import SwiftUI

/// HostGuardIQ radii: chips/inputs 8, buttons 10, cards 14–16, hero 20–24.
enum CandyShape {
    static let small: CGFloat = 10   // buttons
    static let medium: CGFloat = 14  // cards
    static let large: CGFloat = 20   // hero panels
}

/// New-name alias (preferred going forward).
enum PsyRadius {
    static let chip: CGFloat = 8
    static let button: CGFloat = 10
    static let card: CGFloat = 14
    static let hero: CGFloat = 20
    static let pill: CGFloat = 999
}
