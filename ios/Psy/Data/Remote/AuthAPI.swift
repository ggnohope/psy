import Foundation
struct AuthAPI {
    let client: APIClient
    func googleLogin(idToken: String) async throws -> TokenResponse {
        try await client.post("auth/google", body: GoogleLoginRequest(idToken: idToken), authed: false)
    }
}
