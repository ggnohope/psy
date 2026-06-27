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

    // Auto-backup gate: set to the token for which restore-or-seed has SUCCEEDED this
    // session. `backupNow` only runs when this matches the current token, so a freshly
    // wiped/empty local state can never overwrite the cloud backup before it is restored.
    private var preparedToken: String?
    private var isPreparing = false

    init(container: AppContainer) {
        self.container = container
        if skipAuth { isSignedIn = true }
        else {
            container.tokenStore.tokenSubject
                .receive(on: RunLoop.main)
                .sink { [weak self] token in
                    guard let self else { return }
                    self.isSignedIn = (token != nil)
                    if let token {
                        // Cold launch with an existing Keychain token also lands here (not just
                        // explicit sign-in): restore-if-empty BEFORE any auto-backup can fire.
                        Task { await self.prepareLocalData(for: token) }
                    } else {
                        self.preparedToken = nil
                    }
                }
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
                    if let token = container.tokenStore.currentToken {
                        await prepareLocalData(for: token)
                    }
                case .failure(let e):
                    message = "Lỗi đăng nhập: \(e.localizedDescription)"
                }
            } catch { message = "Lỗi đăng nhập Google: \(error.localizedDescription)" }
            signingIn = false
        }
    }

    /// Restore-or-seed local data for the given token, then open the auto-backup gate
    /// (`preparedToken`) only if it succeeded. Idempotent + single-flight per token, so the
    /// cold-launch path and the explicit sign-in path can both call it without double work.
    private func prepareLocalData(for token: String) async {
        guard preparedToken != token, !isPreparing else { return }
        isPreparing = true
        defer { isPreparing = false }
        if await container.backupRepo.prepareLocalDataAfterLogin() {
            preparedToken = token
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
        guard let token = container.tokenStore.currentToken else { return }
        // Never auto-backup until restore-or-seed has succeeded this session, else an empty/
        // freshly-wiped local state would overwrite the good cloud backup.
        guard preparedToken == token else { return }
        let last = container.tokenStore.lastSyncAt ?? 0
        if now - last > autoBackupThrottleMs {
            Task { _ = await container.backupRepo.backupNow() }
        }
    }

    func logout() {
        Task {
            // Only push a final backup if local data is in a known-good (prepared) state.
            if preparedToken == container.tokenStore.currentToken {
                _ = await container.backupRepo.backupNow()
            }
            container.backupRepo.wipeLocal()
            container.authRepo.signOut()
        }
    }

    func unlock() { isLocked = false }
    func verifyPin(_ pin: String) -> Bool { settings.verifyPin(pin) }
}
