import SwiftUI

/// Renders a Lucide icon by its portable name string (e.g. "shopping-bag"), backed by
/// template SVG imagesets named "lucide-<name>" in the asset catalog. Mirrors Android's
/// LucideIcon — the same name strings are stored in the cross-platform snapshot.
enum LucideIcons {
    /// Names that have a bundled "lucide-<name>" imageset. Unknown → circle-dollar-sign.
    static let bundled: Set<String> = [
        "wallet", "landmark", "utensils", "shopping-cart", "coffee", "cup-soda", "bus", "bike",
        "fuel", "train-front", "square-parking", "car", "shopping-bag", "shirt", "package",
        "receipt", "lightbulb", "globe", "gamepad-2", "banknote", "gift", "circle-dollar-sign",
        "house", "pill", "hospital", "smartphone", "plane", "graduation-cap", "dog", "credit-card",
        "trending-up", "dumbbell", "music", "umbrella", "beer", "clapperboard", "chart-column",
        "arrow-right-left", "list", "settings", "plus", "arrow-left", "pencil", "trash-2",
        "triangle-alert", "shield-check", "lock", "palette", "user", "log-out", "chevron-right",
        "chevron-left", "check", "arrow-up-right", "arrow-down-right", "calendar", "fingerprint",
        "delete", "x",
    ]

    /// Icons offered in the picker (data-category vocabulary, ordered).
    static let pickerSet: [String] = [
        "wallet", "landmark", "utensils", "shopping-cart", "coffee", "cup-soda", "bus", "bike",
        "fuel", "train-front", "square-parking", "car", "shopping-bag", "shirt", "package",
        "receipt", "lightbulb", "globe", "gamepad-2", "banknote", "gift", "circle-dollar-sign",
        "house", "pill", "hospital", "smartphone", "plane", "graduation-cap", "dog", "credit-card",
        "trending-up", "dumbbell", "music", "umbrella", "beer", "clapperboard",
    ]

    static func assetName(_ name: String) -> String {
        bundled.contains(name) ? "lucide-\(name)" : "lucide-circle-dollar-sign"
    }
}

struct LucideIcon: View {
    let name: String
    var size: CGFloat = 24
    var tint: Color? = nil

    var body: some View {
        Image(LucideIcons.assetName(name))
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(tint ?? .primary)
    }
}
