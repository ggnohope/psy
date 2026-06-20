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
