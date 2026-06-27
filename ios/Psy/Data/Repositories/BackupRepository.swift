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

    /// After login (or cold launch with an existing token): if local is empty, restore from
    /// cloud; if the server confirms no backup (204), seed defaults.
    ///
    /// Returns `true` when local data is in a known-good state that is SAFE to auto-backup
    /// later. Returns `false` when we could not reach the server to confirm (network/server
    /// error) — the caller MUST NOT auto-backup in that case, otherwise an empty/seed local
    /// state could overwrite a good cloud backup we simply failed to download.
    @discardableResult
    func prepareLocalDataAfterLogin() async -> Bool {
        guard snapshot.isLocalEmpty() else { return true }
        do {
            if let resp = try await api.download(), !resp.blob.isEmpty {
                try snapshot.importBlob(resp.blob)
            } else {
                // 204 / empty body → genuinely new user, safe to seed.
                seeder.seedIfEmpty(now: Int64(Date().timeIntervalSince1970 * 1000))
            }
            return true
        } catch {
            // Could not confirm cloud state: leave local untouched and keep backups gated.
            return false
        }
    }

    func wipeLocal() { snapshot.wipeLocal() }
}
