import SwiftUI

/// Welcome / login gate. Google-only sign-in; no dev/email login. Ports `LoginScreen.kt`.
/// On success `AppViewModel.signInGoogle()` flips `isSignedIn` and opens the gate.
struct LoginView: View {
    let vm: AppViewModel
    @Environment(\.psyColors) private var psyColors

    /// Drives the looping teal glow behind the logo badge. Only animation in the view.
    @State private var pulse = false

    var body: some View {
        ZStack {
            psyColors.bg.ignoresSafeArea()

            VStack(spacing: 0) {
                logoBadge

                Spacer().frame(height: 28)

                EyebrowLabel(text: "Smart money tracking", color: psyColors.blue)

                Spacer().frame(height: 12)

                Text("Psy")
                    .font(PsyFont.display(46))
                    .foregroundStyle(psyColors.text)

                Spacer().frame(height: 8)

                Text("Ghi chép chi tiêu dễ thương")
                    .font(PsyFont.bodyLarge)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(psyColors.text2)

                Spacer().frame(height: 48)

                signInButton

                if let message = vm.message {
                    Spacer().frame(height: 16)
                    Text(message)
                        .font(PsyFont.bodyMedium)
                        .foregroundStyle(psyColors.red)
                        .multilineTextAlignment(.center)
                }

                Spacer().frame(height: 28)

                trustLine
            }
            .padding(.horizontal, 32)
        }
        .onAppear { pulse = true }
    }

    // MARK: - Logo badge + pulsing glow

    private var logoBadge: some View {
        ZStack {
            Circle()
                .fill(psyColors.teal)
                .frame(width: 104, height: 104)
                .blur(radius: 40)
                .scaleEffect(pulse ? 1.16 : 1.0)
                .opacity(pulse ? 0.2 : 0.5)
                .animation(
                    .easeInOut(duration: 3.2).repeatForever(autoreverses: true),
                    value: pulse
                )

            RoundedRectangle(cornerRadius: 26)
                .fill(
                    LinearGradient(
                        colors: [Color(argb: 0xFF103458), Color(argb: 0xFF061A30)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 104, height: 104)
                .overlay(LucideIcon(name: "shield-check", size: 52, tint: psyColors.teal))
        }
    }

    // MARK: - Primary sign-in button

    private var signInButton: some View {
        Button(action: { vm.signInGoogle() }) {
            HStack(spacing: 12) {
                if vm.signingIn {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                } else {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color.white)
                        .frame(width: 24, height: 24)
                        .overlay(
                            Text("G")
                                .font(PsyFont.labelLarge)
                                .foregroundStyle(psyColors.blue)
                        )
                }
                Text("Đăng nhập với Google")
                    .font(PsyFont.labelLarge)
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 17)
            .background(
                RoundedRectangle(cornerRadius: PsyRadius.button)
                    .fill(psyColors.blue)
            )
        }
        .disabled(vm.signingIn)
    }

    // MARK: - Bottom trust line

    private var trustLine: some View {
        HStack(spacing: 6) {
            LucideIcon(name: "lock", size: 14, tint: psyColors.text3)
            Text("Dữ liệu được mã hoá & lưu an toàn")
                .font(.system(size: 12))
                .foregroundStyle(psyColors.text3)
        }
    }
}
