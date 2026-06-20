import SwiftUI

/// Welcome / login gate. Google-only sign-in; no dev/email login. Ports `LoginScreen.kt`.
/// On success `AppViewModel.signInGoogle()` flips `isSignedIn` and opens the gate.
struct LoginView: View {
    let vm: AppViewModel
    @Environment(\.psyColors) private var psyColors

    var body: some View {
        ZStack {
            psyColors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                Text("🐷").font(.system(size: 64))
                Spacer().frame(height: 16)
                Text("Psy")
                    .font(PsyFont.headlineMedium)
                    .foregroundStyle(psyColors.primary)
                Spacer().frame(height: 8)
                Text("Ghi chép chi tiêu dễ thương")
                    .font(PsyFont.bodyMedium)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(psyColors.onSurface.opacity(0.6))

                Spacer().frame(height: 48)

                Button(action: { vm.signInGoogle() }) {
                    HStack(spacing: 8) {
                        if vm.signingIn {
                            ProgressView()
                                .progressViewStyle(.circular)
                                .tint(.white)
                        }
                        Text("Đăng nhập với Google")
                            .font(PsyFont.titleMedium)
                            .foregroundStyle(.white)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: CandyShape.large)
                            .fill(psyColors.primary)
                    )
                }
                .disabled(vm.signingIn)

                if let message = vm.message {
                    Spacer().frame(height: 16)
                    Text(message)
                        .font(PsyFont.bodyMedium)
                        .foregroundStyle(CandyColor.pinkDeep)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 32)
        }
    }
}
