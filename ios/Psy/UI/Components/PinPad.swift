import SwiftUI

/// A row of PIN dots; the first `filled` of `total` are solid. Shared by the lock screen
/// and the set-PIN sheet so both look identical.
struct PinDots: View {
    let total: Int
    let filled: Int
    @Environment(\.psyColors) private var colors

    var body: some View {
        HStack(spacing: 18) {
            ForEach(0..<total, id: \.self) { i in
                Circle()
                    .fill(i < filled ? colors.primary : Color.clear)
                    .frame(width: 16, height: 16)
                    .overlay(
                        Circle().stroke(colors.primary.opacity(i < filled ? 0 : 0.35), lineWidth: 1.5)
                    )
            }
        }
        .animation(.easeOut(duration: 0.12), value: filled)
    }
}

/// Candy Pop numeric keypad: 1–9 then [biometric | 0 | ⌫]. `showBiometric` toggles the
/// Face ID key (used on the lock screen, hidden when creating a PIN).
struct PinKeypad: View {
    var showBiometric: Bool = false
    let onDigit: (String) -> Void
    let onDelete: () -> Void
    var onBiometric: () -> Void = {}

    @Environment(\.psyColors) private var colors
    private let keySize: CGFloat = 72

    var body: some View {
        VStack(spacing: 18) {
            ForEach(0..<3, id: \.self) { row in
                HStack(spacing: 24) {
                    ForEach(1..<4, id: \.self) { col in
                        let digit = String(row * 3 + col)
                        digitKey(digit)
                    }
                }
            }
            HStack(spacing: 24) {
                if showBiometric {
                    Button(action: onBiometric) {
                        Image(systemName: "faceid")
                            .font(.system(size: 28))
                            .foregroundStyle(colors.primary)
                            .frame(width: keySize, height: keySize)
                    }
                    .accessibilityLabel("Mở bằng khuôn mặt")
                } else {
                    Color.clear.frame(width: keySize, height: keySize)
                }

                digitKey("0")

                Button(action: onDelete) {
                    Image(systemName: "delete.left")
                        .font(.system(size: 24))
                        .foregroundStyle(colors.onBackground.opacity(0.7))
                        .frame(width: keySize, height: keySize)
                }
                .accessibilityLabel("Xoá")
            }
        }
    }

    private func digitKey(_ label: String) -> some View {
        Button { onDigit(label) } label: {
            Text(label)
                .font(PsyFont.headlineMedium)
                .foregroundStyle(colors.onSurface)
                .frame(width: keySize, height: keySize)
                .background(Circle().fill(colors.primary.opacity(0.10)))
        }
    }
}
