import SwiftUI

private let pinMaxLength = 6

/// Candy Pop lock screen ported from Android `LockScreen.kt`.
/// PIN entry via a numeric keypad with dots; optional Face ID auto-prompt + button.
struct LockView: View {
    let biometricEnabled: Bool
    let verifyPin: (String) -> Bool
    let onUnlock: () -> Void

    @Environment(\.psyColors) private var psyColors

    @State private var pin = ""
    @State private var error = ""

    private var biometricUsable: Bool { biometricEnabled && BiometricAuthenticator.isAvailable }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            // Title
            Text("🔒")
                .font(.system(size: 48))
            Spacer().frame(height: 8)
            Text("Psy")
                .font(PsyFont.headlineMedium)
                .foregroundStyle(psyColors.primary)

            Spacer().frame(height: 32)

            // PIN dots
            HStack(spacing: 12) {
                ForEach(0..<pinMaxLength, id: \.self) { index in
                    PinDot(filled: index < pin.count, colors: psyColors)
                }
            }

            Spacer().frame(height: 12)

            // Error message (reserve space when empty)
            if !error.isEmpty {
                Text(error)
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(CandyColor.pinkDeep)
            } else {
                Spacer().frame(height: 20)
            }

            Spacer().frame(height: 8)

            // Numeric keypad rows 1–3
            ForEach(0..<3, id: \.self) { row in
                HStack(spacing: 24) {
                    ForEach(1..<4, id: \.self) { col in
                        let digit = String(row * 3 + col)
                        KeypadButton(label: digit, colors: psyColors) { appendDigit(digit) }
                    }
                }
                Spacer().frame(height: 12)
            }

            // Bottom row: biometric | 0 | backspace
            HStack(spacing: 24) {
                if biometricUsable {
                    Button(action: promptBiometric) {
                        Image(systemName: "faceid")
                            .font(.system(size: 30))
                            .foregroundStyle(psyColors.primary)
                            .frame(width: 64, height: 64)
                    }
                    .accessibilityLabel("Vân tay / Khuôn mặt")
                } else {
                    Color.clear.frame(width: 64, height: 64)
                }

                KeypadButton(label: "0", colors: psyColors) { appendDigit("0") }

                Button(action: deleteLastDigit) {
                    Text("⌫")
                        .font(.system(size: 24))
                        .foregroundStyle(psyColors.onBackground)
                        .frame(width: 64, height: 64)
                }
            }

            Spacer().frame(height: 16)

            // Confirm button when 4–5 digits entered (6 digits auto-submits)
            if pin.count >= 4 && pin.count < pinMaxLength {
                Button(action: submitPin) {
                    Text("Mở khoá")
                        .font(PsyFont.titleMedium)
                        .foregroundStyle(psyColors.onSurface)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: CandyShape.large)
                                .fill(psyColors.secondary.opacity(0.4))
                        )
                }
            }

            // Biometric prompt shortcut button
            if biometricUsable {
                Spacer().frame(height: 8)
                Button("Dùng Face ID", action: promptBiometric)
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.primary)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, 48)
        .background(psyColors.background.ignoresSafeArea())
        .task {
            if biometricUsable { promptBiometric() }
        }
    }

    private func appendDigit(_ digit: String) {
        guard pin.count < pinMaxLength else { return }
        pin += digit
        error = ""
        if pin.count == pinMaxLength {
            check(pin)
        }
    }

    private func deleteLastDigit() {
        if !pin.isEmpty { pin.removeLast() }
    }

    private func submitPin() {
        if pin.count >= 4 && pin.count <= pinMaxLength {
            check(pin)
        }
    }

    private func check(_ candidate: String) {
        if verifyPin(candidate) {
            onUnlock()
        } else {
            error = "Sai PIN, thử lại"
            pin = ""
        }
    }

    private func promptBiometric() {
        Task {
            if await BiometricAuthenticator.authenticate() {
                onUnlock()
            }
            // dismissed/failed — fall through to PIN
        }
    }
}

private struct PinDot: View {
    let filled: Bool
    let colors: PsyColors

    var body: some View {
        Circle()
            .fill(filled ? colors.primary : colors.onSurface.opacity(0.3))
            .frame(width: 16, height: 16)
    }
}

private struct KeypadButton: View {
    let label: String
    let colors: PsyColors
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(PsyFont.headlineMedium)
                .foregroundStyle(colors.onSurface)
                .frame(width: 64, height: 64)
                .background(Circle().fill(colors.surface))
        }
    }
}
