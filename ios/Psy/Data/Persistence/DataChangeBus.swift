import Foundation
import Combine

enum PsyTable {
    case ledgers, accounts, categories, transactions, budgets
}

/// Mirrors Room's per-DAO Flow emissions: each table has a subject that fires on every write.
/// Seeded with an initial value so a new subscriber immediately performs its first fetch.
@MainActor
final class DataChangeBus {
    private var subjects: [PsyTable: CurrentValueSubject<Void, Never>] = [:]

    func subject(_ table: PsyTable) -> CurrentValueSubject<Void, Never> {
        if let existing = subjects[table] { return existing }
        let created = CurrentValueSubject<Void, Never>(())
        subjects[table] = created
        return created
    }

    func notify(_ table: PsyTable) {
        subject(table).send(())
    }
}
