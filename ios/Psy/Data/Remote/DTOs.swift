import Foundation
struct GoogleLoginRequest: Encodable { let idToken: String }
struct TokenResponse: Decodable { let token: String }
struct BackupRequest: Encodable { let blob: String }
struct BackupResponse: Decodable { let version: Int; let blob: String; let updatedAt: String }
