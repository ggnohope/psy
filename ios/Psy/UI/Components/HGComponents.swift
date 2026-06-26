import SwiftUI

/// Shared HostGuardIQ SwiftUI components. Mirror the Android components in ui/components/.
/// All read colors from `@Environment(\.psyColors)`.

/// Tinted rounded-square tile holding a Lucide data icon.
struct IconTile: View {
    let iconName: String
    let tint: Color
    let bg: Color
    var size: CGFloat = 44

    var body: some View {
        RoundedRectangle(cornerRadius: size * 0.25)
            .fill(bg)
            .frame(width: size, height: size)
            .overlay(LucideIcon(name: iconName, size: size * 0.5, tint: tint))
    }
}

/// IBM Plex Mono uppercase eyebrow label.
struct EyebrowLabel: View {
    let text: String
    var color: Color? = nil
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        Text(text.uppercased())
            .font(PsyFont.mono(11))
            .tracking(1.6)
            .foregroundStyle(color ?? psyColors.text3)
    }
}

/// Single-select segmented control. Active segment = primary bg + white text.
struct SegmentedControl: View {
    let options: [String]
    let selectedIndex: Int
    let onSelect: (Int) -> Void
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Array(options.enumerated()), id: \.offset) { item in
                let i = item.offset
                let active = i == selectedIndex
                Text(item.element)
                    .font(PsyFont.bodyMedium).fontWeight(.semibold)
                    .foregroundStyle(active ? .white : psyColors.text2)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(active ? psyColors.blue : .clear)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .contentShape(Rectangle())
                    .onTapGesture { onSelect(i) }
            }
        }
        .padding(4)
        .background(psyColors.sunken)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

/// Navy-gradient hero card with a 3px accent bar across the top edge.
struct HeroCard<Content: View>: View {
    @ViewBuilder var content: Content
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(psyColors.heroGradient)
        .overlay(alignment: .top) {
            Rectangle().fill(psyColors.accentLine).frame(height: 3)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

/// Shared transaction row used on Home and Calendar.
struct TransactionRowView: View {
    let iconName: String
    let iconTint: Color
    let iconBg: Color
    let name: String
    let meta: String
    let amount: String
    let isIncome: Bool
    let account: String
    var isFund: Bool = false
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        HStack(spacing: 13) {
            IconTile(iconName: iconName, tint: iconTint, bg: iconBg, size: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(name).font(PsyFont.bodyLarge.weight(.semibold)).foregroundStyle(psyColors.text)
                Text(meta).font(.system(size: 12)).foregroundStyle(psyColors.text3)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(amount).font(PsyFont.display(15)).foregroundStyle(isIncome ? psyColors.green : psyColors.red)
                Text(account).font(.system(size: 11)).foregroundStyle(psyColors.text3)
                if isFund {
                    Text("Quỹ")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(psyColors.text3)
                        .padding(.horizontal, 6).padding(.vertical, 1)
                        .background(psyColors.text3.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
                        .padding(.top, 2)
                }
            }
        }
        .padding(.horizontal, 14).padding(.vertical, 13)
        .background(psyColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(psyColors.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

/// Calm empty-state block.
struct EmptyStateView: View {
    let iconName: String
    let title: String
    let caption: String
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        VStack(spacing: 8) {
            IconTile(iconName: iconName, tint: psyColors.text3, bg: psyColors.sunken, size: 56)
            Text(title).font(PsyFont.bodyLarge.weight(.semibold)).foregroundStyle(psyColors.text)
            Text(caption).font(.system(size: 13)).foregroundStyle(psyColors.text3).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity).padding(32)
    }
}

/// 42x42 chevron-flanked month switcher (Space Grotesk 18 label).
struct MonthSwitcher: View {
    let label: String
    let onPrev: () -> Void
    let onNext: () -> Void
    @Environment(\.psyColors) private var psyColors

    private func chip(_ icon: String, _ action: @escaping () -> Void) -> some View {
        RoundedRectangle(cornerRadius: 10).fill(psyColors.surface)
            .frame(width: 36, height: 36)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(psyColors.hair, lineWidth: 1))
            .overlay(LucideIcon(name: icon, size: 18, tint: psyColors.text))
            .onTapGesture(perform: action)
    }

    var body: some View {
        HStack {
            chip("chevron-left", onPrev)
            Spacer()
            Text(label).font(PsyFont.titleMedium).foregroundStyle(psyColors.text)
            Spacer()
            chip("chevron-right", onNext)
        }
    }
}
