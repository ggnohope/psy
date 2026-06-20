# iOS Port — Plan 5: Auth, Sync, Lock, Settings, App Gate

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`).

**Goal:** Complete the app: Google Sign-In (mandatory login gate), Keychain JWT storage, cloud backup/restore sync against the existing backend, app lock (PIN + biometric), Settings/Appearance, and the `AppRoot` gate with scene-phase auto-backup.

**Architecture:** New data layer pieces (`TokenStore` Keychain, `SettingsStore` UserDefaults, `APIClient`, `AuthAPI`/`BackupAPI`, `AuthRepository`, `BackupRepository`) added to `AppContainer`. An `@Observable AppViewModel` drives the gate: tri-state `isSignedIn` (token presence), `isLocked`, sign-in, logout, scene-phase hooks. `AppRoot` switches Loading→Login→Lock→app. Mirrors the Android `AppViewModel`/`AppRoot`/`AuthTokenStore`/`BackupRepositoryImpl`/`SettingsRepositoryImpl`.

**Tech Stack:** SwiftUI, Combine, URLSession async/await, Security (Keychain), LocalAuthentication, GoogleSignIn-iOS (SPM).

**Reference (port logic/UX):**
- `android/.../ui/app/{AppViewModel,AppRoot}.kt` (already the model for the gate)
- `android/.../data/auth/AuthTokenStore.kt`, `data/settings/SettingsRepositoryImpl.kt`, `data/repo/{AuthRepositoryImpl,BackupRepositoryImpl}.kt`, `data/remote/dto/*.kt`
- `android/.../ui/auth/{LoginScreen,GoogleSignIn}.kt`, `ui/lock/{LockScreen,BiometricAuthenticator}.kt`, `ui/settings/{SettingsScreen,AppearanceScreen,LockSettingsScreen}.kt`
- Spec §4.7, §4.8, §6.

**Prerequisites:** Plans 1,2,4a,4b landed. Backend (Plan 3 + EC2 `.env GOOGLE_CLIENT_IDS`) deployed. Manual: user creates the iOS OAuth client (placeholder used until then). Build verify: `cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`.

---

## File Structure
```
ios/Psy/Data/Auth/TokenStore.swift              # Keychain token + email + lastSyncAt
ios/Psy/Data/Settings/SettingsStore.swift       # UserDefaults theme/accent/lock/pin/biometric
ios/Psy/Data/Remote/APIClient.swift
ios/Psy/Data/Remote/DTOs.swift                  # GoogleLoginRequest/TokenResponse/BackupRequest/BackupResponse
ios/Psy/Data/Remote/AuthAPI.swift
ios/Psy/Data/Remote/BackupAPI.swift
ios/Psy/Data/Repositories/AuthRepository.swift
ios/Psy/Data/Repositories/BackupRepository.swift
ios/Psy/Features/Auth/GoogleSignInClient.swift
ios/Psy/Features/Auth/LoginView.swift
ios/Psy/Features/Lock/BiometricAuthenticator.swift
ios/Psy/Features/Lock/LockView.swift
ios/Psy/Features/Settings/SettingsView.swift
ios/Psy/Features/Settings/AppearanceView.swift
ios/Psy/Features/Settings/LockSettingsView.swift
ios/Psy/App/AppViewModel.swift
ios/Psy/App/AppRoot.swift
(modify) ios/Psy/App/AppContainer.swift, PsyApp.swift, RootView.swift, Features/Home/HomeView.swift, project.yml, Info.plist
```

---

## Task 1: Keychain TokenStore + SettingsStore

**Files:** Create `ios/Psy/Data/Auth/TokenStore.swift`, `ios/Psy/Data/Settings/SettingsStore.swift`.

- [ ] **Step 1: TokenStore (Keychain for token; UserDefaults for email + lastSyncAt — mirrors AuthTokenStore.kt)**

`ios/Psy/Data/Auth/TokenStore.swift`:
```swift
import Foundation
import Combine
import Security

@MainActor
final class TokenStore {
    private let service = "com.psy.auth"
    private let account = "jwt"
    private let emailKey = "psy.user.email"
    private let lastSyncKey = "psy.last.sync.at"

    let tokenSubject: CurrentValueSubject<String?, Never>

    init() {
        tokenSubject = CurrentValueSubject(TokenStore.keychainRead(service: "com.psy.auth", account: "jwt"))
    }

    var currentToken: String? { tokenSubject.value }
    var email: String? { UserDefaults.standard.string(forKey: emailKey) }

    func setAuth(token: String, email: String) {
        Self.keychainWrite(service: service, account: account, value: token)
        UserDefaults.standard.set(email, forKey: emailKey)
        tokenSubject.send(token)
    }

    func clearAuth() {
        Self.keychainDelete(service: service, account: account)
        UserDefaults.standard.removeObject(forKey: emailKey)
        tokenSubject.send(nil)
    }

    var lastSyncAt: Int64? {
        let v = UserDefaults.standard.object(forKey: lastSyncKey) as? NSNumber
        return v?.int64Value
    }
    func setLastSyncAt(_ millis: Int64) {
        UserDefaults.standard.set(NSNumber(value: millis), forKey: lastSyncKey)
    }

    // MARK: - Keychain helpers (generic password)
    private static func keychainWrite(service: String, account: String, value: String) {
        let data = Data(value.utf8)
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account]
        SecItemDelete(query as CFDictionary)
        var add = query
        add[kSecValueData as String] = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(add as CFDictionary, nil)
    }
    private static func keychainRead(service: String, account: String) -> String? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account,
                                     kSecReturnData as String: true,
                                     kSecMatchLimit as String: kSecMatchLimitOne]
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
    private static func keychainDelete(service: String, account: String) {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account]
        SecItemDelete(query as CFDictionary)
    }
}
```

- [ ] **Step 2: SettingsStore (UserDefaults + @Observable; PIN sha256 "psy_salt:" — mirrors SettingsRepositoryImpl.kt)**

`ios/Psy/Data/Settings/SettingsStore.swift`:
```swift
import Foundation
import CryptoKit

@MainActor @Observable
final class SettingsStore {
    private let d = UserDefaults.standard
    private enum K {
        static let mode = "theme_mode", accent = "accent_palette", lock = "lock_enabled"
        static let pin = "pin_hash", bio = "biometric_enabled"
    }

    var themeMode: ThemeMode { didSet { d.set(themeMode.rawValue, forKey: K.mode) } }
    var accent: AccentPalette { didSet { d.set(accent.rawValue, forKey: K.accent) } }
    var lockEnabled: Bool { didSet { d.set(lockEnabled, forKey: K.lock) } }
    var biometricEnabled: Bool { didSet { d.set(biometricEnabled, forKey: K.bio) } }
    private(set) var pinHash: String?

    init() {
        themeMode = ThemeMode(rawValue: d.string(forKey: K.mode) ?? "") ?? .system
        accent = AccentPalette(rawValue: d.string(forKey: K.accent) ?? "") ?? .candyViolet
        lockEnabled = d.bool(forKey: K.lock)
        biometricEnabled = d.bool(forKey: K.bio)
        pinHash = d.string(forKey: K.pin)
    }

    func setPin(_ pin: String) { pinHash = Self.hash(pin); d.set(pinHash, forKey: K.pin) }
    func clearPin() { pinHash = nil; d.removeObject(forKey: K.pin) }
    func verifyPin(_ pin: String) -> Bool { pinHash != nil && pinHash == Self.hash(pin) }

    private static func hash(_ pin: String) -> String {
        let digest = SHA256.hash(data: Data("psy_salt:\(pin)".utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
```

- [ ] **Step 3: Verify** build. **Step 4: Commit** `feat(ios): Keychain TokenStore + UserDefaults SettingsStore`.

---

## Task 2: APIClient + DTOs + AuthAPI + BackupAPI

**Files:** Create `ios/Psy/Data/Remote/{APIClient,DTOs,AuthAPI,BackupAPI}.swift`.

- [ ] **Step 1: DTOs (match backend JSON — mirrors AuthDtos.kt/BackupDtos.kt)**

`ios/Psy/Data/Remote/DTOs.swift`:
```swift
import Foundation
struct GoogleLoginRequest: Encodable { let idToken: String }
struct TokenResponse: Decodable { let token: String }
struct BackupRequest: Encodable { let blob: String }
struct BackupResponse: Decodable { let version: Int; let blob: String; let updatedAt: String }
```

- [ ] **Step 2: APIClient (URLSession async; Bearer; base URL from BuildConfig)**

`ios/Psy/Data/Remote/APIClient.swift`:
```swift
import Foundation

enum APIError: Error { case http(Int), noData }

struct APIClient {
    let baseURL: URL
    let tokenProvider: @Sendable () -> String?

    init(baseURLString: String, tokenProvider: @escaping @Sendable () -> String?) {
        self.baseURL = URL(string: baseURLString)!
        self.tokenProvider = tokenProvider
    }

    /// POST JSON, decode response. `authed` adds the Bearer header.
    func post<B: Encodable, R: Decodable>(_ path: String, body: B, authed: Bool) async throws -> R {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authed, let t = tokenProvider() { req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization") }
        req.httpBody = try JSONEncoder().encode(body)
        let (data, resp) = try await URLSession.shared.data(for: req)
        try ensureOK(resp)
        return try JSONDecoder().decode(R.self, from: data)
    }

    /// GET; returns nil on 204. `R` decoded otherwise.
    func get<R: Decodable>(_ path: String, authed: Bool) async throws -> R? {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        if authed, let t = tokenProvider() { req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization") }
        let (data, resp) = try await URLSession.shared.data(for: req)
        if let http = resp as? HTTPURLResponse, http.statusCode == 204 { return nil }
        try ensureOK(resp)
        if data.isEmpty { return nil }
        return try JSONDecoder().decode(R.self, from: data)
    }

    private func ensureOK(_ resp: URLResponse) throws {
        guard let http = resp as? HTTPURLResponse else { throw APIError.noData }
        guard (200..<300).contains(http.statusCode) else { throw APIError.http(http.statusCode) }
    }
}
```

- [ ] **Step 3: AuthAPI + BackupAPI**

`ios/Psy/Data/Remote/AuthAPI.swift`:
```swift
import Foundation
struct AuthAPI {
    let client: APIClient
    func googleLogin(idToken: String) async throws -> TokenResponse {
        try await client.post("auth/google", body: GoogleLoginRequest(idToken: idToken), authed: false)
    }
}
```
`ios/Psy/Data/Remote/BackupAPI.swift`:
```swift
import Foundation
struct BackupAPI {
    let client: APIClient
    func upload(blob: String) async throws {
        struct Empty: Decodable {}
        // Backend returns JSON on success; ignore the body shape.
        _ = try? await client.post("backup", body: BackupRequest(blob: blob), authed: true) as Empty?
        // If the endpoint returns a non-decodable/empty body, treat as success via a raw call:
    }
    func download() async throws -> BackupResponse? {
        try await client.get("backup", authed: true)
    }
}
```
> Note: if `upload`'s `Empty` decode is unreliable, implement `upload` with a raw `post` variant that ignores the response body (add an `APIClient.postNoContent(_:body:authed:)` that only checks the status code). Prefer the raw variant — add it to APIClient and use it here.

- [ ] **Step 3a (apply): add `postNoContent` to APIClient and use it in `BackupAPI.upload`:**
```swift
    func postNoContent<B: Encodable>(_ path: String, body: B, authed: Bool) async throws {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authed, let t = tokenProvider() { req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization") }
        req.httpBody = try JSONEncoder().encode(body)
        let (_, resp) = try await URLSession.shared.data(for: req)
        try ensureOK(resp)
    }
```
Then `BackupAPI.upload` body becomes: `try await client.postNoContent("backup", body: BackupRequest(blob: blob), authed: true)`.

- [ ] **Step 4: Verify** build. **Step 5: Commit** `feat(ios): APIClient + auth/backup API + DTOs`.

---

## Task 3: AuthRepository + BackupRepository + AppContainer wiring

**Files:** Create `ios/Psy/Data/Repositories/{AuthRepository,BackupRepository}.swift`; modify `ios/Psy/App/AppContainer.swift`.

- [ ] **Step 1: AuthRepository (mirrors AuthRepositoryImpl.kt)**

`ios/Psy/Data/Repositories/AuthRepository.swift`:
```swift
import Foundation

@MainActor
final class AuthRepository {
    private let api: AuthAPI
    private let tokenStore: TokenStore
    init(api: AuthAPI, tokenStore: TokenStore) { self.api = api; self.tokenStore = tokenStore }

    /// Verify the Google ID token with the backend, store the JWT + email.
    func signInGoogle(idToken: String, email: String) async -> Result<Void, Error> {
        do {
            let resp = try await api.googleLogin(idToken: idToken)
            tokenStore.setAuth(token: resp.token, email: email)
            return .success(())
        } catch { return .failure(error) }
    }

    func signOut() { tokenStore.clearAuth() }
}
```

- [ ] **Step 2: BackupRepository (mirrors BackupRepositoryImpl.kt)**

`ios/Psy/Data/Repositories/BackupRepository.swift`:
```swift
import Foundation
import PsyCore

enum RestoreOutcome { case restored, noBackup }

@MainActor
final class BackupRepository {
    private let api: BackupAPI
    private let snapshot: SnapshotManager
    private let tokenStore: TokenStore
    private let seeder: DefaultDataSeeder
    init(api: BackupAPI, snapshot: SnapshotManager, tokenStore: TokenStore, seeder: DefaultDataSeeder) {
        self.api = api; self.snapshot = snapshot; self.tokenStore = tokenStore; self.seeder = seeder
    }

    func backupNow() async -> Result<Void, Error> {
        do {
            let blob = try snapshot.export()
            try await api.upload(blob: blob)
            tokenStore.setLastSyncAt(Int64(Date().timeIntervalSince1970 * 1000))
            return .success(())
        } catch { return .failure(error) }
    }

    func restore() async -> Result<RestoreOutcome, Error> {
        do {
            guard let resp = try await api.download() else { return .success(.noBackup) }
            try snapshot.importBlob(resp.blob)
            return .success(.restored)
        } catch { return .failure(error) }
    }

    /// After login: if local is empty, restore from cloud; if no cloud backup, seed defaults.
    func prepareLocalDataAfterLogin() async {
        guard snapshot.isLocalEmpty() else { return }
        if let resp = try? await api.download(), !resp.blob.isEmpty {
            try? snapshot.importBlob(resp.blob)
        } else {
            seeder.seedIfEmpty(now: Int64(Date().timeIntervalSince1970 * 1000))
        }
    }

    func wipeLocal() { snapshot.wipeLocal() }
}
```

- [ ] **Step 3: Wire into AppContainer** — add to `ios/Psy/App/AppContainer.swift`:
```swift
    let tokenStore: TokenStore
    let settingsStore: SettingsStore
    let authRepo: AuthRepository
    let backupRepo: BackupRepository
```
and in `init`, after the existing setup:
```swift
        tokenStore = TokenStore()
        settingsStore = SettingsStore()
        let apiClient = APIClient(baseURLString: BuildConfig.baseURL,
                                  tokenProvider: { [tokenStore] in tokenStore.currentToken })
        authRepo = AuthRepository(api: AuthAPI(client: apiClient), tokenStore: tokenStore)
        backupRepo = BackupRepository(api: BackupAPI(client: apiClient), snapshot: snapshotManager,
                                      tokenStore: tokenStore, seeder: seeder)
```
> `tokenProvider` reads `currentToken` lazily on each request (the captured `tokenStore` is the container's instance). Since `APIClient.tokenProvider` is `@Sendable () -> String?` and `TokenStore` is `@MainActor`, mark the closure access acceptably (app is Swift 5 mode; if the compiler complains, drop `@Sendable` from `APIClient.tokenProvider`).

- [ ] **Step 4: Verify** build. **Step 5: Commit** `feat(ios): Auth + Backup repositories wired into AppContainer`.

---

## Task 4: GoogleSignIn package + LoginView

**Files:** modify `ios/project.yml`, `ios/Psy/Info.plist`; create `ios/Psy/Features/Auth/{GoogleSignInClient,LoginView}.swift`.

- [ ] **Step 1: Add GoogleSignIn SPM package to `ios/project.yml`** — under `packages:` add:
```yaml
  GoogleSignIn:
    url: https://github.com/google/GoogleSignIn-iOS
    from: "8.0.0"
```
and under the `Psy` target `dependencies:` add:
```yaml
      - package: GoogleSignIn
        product: GoogleSignIn
```

- [ ] **Step 2: Info.plist — GIDClientID + URL scheme (PLACEHOLDERS; user replaces)** — add inside the top-level `<dict>`:
```xml
    <key>GIDClientID</key>
    <string>YOUR_IOS_CLIENT_ID.apps.googleusercontent.com</string>
    <key>GIDServerClientID</key>
    <string>885786271406-u79onredebj2udsoaen122vphe3pk8hd.apps.googleusercontent.com</string>
    <key>CFBundleURLTypes</key>
    <array>
        <dict>
            <key>CFBundleURLSchemes</key>
            <array>
                <string>com.googleusercontent.apps.YOUR_IOS_CLIENT_ID</string>
            </array>
        </dict>
    </array>
```
> The web client id is the real existing one (safe to commit). Replace `YOUR_IOS_CLIENT_ID` placeholders with the user's iOS client id when provided. Sign-in will not complete until then — the rest of the app builds and runs.

- [ ] **Step 3: GoogleSignInClient (wraps GIDSignIn; returns idToken + email)**

`ios/Psy/Features/Auth/GoogleSignInClient.swift`:
```swift
import Foundation
import UIKit
import GoogleSignIn

enum GoogleSignInError: Error { case noPresenter, noIDToken }

@MainActor
enum GoogleSignInClient {
    /// Presents Google Sign-In and returns (idToken, email). Uses GIDServerClientID from Info.plist
    /// so the backend (which validates against the web client id) accepts the token.
    static func signIn() async throws -> (idToken: String, email: String) {
        guard let root = Self.rootViewController() else { throw GoogleSignInError.noPresenter }
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: root)
        guard let idToken = result.user.idToken?.tokenString else { throw GoogleSignInError.noIDToken }
        let email = result.user.profile?.email ?? ""
        return (idToken, email)
    }

    private static func rootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?.rootViewController
    }
}
```
> If `GIDConfiguration`/`serverClientID` is not auto-read from `GIDClientID`/`GIDServerClientID` in Info.plist by the installed GoogleSignIn version, configure it explicitly in `PsyApp` init: `GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: <GIDClientID>, serverClientID: <GIDServerClientID>)` read from `Bundle.main`. Apply whichever the SDK version needs; note it.

- [ ] **Step 4: LoginView** — port `LoginScreen.kt`. A centered Candy Pop screen: app mascot "🐷"/title, a "Đăng nhập với Google" button → calls `vm.signInGoogle()` (the AppViewModel method from Task 6 triggers `GoogleSignInClient.signIn()` then `authRepo`), shows a spinner while signing in and an error message on failure. Takes the `AppViewModel` (Task 6). Build it now referencing `AppViewModel` (created in Task 6) — if implementing Task 4 before Task 6, stub the action to `await vm.signInGoogle()` matching the Task 6 API.

- [ ] **Step 5: Verify** build (SPM will resolve GoogleSignIn — first build downloads it; ensure network). **Step 6: Commit** `feat(ios): GoogleSignIn package + LoginView (placeholder client id)`.

---

## Task 5: BiometricAuthenticator + LockView

**Files:** Create `ios/Psy/Features/Lock/{BiometricAuthenticator,LockView}.swift`.

- [ ] **Step 1: BiometricAuthenticator (LocalAuthentication; mirrors BiometricAuthenticator.kt)**

`ios/Psy/Features/Lock/BiometricAuthenticator.swift`:
```swift
import Foundation
import LocalAuthentication

enum BiometricAuthenticator {
    static var isAvailable: Bool {
        var error: NSError?
        return LAContext().canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }
    static func authenticate(reason: String = "Mở khoá Psy") async -> Bool {
        let ctx = LAContext()
        return await withCheckedContinuation { cont in
            ctx.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { ok, _ in
                cont.resume(returning: ok)
            }
        }
    }
}
```

- [ ] **Step 2: LockView** — port `LockScreen.kt`. A PIN entry screen (numeric pad or secure field) calling `verifyPin`; if `biometricEnabled` and available, auto-prompt Face ID on appear and offer a biometric button. On success → `onUnlock()`. Candy Pop styling. Signature: `LockView(biometricEnabled: Bool, verifyPin: (String) -> Bool, onUnlock: () -> Void)`.

- [ ] **Step 3: Verify** build. **Step 4: Commit** `feat(ios): BiometricAuthenticator + LockView (PIN + Face ID)`.

---

## Task 6: AppViewModel + AppRoot gate + Settings + wiring

**Files:** Create `ios/Psy/App/{AppViewModel,AppRoot}.swift`, `ios/Psy/Features/Settings/{SettingsView,AppearanceView,LockSettingsView}.swift`; modify `ios/Psy/PsyApp.swift`, `ios/Psy/Features/Home/HomeView.swift`.

- [ ] **Step 1: AppViewModel (mirrors AppViewModel.kt)**

`ios/Psy/App/AppViewModel.swift`:
```swift
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
```

- [ ] **Step 2: AppRoot (mirrors AppRoot.kt) + scenePhase**

`ios/Psy/App/AppRoot.swift`:
```swift
import SwiftUI

struct AppRoot: View {
    let container: AppContainer
    @State private var vm: AppViewModel
    @Environment(\.scenePhase) private var scenePhase

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: AppViewModel(container: container))
    }

    var body: some View {
        Group {
            switch vm.isSignedIn {
            case .none:
                ProgressView()
            case .some(false):
                LoginView(vm: vm)
            case .some(true):
                if vm.isLocked {
                    LockView(biometricEnabled: vm.settings.biometricEnabled,
                             verifyPin: { vm.verifyPin($0) },
                             onUnlock: { vm.unlock() })
                } else {
                    RootView(container: container, appVM: vm)
                }
            }
        }
        .psyTheme(mode: vm.settings.themeMode, accent: vm.settings.accent)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { vm.onScenePhaseActive() }
            else if phase == .background { vm.onScenePhaseBackground() }
        }
    }
}
```

- [ ] **Step 3: Settings screens** — port `SettingsScreen.kt`, `AppearanceScreen.kt`, `LockSettingsScreen.kt`:
  - `SettingsView(container:appVM:)`: a list with sections — "Giao diện" → `NavigationLink` to `AppearanceView`; "Khoá ứng dụng" → `LockSettingsView`; "Quản lý" → `ManageAccountsView`/`ManageCategoriesView`; a "Đăng xuất" button → `appVM.logout()`. Show the signed-in email (`container.tokenStore.email`).
  - `AppearanceView(settings:)`: theme mode picker (System/Light/Dark) bound to `settings.themeMode`; accent palette picker (Candy Violet/Pink/Mint) bound to `settings.accent`. Live preview (theme updates immediately via `@Observable`).
  - `LockSettingsView(settings:)`: toggle "Khoá ứng dụng" (`settings.lockEnabled`); when enabling, prompt to set a PIN (`settings.setPin`); toggle biometric (`settings.biometricEnabled`, only enabled if a PIN is set and `BiometricAuthenticator.isAvailable`); option to change/clear PIN.
- [ ] **Step 4: RootView + HomeView wiring** — `RootView` gains `appVM: AppViewModel` and passes it where needed; the Home toolbar gear now opens `SettingsView` (replace the temporary Manage `Menu` from Plan 4b with a `NavigationLink`/sheet to `SettingsView(container:appVM:)`). Update `RootView(container:appVM:)` signature and its callers.
- [ ] **Step 5: PsyApp** — replace body with the gate:
```swift
import SwiftUI
import GoogleSignIn

@main
struct PsyApp: App {
    @State private var container = AppContainer()
    var body: some Scene {
        WindowGroup {
            AppRoot(container: container)
                .task {
                    container.seeder.seedIfEmpty(now: Int64(Date().timeIntervalSince1970 * 1000))
                    SampleData.seedIfRequested(container, calendar: Calendar(identifier: .gregorian), now: Date())
                }
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
```
> Keep the `PSY_SKIP_AUTH` env (set in AppViewModel) so screenshots can bypass the login gate. The seed-on-launch stays for offline/demo; in a real signed-in flow `prepareLocalDataAfterLogin` handles restore-or-seed.

- [ ] **Step 6: Verify build + screenshots:**
  - Login gate (no token): launch WITHOUT `PSY_SKIP_AUTH` → screenshot `login.png` (shows the Google sign-in screen).
  - App + Settings (bypass auth): launch with `SIMCTL_CHILD_PSY_SKIP_AUTH=1 SIMCTL_CHILD_PSY_SAMPLE_DATA=1` → from Home open Settings; screenshot `settings.png` and `appearance.png` if reachable (Settings at least).
  Build: `cd ios && xcodegen generate && xcodebuild ... build` → `** BUILD SUCCEEDED **`.
  ```bash
  APP=$(xcodebuild -project ios/Psy.xcodeproj -scheme Psy -showBuildSettings -destination 'platform=iOS Simulator,name=iPhone 17' 2>/dev/null | awk -F' = ' '/ TARGET_BUILD_DIR /{d=$2} / FULL_PRODUCT_NAME /{p=$2} END{print d"/"p}')
  xcrun simctl boot "iPhone 17" 2>/dev/null; open -a Simulator 2>/dev/null
  xcrun simctl uninstall booted com.psy 2>/dev/null; xcrun simctl install booted "$APP"
  xcrun simctl launch booted com.psy; sleep 4
  xcrun simctl io booted screenshot /private/tmp/claude-501/-Users-hoalam-Codes-psy/92a0178c-85c8-4058-a0d6-f7ded334a8e4/scratchpad/login.png
  ```
- [ ] **Step 7: Commit** `feat(ios): AppViewModel + AppRoot login/lock gate + Settings + scene-phase auto-backup`.

---

## Self-Review (completed during authoring)
- §4.8 gate/tri-state/scenePhase/auto-backup → Tasks 6. §4.7 settings/appearance/lock → Tasks 1,5,6. §6 Google config/base URL → Tasks 2,4. Sync (backup/restore/prepare) → Task 3. Keychain → Task 1.
- Placeholders: the OAuth client id is an intentional, documented placeholder (manual user step); everything else is concrete. The GoogleSignIn config + upload-no-content notes are explicit "apply this variant" guards.
- Type consistency: `AppViewModel` API (`signInGoogle`, `onScenePhaseActive/Background`, `unlock`, `verifyPin`, `settings`, `isSignedIn`, `isLocked`, `signingIn`, `message`) used by `AppRoot`/`LoginView`/`LockView`. `RootView(container:appVM:)` updated everywhere. `TokenStore`/`SettingsStore`/`AuthRepository`/`BackupRepository` names consistent across AppContainer + repos. DTO field names match the backend.

## Execution Handoff
After Plan 5 the app is feature-complete. Remaining real-world step: user supplies the iOS OAuth client id (Info.plist placeholders) to enable live Google login; everything else builds, runs, and is screenshot-verified. Then finishing-a-development-branch (PR into main).
