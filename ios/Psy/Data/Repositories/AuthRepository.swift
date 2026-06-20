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
