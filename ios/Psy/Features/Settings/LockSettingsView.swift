import SwiftUI

/// App-lock settings. Ports `LockSettingsScreen.kt`: lock toggle (set PIN on enable),
/// change/clear PIN, biometric toggle (only when a PIN is set + biometrics available).
struct LockSettingsView: View {
    @Bindable var settings: SettingsStore

    @State private var showSetPin = false
    @State private var pinEntry = ""
    @State private var pinConfirm = ""
    @State private var pinError = ""

    private var biometricAvailable: Bool { BiometricAuthenticator.isAvailable }

    var body: some View {
        List {
            Section {
                Toggle(isOn: lockBinding) {
                    Label("Khoá ứng dụng", systemImage: "lock")
                }

                if settings.lockEnabled {
                    Button {
                        openSetPin()
                    } label: {
                        HStack {
                            Text("Đổi PIN")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.secondary)
                                .font(.footnote)
                        }
                    }
                }
            }

            Section {
                Toggle(isOn: $settings.biometricEnabled) {
                    Text("Mở bằng vân tay/khuôn mặt")
                }
                .disabled(!(settings.lockEnabled && settings.pinHash != nil && biometricAvailable))
            } footer: {
                if !biometricAvailable {
                    Text("Thiết bị chưa cài vân tay/khuôn mặt")
                }
            }
        }
        .navigationTitle("Khoá ứng dụng")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Đặt PIN", isPresented: $showSetPin) {
            SecureField("PIN (4–6 chữ số)", text: $pinEntry)
                .keyboardType(.numberPad)
            SecureField("Xác nhận PIN", text: $pinConfirm)
                .keyboardType(.numberPad)
            Button("Lưu") { confirmSetPin() }
            Button("Huỷ", role: .cancel) { closeSetPin() }
        } message: {
            if pinError.isEmpty {
                Text("Nhập mã PIN 4–6 chữ số.")
            } else {
                Text(pinError)
            }
        }
    }

    private var lockBinding: Binding<Bool> {
        Binding(
            get: { settings.lockEnabled },
            set: { enabled in
                if enabled {
                    // Require a PIN before lock is actually enabled.
                    openSetPin()
                } else {
                    settings.lockEnabled = false
                    settings.biometricEnabled = false
                    settings.clearPin()
                }
            }
        )
    }

    private func openSetPin() {
        pinEntry = ""
        pinConfirm = ""
        pinError = ""
        showSetPin = true
    }

    private func closeSetPin() {
        showSetPin = false
    }

    private func confirmSetPin() {
        let pin = pinEntry
        guard pin.count >= 4 && pin.count <= 6, pin.allSatisfy(\.isNumber) else {
            pinError = "PIN phải có 4–6 chữ số"
            // Re-open so the user sees the error (alert dismisses on button tap).
            reopenWithError()
            return
        }
        guard pin == pinConfirm else {
            pinError = "PIN xác nhận không khớp"
            reopenWithError()
            return
        }
        settings.setPin(pin)
        settings.lockEnabled = true
    }

    private func reopenWithError() {
        // SwiftUI dismisses the alert on any button; re-present on the next runloop tick.
        DispatchQueue.main.async { showSetPin = true }
    }
}
