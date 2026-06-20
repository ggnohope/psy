import Foundation
import Combine
import PsyCore

@MainActor @Observable
final class HomeViewModel {
    private let container: AppContainer
    let calendar: Calendar
    private var cancellables = Set<AnyCancellable>()

    var monthLabel = ""
    var incomeMinor: Int64 = 0
    var expenseMinor: Int64 = 0
    var netMinor: Int64 = 0
    var currency: Currency = .vnd
    var days: [DayGroup] = []
    var loading = true

    init(container: AppContainer) {
        self.container = container
        var c = Calendar(identifier: .gregorian); c.timeZone = .current
        self.calendar = c
        start()
    }

    private func start() {
        let now = Date()
        let month = PsyMonth.current(calendar, now: now)
        monthLabel = month.label
        let start = month.startMillis(calendar), end = month.endMillis(calendar)
        let cal = calendar
        let c = container

        c.ledgerRepo.observeAll()
            .map { ledgers -> AnyPublisher<(Currency, HomeResult)?, Never> in
                guard let ledger = ledgers.first else { return Just(nil).eraseToAnyPublisher() }
                let currency = Currency.of(ledger.currency)
                return Publishers.CombineLatest3(
                    c.transactionRepo.observeBetween(ledgerId: ledger.id, start: start, end: end),
                    c.categoryRepo.observeAll(),
                    c.accountRepo.observeAll()
                )
                .map { txns, cats, accts in
                    (currency, HomeEngine.build(transactions: txns, categories: cats, accounts: accts, calendar: cal, now: now))
                }
                .eraseToAnyPublisher()
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] result in
                guard let self else { return }
                if let (currency, r) = result {
                    self.currency = currency
                    self.incomeMinor = r.incomeMinor
                    self.expenseMinor = r.expenseMinor
                    self.netMinor = r.netMinor
                    self.days = r.days
                }
                self.loading = false
            }
            .store(in: &cancellables)
    }
}
