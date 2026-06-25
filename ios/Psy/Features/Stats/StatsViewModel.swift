import Foundation
import Combine
import PsyCore

/// Stats screen ViewModel. Mirrors `StatsViewModel.kt`:
/// three subjects (month, pieMode, accountFilter) feed the StatsEngine, which builds the
/// summary, pie slices, top entries, 6-month trend and per-account breakdown.
/// The window query spans [month-5, month+1) so the trend chart can be derived in one query.
/// Canonical Combine pattern (nested CombineLatest because the inner combine has 5 inputs).
@MainActor @Observable
final class StatsViewModel {
    private let container: AppContainer
    let calendar: Calendar
    private var cancellables = Set<AnyCancellable>()

    private let monthSubject: CurrentValueSubject<PsyMonth, Never>
    private let pieModeSubject = CurrentValueSubject<TxType, Never>(.expense)
    private let accountFilterSubject = CurrentValueSubject<Int64?, Never>(nil)

    // State mirroring StatsResult + presentation extras.
    var monthLabel = ""
    var currency: Currency = .vnd
    var summary = StatsSummary(incomeMinor: 0, expenseMinor: 0, netMinor: 0, avgPerDayMinor: 0)
    var pieMode: TxType = .expense
    var slices: [PieSlice] = []
    var top: [TopGroup] = []
    var trend: [MonthBars] = []
    var accountBreakdown: [AccountStat] = []
    var selectedAccountId: Int64?
    var accounts: [Account] = []
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
        let pieModePub = pieModeSubject
        let filterPub = accountFilterSubject

        c.ledgerRepo.observeAll()
            .combineLatest(monthPub)
            .map { ledgers, month -> AnyPublisher<(Currency, PsyMonth, StatsResult)?, Never> in
                guard let ledger = ledgers.first else { return Just(nil).eraseToAnyPublisher() }
                let currency = Currency.of(ledger.currency)
                // 6-month trend window: [month-5, month+1)
                let trendStart = month.adding(-5, cal).startMillis(cal)
                let monthEnd = month.endMillis(cal)

                // Inner combine has inputs → nest CombineLatest4 + CombineLatest.
                // cats + groups are paired into one slot to stay within CombineLatest4.
                let catsAndGroups = c.categoryRepo.observeAll().combineLatest(c.categoryGroupRepo.observeAll())
                let repos = Publishers.CombineLatest4(
                    c.transactionRepo.observeBetween(ledgerId: ledger.id, start: trendStart, end: monthEnd),
                    catsAndGroups,
                    c.accountRepo.observeAll(),
                    pieModePub
                )
                return Publishers.CombineLatest(repos, filterPub)
                    .map { combined, accountFilter in
                        let (windowTxns, cg, accts, mode) = combined
                        let result = StatsEngine.build(
                            windowTransactions: windowTxns, categories: cg.0, groups: cg.1, accounts: accts,
                            month: month, pieMode: mode, accountFilter: accountFilter,
                            calendar: cal, now: now
                        )
                        return (currency, month, result)
                    }
                    .eraseToAnyPublisher()
            }
            .switchToLatest()
            .receive(on: RunLoop.main)
            .sink { [weak self] payload in
                guard let self else { return }
                if let (currency, month, r) = payload {
                    self.currency = currency
                    self.monthLabel = month.label
                    self.summary = r.summary
                    self.pieMode = r.pieMode
                    self.slices = r.slices
                    self.top = r.top
                    self.trend = r.trend
                    self.accountBreakdown = r.accountBreakdown
                    self.selectedAccountId = r.selectedAccountId
                    // accounts list for the filter chips comes straight from the breakdown source;
                    // keep it in sync via a dedicated subscription below.
                } else {
                    self.summary = StatsSummary(incomeMinor: 0, expenseMinor: 0, netMinor: 0, avgPerDayMinor: 0)
                    self.slices = []
                    self.top = []
                    self.trend = []
                    self.accountBreakdown = []
                    self.selectedAccountId = nil
                }
                self.loading = false
            }
            .store(in: &cancellables)

        // Accounts for the filter chips (independent of month/filter).
        c.accountRepo.observeAll()
            .receive(on: RunLoop.main)
            .sink { [weak self] accts in self?.accounts = accts }
            .store(in: &cancellables)
    }

    func prevMonth() { monthSubject.value = monthSubject.value.adding(-1, calendar) }
    func nextMonth() { monthSubject.value = monthSubject.value.adding(1, calendar) }

    func setPieMode(_ type: TxType) { pieModeSubject.value = type }

    func selectAccount(_ id: Int64?) { accountFilterSubject.value = id }
}
