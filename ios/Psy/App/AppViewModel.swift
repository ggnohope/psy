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

    private var hasResolvedInitial = false
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

    func onScenePhaseActive() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if settings.lockEnabled {
            let elapsed = now - lastBackgroundedAt
            if !hasResolvedInitial || elapsed > 2000 { isLocked = true }
        }
        hasResolvedInitial = true
    }

    func onScenePhaseBackground() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
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
