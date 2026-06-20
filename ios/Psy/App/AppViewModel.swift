import Foundation
import Combine
import SwiftUI

@MainActor @Observable
final class AppViewModel {
    private let container: AppContainer
    private var cancellables = Set<AnyCancellable>()
    private let autoBackupThrottleMs: Int64 = 5 * 60 * 1000

    // Tri-state: nil = loading, false = login, true = signed in.
    var isSignedIn: Bool?
    var isLocked: Bool = false
    var signingIn: Bool = false
    var message: String?

    var settings: SettingsStore { container.settingsStore }

    private var didEnterBackground = false
    private var lastBackgroundedAt: Int64 = 0
    private let skipAuth = ProcessInfo.processInfo.environment["PSY_SKIP_AUTH"] == "1"

    init(container: AppContainer) {
        self.container = container
        if skipAuth { isSignedIn = true }
        else {
            container.tokenStore.tokenSubject
                .map { $0 != nil }
                .receive(on: RunLoop.main)
                .sink { [weak self] v in self?.isSignedIn = v }
                .store(in: &cancellables)
        }
        if container.settingsStore.lockEnabled { isLocked = true }
        // Debug/screenshot hook: force the lock screen (paired with PSY_SKIP_AUTH).
        if ProcessInfo.processInfo.environment["PSY_PREVIEW_LOCK"] == "1" { isSignedIn = true; isLocked = true }
    }

    func signInGoogle() {
        signingIn = true
        Task {
            do {
                let (idToken, email) = try await GoogleSignInClient.signIn()
                let result = await container.authRepo.signInGoogle(idToken: idToken, email: email)
                switch result {
                case .success:
                    message = nil
                    await container.backupRepo.prepareLocalDataAfterLogin()
                case .failure(let e):
                    message = "Lỗi đăng nhập: \(e.localizedDescription)"
                }
            } catch { message = "Lỗi đăng nhập Google: \(error.localizedDescription)" }
            signingIn = false
        }
    }

    /// Re-lock ONLY when returning from a real background that lasted > 2s. The Face ID /
    /// system prompt drives the app `.inactive` → `.active` WITHOUT a `.background`, so it must
    /// not re-lock here — otherwise unlock → re-lock → re-prompt loops forever. The initial
    /// lock on cold launch is set in `init()`. Mirrors Android, where the in-activity
    /// BiometricPrompt never triggers ON_STOP (so no spurious re-lock).
    func onScenePhaseActive() {
        defer { didEnterBackground = false }
        guard settings.lockEnabled, didEnterBackground else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if now - lastBackgroundedAt > 2000 { isLocked = true }
    }

    func onScenePhaseBackground() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        didEnterBackground = true
        lastBackgroundedAt = now
        guard container.tokenStore.currentToken != nil else { return }
        let last = container.tokenStore.lastSyncAt ?? 0
        if now - last > autoBackupThrottleMs {
            Task { _ = await container.backupRepo.backupNow() }
        }
    }

    func logout() {
        Task {
            _ = await container.backupRepo.backupNow()
            container.backupRepo.wipeLocal()
            container.authRepo.signOut()
        }
    }

    func unlock() { isLocked = false }
    func verifyPin(_ pin: String) -> Bool { settings.verifyPin(pin) }
}
