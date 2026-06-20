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
