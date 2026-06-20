import SwiftUI

/// Emoji + color palette pickers shared by the account/category editors.
/// Ports IconColorPicker.kt (same emoji set and color palette values).

enum IconColorPalette {
    /// 36 emojis (6×6 grid), matching EMOJI_LIST in IconColorPicker.kt.
    static let emojis: [String] = [
        "🍜", "🚌", "🛍️", "🧾", "🎮", "💊",
        "📦", "💰", "🎁", "🏠", "🚗", "☕",
        "🍺", "👕", "🏥", "🎬", "📱", "✈️",
        "🎓", "🐶", "💵", "🏦", "💳", "🪙",
        "📈", "🎀", "🧴", "🍔", "🍰", "🚕",
        "⛽", "🏋️", "🎵", "🛒", "☂️", "🎈",
    ]

    /// ARGB-packed colors, matching COLOR_PALETTE in IconColorPicker.kt.
    static let colors: [Int64] = [
        0xFFA18CFF, 0xFF7FD8FF, 0xFFFF8FD6, 0xFFFF5FA2, 0xFF22C55E,
        0xFFFFB86B, 0xFF6BCB77, 0xFF4D96FF, 0xFFFF6B6B, 0xFFB088F9,
    ]
}

/// 6-column emoji grid; selected emoji highlighted with a tinted cell + primary border.
struct EmojiPicker: View {
    @Environment(\.psyColors) private var psyColors
    let selected: String
    let onPick: (String) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 6)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 0) {
            ForEach(IconColorPalette.emojis, id: \.self) { emoji in
                let isSelected = emoji == selected
                Text(emoji)
                    .font(.system(size: 22))
                    .frame(maxWidth: .infinity)
                    .aspectRatio(1, contentMode: .fit)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(isSelected ? psyColors.primary.opacity(0.18) : Color.clear)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(isSelected ? psyColors.primary : .clear, lineWidth: 2)
                    )
                    .contentShape(Rectangle())
                    .onTapGesture { onPick(emoji) }
            }
        }
    }
}

/// Wrapping row of color swatches; selected swatch highlighted with a white ring + check.
struct ColorPicker: View {
    let selected: Int64
    let onPick: (Int64) -> Void

    var body: some View {
        WrapLayout(spacing: 8) {
            ForEach(IconColorPalette.colors, id: \.self) { value in
                let isSelected = value == selected
                ZStack {
                    Circle()
                        .fill(Color(argb: value))
                        .frame(width: 36, height: 36)
                        .overlay(
                            Circle().stroke(Color.white, lineWidth: isSelected ? 3 : 0)
                        )
                    if isSelected {
                        Image(systemName: "checkmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(.white)
                    }
                }
                .contentShape(Circle())
                .onTapGesture { onPick(value) }
            }
        }
    }
}

/// Minimal wrapping layout (mirrors Compose FlowRow) for the color swatches / chips.
struct WrapLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxWidth == .infinity ? x : maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let maxWidth = bounds.width
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.minX + maxWidth, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
