import SwiftUI

private let pinLength = 4

/// Candy Pop lock screen. Fixed 4-digit PIN via a numeric keypad; optional Face ID
/// auto-prompt + key. Auto-submits as soon as 4 digits are entered.
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

            Text("🔒").font(.system(size: 48))
            Spacer().frame(height: 10)
            Text("Psy").font(PsyFont.headlineMedium).foregroundStyle(psyColors.primary)
            Spacer().frame(height: 6)
            Text("Nhập mã PIN").font(PsyFont.bodyMedium).foregroundStyle(psyColors.onSurface.opacity(0.6))

            Spacer().frame(height: 28)
            PinDots(total: pinLength, filled: pin.count)

            Spacer().frame(height: 12)
            Text(error.isEmpty ? " " : error)
                .font(PsyFont.bodyMedium)
                .foregroundStyle(CandyColor.pinkDeep)
                .frame(height: 20)

            Spacer().frame(height: 20)
            PinKeypad(showBiometric: biometricUsable, onDigit: appendDigit, onDelete: deleteLast, onBiometric: promptBiometric)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, 40)
        .background(psyColors.background.ignoresSafeArea())
        .task { if biometricUsable { promptBiometric() } }
    }

    private func appendDigit(_ digit: String) {
        guard pin.count < pinLength else { return }
        pin += digit
        error = ""
        if pin.count == pinLength {
            if verifyPin(pin) {
                onUnlock()
            } else {
                error = "Sai mã PIN, thử lại"
                pin = ""
            }
        }
    }

    private func deleteLast() {
        if !pin.isEmpty { pin.removeLast() }
    }

    private func promptBiometric() {
        Task {
            if await BiometricAuthenticator.authenticate() { onUnlock() }
            // dismissed / failed → fall through to PIN
        }
    }
}
