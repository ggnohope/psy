import Foundation
import Combine
import PsyCore

/// Screenshot-only sample-data injector. Runs only when launched with the environment variable
/// `PSY_SAMPLE_DATA=1` AND the current month has no transactions yet. NOT used in production flows.
@MainActor
enum SampleData {
    static func seedIfRequested(_ container: AppContainer, calendar: Calendar, now: Date) {
        guard ProcessInfo.processInfo.environment["PSY_SAMPLE_DATA"] == "1" else { return }
        guard let ledger = container.ledgerRepo.firstOrNull() else { return }

        let month = PsyMonth.current(calendar, now: now)

        // Guard: only seed if the CURRENT month is empty (avoids duplicate seeds on relaunch).
        if !currentMonthEmpty(container, ledgerId: ledger.id, month: month, calendar: calendar) { return }

        let accounts = container.accountRepo.all()
        let categories = container.categoryRepo.all()
        guard !accounts.isEmpty, !categories.isEmpty else { return }

        let cash = accounts.first { $0.name == "Tiền mặt" } ?? accounts[0]
        let bank = accounts.first { $0.name == "Ngân hàng" } ?? accounts[min(1, accounts.count - 1)]

        func expenseCat(_ name: String) -> PsyCore.Category? {
            categories.first { $0.type == .expense && $0.name == name }
        }
        func incomeCat(_ name: String) -> PsyCore.Category? {
            categories.first { $0.type == .income && $0.name == name }
        }
        let anyExpense = categories.first { $0.type == .expense }
        let anyIncome = categories.first { $0.type == .income }

        let nowMillis = Int64(now.timeIntervalSince1970 * 1000)

        // Build a date in a given month offset, on a given day-of-month.
        func date(monthOffset: Int, day: Int) -> Int64 {
            let m = month.adding(monthOffset, calendar)
            let clamped = min(day, m.lengthOfMonth(calendar))
            return Int64(m.atDay(clamped, calendar).timeIntervalSince1970 * 1000)
        }

        func insert(type: TxType, amount: Int64, category: PsyCore.Category?, account: Account,
                    to: Account? = nil, note: String, date dateMillis: Int64) {
            let tx = Transaction(
                ledgerId: ledger.id, type: type, amountMinor: amount,
                categoryId: type == .transfer ? nil : category?.id,
                accountId: account.id, toAccountId: type == .transfer ? to?.id : nil,
                note: note, date: dateMillis, createdAt: nowMillis, updatedAt: nowMillis)
            container.transactionRepo.upsert(tx)
        }

        // ---- Current month: spread of expenses, incomes, and a transfer ----
        insert(type: .expense, amount: 85_000, category: expenseCat("Ăn uống") ?? anyExpense, account: cash, note: "Cơm trưa", date: date(monthOffset: 0, day: 2))
        insert(type: .expense, amount: 32_000, category: expenseCat("Di chuyển") ?? anyExpense, account: cash, note: "Grab", date: date(monthOffset: 0, day: 4))
        insert(type: .expense, amount: 450_000, category: expenseCat("Mua sắm") ?? anyExpense, account: bank, note: "Áo mới", date: date(monthOffset: 0, day: 6))
        insert(type: .expense, amount: 1_200_000, category: expenseCat("Hoá đơn") ?? anyExpense, account: bank, note: "Tiền nhà", date: date(monthOffset: 0, day: 8))
        insert(type: .expense, amount: 150_000, category: expenseCat("Giải trí") ?? anyExpense, account: cash, note: "Xem phim", date: date(monthOffset: 0, day: 10))
        insert(type: .income, amount: 15_000_000, category: incomeCat("Lương") ?? anyIncome, account: bank, note: "Lương tháng", date: date(monthOffset: 0, day: 1))
        insert(type: .income, amount: 500_000, category: incomeCat("Thưởng") ?? anyIncome, account: cash, note: "Thưởng nóng", date: date(monthOffset: 0, day: 5))
        insert(type: .transfer, amount: 2_000_000, category: nil, account: bank, to: cash, note: "Rút tiền mặt", date: date(monthOffset: 0, day: 7))

        // ---- Previous month ----
        insert(type: .income, amount: 15_000_000, category: incomeCat("Lương") ?? anyIncome, account: bank, note: "Lương", date: date(monthOffset: -1, day: 1))
        insert(type: .expense, amount: 1_200_000, category: expenseCat("Hoá đơn") ?? anyExpense, account: bank, note: "Tiền nhà", date: date(monthOffset: -1, day: 8))
        insert(type: .expense, amount: 620_000, category: expenseCat("Ăn uống") ?? anyExpense, account: cash, note: "Ăn ngoài", date: date(monthOffset: -1, day: 15))

        // ---- Two months ago ----
        insert(type: .income, amount: 15_000_000, category: incomeCat("Lương") ?? anyIncome, account: bank, note: "Lương", date: date(monthOffset: -2, day: 1))
        insert(type: .expense, amount: 1_200_000, category: expenseCat("Hoá đơn") ?? anyExpense, account: bank, note: "Tiền nhà", date: date(monthOffset: -2, day: 8))
        insert(type: .expense, amount: 900_000, category: expenseCat("Mua sắm") ?? anyExpense, account: cash, note: "Đồ dùng", date: date(monthOffset: -2, day: 20))
    }

    /// Synchronous one-shot read of the current month's transactions (the publisher is backed by a
    /// CurrentValueSubject, so `.first()` emits immediately on the main actor — no blocking).
    private static func currentMonthEmpty(_ container: AppContainer, ledgerId: Int64,
                                          month: PsyMonth, calendar: Calendar) -> Bool {
        var isEmpty = true
        var cancellable: AnyCancellable?
        cancellable = container.transactionRepo
            .observeBetween(ledgerId: ledgerId, start: month.startMillis(calendar), end: month.endMillis(calendar))
            .first()
            .sink { isEmpty = $0.isEmpty }
        cancellable?.cancel()
        return isEmpty
    }
}
