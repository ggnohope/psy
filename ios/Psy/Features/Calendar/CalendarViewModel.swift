import Foundation
import Combine
import PsyCore

/// Calendar screen ViewModel. Mirrors `CalendarViewModel.kt`:
/// a month subject + a selected-day subject feed the CalendarEngine, which builds the
/// Monday-start grid and the selected-day transaction rows. Canonical Combine pattern.
@MainActor @Observable
final class CalendarViewModel {
    private let container: AppContainer
    let calendar: Calendar
    private var cancellables = Set<AnyCancellable>()

    private let monthSubject: CurrentValueSubject<PsyMonth, Never>
    private let selectedDaySubject = CurrentValueSubject<Date?, Never>(nil)

    var monthLabel = ""
    var weeks: [[DayCell?]] = []
    var dayRows: [TxRow] = []
    var selectedDay: Date?
    var currency: Currency = .vnd
    var loading = true

    init(container: AppContainer) {
        self.container = container
        var c = Calendar(identifier: .gregorian); c.timeZone = .current
        self.calendar = c
        let now = Date()
        self.monthSubject = CurrentValueSubject(PsyMonth.current(c, now: now))
        self.monthLabel = monthSubject.value.label
        start(now: now)
    }

    private func start(now: Date) {
        let cal = calendar
        let c = container
        let monthPub = monthSubject
        let selDayPub = selectedDaySubject

        c.ledgerRepo.observeAll()
            .combineLatest(monthPub)
            .map { ledgers, month -> AnyPublisher<(Currency, PsyMonth, CalendarResult)?, Never> in
                guard let ledger = ledgers.first else { return Just(nil).eraseToAnyPublisher() }
                let currency = Currency.of(ledger.currency)
                let start = month.startMillis(cal), end = month.endMillis(cal)
                return Publishers.CombineLatest4(
                    c.transactionRepo.observeBetween(ledgerId: ledger.id, start: start, end: end),
                    c.categoryRepo.observeAll(),
                    c.accountRepo.observeAll(),
                    selDayPub
                )
                .map { txns, cats, accts, selDay in
                    let result = CalendarEngine.build(
                        monthTransactions: txns, month: month, categories: cats, accounts: accts,
                        selectedDay: selDay, calendar: cal, now: now
                    )
                    return (currency, month, result)
                }
                .eraseToAnyPublisher()
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] result in
                guard let self else { return }
                if let (currency, month, r) = result {
                    self.currency = currency
                    self.monthLabel = month.label
                    self.weeks = r.weeks
                    self.dayRows = r.dayRows
                    self.selectedDay = selDayPub.value
                } else {
                    self.weeks = []
                    self.dayRows = []
                }
                self.loading = false
            }
            .store(in: &cancellables)
    }

    func prevMonth() {
        selectedDaySubject.value = nil
        monthSubject.value = monthSubject.value.adding(-1, calendar)
    }

    func nextMonth() {
        selectedDaySubject.value = nil
        monthSubject.value = monthSubject.value.adding(1, calendar)
    }

    func selectDay(_ date: Date) {
        selectedDaySubject.value = date
    }
}
