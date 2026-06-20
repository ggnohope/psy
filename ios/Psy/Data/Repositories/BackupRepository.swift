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
