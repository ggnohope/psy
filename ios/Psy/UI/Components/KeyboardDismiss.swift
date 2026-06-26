import SwiftUI
import UIKit

/// Installs a window-wide tap recognizer that dismisses the keyboard on any tap
/// outside a text field. `cancelsTouchesInView = false` + simultaneous recognition
/// means it never swallows taps meant for buttons, toggles, list rows, etc.
private final class KeyboardDismisser: NSObject, UIGestureRecognizerDelegate {
    static let shared = KeyboardDismisser()

    func install(on window: UIWindow) {
        let already = window.gestureRecognizers?.contains { $0.name == "psyKeyboardDismiss" } ?? false
        guard !already else { return }
        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        tap.name = "psyKeyboardDismiss"
        tap.cancelsTouchesInView = false
        tap.delegate = self
        window.addGestureRecognizer(tap)
    }

    @objc private func handleTap() {
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.windows.first(where: { $0.isKeyWindow }) }
            .first?
            .endEditing(true)
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer,
                           shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer) -> Bool { true }
}

/// Zero-size representable used only to grab the hosting `UIWindow` and install the recognizer.
private struct KeyboardDismissInstaller: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView { UIView(frame: .zero) }
    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            if let window = uiView.window { KeyboardDismisser.shared.install(on: window) }
        }
    }
}

extension View {
    /// Dismiss the keyboard when tapping anywhere outside the focused text field (app-wide).
    func dismissKeyboardOnTapOutside() -> some View {
        background(KeyboardDismissInstaller())
    }
}
