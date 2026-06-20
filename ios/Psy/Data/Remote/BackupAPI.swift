import Foundation
struct BackupAPI {
    let client: APIClient
    func upload(blob: String) async throws {
        try await client.postNoContent("backup", body: BackupRequest(blob: blob), authed: true)
    }
    func download() async throws -> BackupResponse? {
        try await client.get("backup", authed: true)
    }
}
