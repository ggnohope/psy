import SwiftUI

/// App-lock settings: lock toggle (set a 4-digit PIN on enable), change PIN, biometric toggle
/// (only when a PIN is set + biometrics available). PIN creation uses a HostGuardIQ keypad sheet.
struct LockSettingsView: View {
    @Bindable var settings: SettingsStore

    @Environment(\.psyColors) private var psyColors
    @State private var showSetPin = false

    private var biometricAvailable: Bool { BiometricAuthenticator.isAvailable }
    private var biometricEnabledAllowed: Bool { settings.lockEnabled && settings.pinHash != nil && biometricAvailable }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Yêu cầu mã PIN 4 số mỗi khi mở lại ứng dụng.")
                    .font(PsyFont.bodyMedium)
                    .foregroundStyle(psyColors.text3)

                card
            }
            .padding(22)
        }
        .background(psyColors.bg.ignoresSafeArea())
        .navigationTitle("Khoá ứng dụng")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showSetPin) {
            SetPinSheet(
                onSave: { newPin in
                    settings.setPin(newPin)
                    settings.lockEnabled = true
                    showSetPin = false
                },
                onCancel: { showSetPin = false }
            )
        }
    }

    private var card: some View {
        VStack(spacing: 0) {
            // Lock toggle row
            HStack(spacing: 12) {
                iconTile
                Text("Khoá ứng dụng")
                    .font(PsyFont.bodyLarge)
                    .foregroundStyle(psyColors.text)
                Spacer()
                Toggle("", isOn: lockBinding)
                    .labelsHidden()
                    .tint(psyColors.blue)
            }
            .padding(16)

            if settings.lockEnabled {
                divider
                Button { showSetPin = true } label: {
                    HStack(spacing: 12) {
                        Text("Đổi mã PIN")
                            .font(PsyFont.bodyLarge)
                            .foregroundStyle(psyColors.text)
                        Spacer()
                        LucideIcon(name: "chevron-right", size: 20, tint: psyColors.text3)
                    }
                    .padding(16)
                }
                .buttonStyle(.plain)
            }

            divider

            // Biometric row
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Mở bằng vân tay / khuôn mặt")
                        .font(PsyFont.bodyLarge)
                        .foregroundStyle(psyColors.text)
                    if !biometricAvailable {
                        Text("Thiết bị chưa cài vân tay / khuôn mặt")
                            .font(PsyFont.bodyMedium)
                            .foregroundStyle(psyColors.text3)
                    }
                }
                Spacer()
                if biometricAvailable {
                    Toggle("", isOn: $settings.biometricEnabled)
                        .labelsHidden()
                        .tint(psyColors.blue)
                        .disabled(!biometricEnabledAllowed)
                }
            }
            .padding(16)
            .opacity(biometricAvailable ? 1 : 0.55)
        }
        .background(psyColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(psyColors.hair, lineWidth: 1)
        )
    }

    private var iconTile: some View {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .fill(psyColors.blueSoft)
            .frame(width: 38, height: 38)
            .overlay(LucideIcon(name: "lock", size: 20, tint: psyColors.blue))
    }

    private var divider: some View {
        Rectangle()
            .fill(psyColors.hair)
            .frame(height: 1)
    }

    private var lockBinding: Binding<Bool> {
        Binding(
            get: { settings.lockEnabled },
            set: { enabled in
                if enabled {
                    showSetPin = true            // require a PIN before lock turns on
                } else {
                    settings.lockEnabled = false
                    settings.biometricEnabled = false
                    settings.clearPin()
                }
            }
        )
    }
}

/// HostGuardIQ keypad sheet to create a 4-digit PIN: enter once, then confirm. Mismatch resets.
private struct SetPinSheet: View {
    let onSave: (String) -> Void
    let onCancel: () -> Void

    @Environment(\.psyColors) private var psyColors

    private let pinLength = 4
    @State private var first = ""
    @State private var pin = ""
    @State private var error = ""

    private var confirming: Bool { first.count == pinLength }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("Huỷ", action: onCancel)
                    .foregroundStyle(psyColors.blue)
                    .font(PsyFont.bodyMedium)
                Spacer()
            }
            .padding()

            Spacer()
            LucideIcon(name: "lock", size: 44, tint: psyColors.blue)
            Spacer().frame(height: 14)
            Text(confirming ? "Nhập lại mã PIN" : "Tạo mã PIN 4 số")
                .font(PsyFont.titleMedium)
                .foregroundStyle(psyColors.text)

            Spacer().frame(height: 26)
            PinDots(total: pinLength, filled: pin.count)

            Spacer().frame(height: 12)
            Text(error.isEmpty ? " " : error)
                .font(PsyFont.bodyMedium)
                .foregroundStyle(psyColors.red)
                .frame(height: 20)

            Spacer().frame(height: 20)
            PinKeypad(onDigit: appendDigit, onDelete: deleteLast)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(psyColors.bg.ignoresSafeArea())
        .presentationDetents([.large])
    }

    private func appendDigit(_ digit: String) {
        guard pin.count < pinLength else { return }
        pin += digit
        error = ""
        guard pin.count == pinLength else { return }
        if first.isEmpty {
            first = pin            // captured first entry → move to confirm
            pin = ""
        } else if pin == first {
            onSave(first)
        } else {
            error = "Mã PIN không khớp, thử lại"
            first = ""
            pin = ""
        }
    }

    private func deleteLast() {
        if !pin.isEmpty { pin.removeLast() }
    }
}
