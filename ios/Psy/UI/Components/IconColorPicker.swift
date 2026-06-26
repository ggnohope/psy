import SwiftUI

/// Emoji + color palette pickers shared by the account/category editors.
/// Ports IconColorPicker.kt (same emoji set and color palette values).

enum IconColorPalette {
    /// HostGuardIQ color palette (blue/amber/teal/green/red + neutrals).
    static let colors: [Int64] = [
        0xFF0A7CF6, 0xFFF59E0B, 0xFF0BB3B0, 0xFF1F9D62, 0xFFE0413A,
        0xFF3D97F8, 0xFFFBB43D, 0xFF19E3E0, 0xFF5B6B80, 0xFF0A2540,
    ]
}

/// Searchable Lucide icon picker (replaces the old emoji grid).
/// `selected`/`onPick` are Lucide name strings (e.g. "shopping-bag").
struct IconPicker: View {
    @Environment(\.psyColors) private var psyColors
    let selected: String
    let onPick: (String) -> Void
    @State private var query = ""

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 6), count: 6)
    private var items: [String] {
        query.isEmpty ? LucideIcons.pickerSet
            : LucideIcons.pickerSet.filter { $0.localizedCaseInsensitiveContains(query.trimmingCharacters(in: .whitespaces)) }
    }

    var body: some View {
        VStack(spacing: 8) {
            TextField("Tìm biểu tượng", text: $query)
                .textFieldStyle(.roundedBorder)
            // Fixed-height scroll area so the (now ~112-icon) grid doesn't blow up the
            // editor's height — mirrors Android's height((6*52).dp) bounded grid.
            ScrollView {
                LazyVGrid(columns: columns, spacing: 6) {
                    ForEach(items, id: \.self) { name in
                        let isSelected = name == selected
                        RoundedRectangle(cornerRadius: 10)
                            .fill(isSelected ? psyColors.blueSoft : psyColors.sunken)
                            .aspectRatio(1, contentMode: .fit)
                            .overlay(LucideIcon(name: name, size: 22, tint: isSelected ? psyColors.blue : psyColors.text2))
                            .contentShape(Rectangle())
                            .onTapGesture { onPick(name) }
                    }
                }
            }
            .frame(height: 312)
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
