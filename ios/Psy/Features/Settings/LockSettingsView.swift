import SwiftUI

/// App-lock settings: lock toggle (set a 4-digit PIN on enable), change PIN, biometric toggle
/// (only when a PIN is set + biometrics available). PIN creation uses a Candy keypad sheet.
struct LockSettingsView: View {
    @Bindable var settings: SettingsStore

    @State private var showSetPin = false

    private var biometricAvailable: Bool { BiometricAuthenticator.isAvailable }

    var body: some View {
        List {
            Section {
                Toggle(isOn: lockBinding) {
                    Label("Khoá ứng dụng", systemImage: "lock.fill")
                }

                if settings.lockEnabled {
                    Button { showSetPin = true } label: {
                        HStack {
                            Text("Đổi mã PIN").foregroundStyle(.primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(.secondary).font(.footnote)
                        }
                    }
                }
            } footer: {
                Text("Yêu cầu mã PIN 4 số mỗi khi mở lại ứng dụng.")
            }

            Section {
                Toggle(isOn: $settings.biometricEnabled) {
                    Label("Mở bằng vân tay / khuôn mặt", systemImage: "faceid")
                }
                .disabled(!(settings.lockEnabled && settings.pinHash != nil && biometricAvailable))
            } footer: {
                if !biometricAvailable {
                    Text("Thiết bị chưa cài vân tay / khuôn mặt.")
                }
            }
        }
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

/// Candy keypad sheet to create a 4-digit PIN: enter once, then confirm. Mismatch resets.
private struct SetPinSheet: View {
    let onSave: (String) -> Void
    let onCancel: () -> Void

    @Environment(\.psyColors) private var colors

    private let pinLength = 4
    @State private var first = ""
    @State private var pin = ""
    @State private var error = ""

    private var confirming: Bool { first.count == pinLength }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("Huỷ", action: onCancel).foregroundStyle(colors.primary).font(PsyFont.bodyMedium)
                Spacer()
            }
            .padding()

            Spacer()
            Text("🔐").font(.system(size: 44))
            Spacer().frame(height: 14)
            Text(confirming ? "Nhập lại mã PIN" : "Tạo mã PIN 4 số")
                .font(PsyFont.titleMedium)
                .foregroundStyle(colors.onSurface)

            Spacer().frame(height: 26)
            PinDots(total: pinLength, filled: pin.count)

            Spacer().frame(height: 12)
            Text(error.isEmpty ? " " : error)
                .font(PsyFont.bodyMedium)
                .foregroundStyle(CandyColor.pinkDeep)
                .frame(height: 20)

            Spacer().frame(height: 20)
            PinKeypad(onDigit: appendDigit, onDelete: deleteLast)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(colors.background.ignoresSafeArea())
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
