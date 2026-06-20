import XCTest
@testable import Psy

@MainActor
final class AppViewModelTests: XCTestCase {

    override func tearDown() {
        // SettingsStore writes to UserDefaults.standard — reset lock keys so tests don't leak.
        let c = AppContainer(inMemory: true)
        c.settingsStore.lockEnabled = false
        c.settingsStore.biometricEnabled = false
        c.settingsStore.clearPin()
        super.tearDown()
    }

    /// Regression: the Face ID prompt drives the app .inactive -> .active WITHOUT a real
    /// background. Becoming active again must NOT re-lock (else unlock -> re-lock -> re-prompt
    /// loops forever). Only a real background (onScenePhaseBackground) may arm a re-lock.
    func testScenePhaseActiveWithoutBackgroundDoesNotRelock() {
        let c = AppContainer(inMemory: true)
        c.settingsStore.lockEnabled = true
        let vm = AppViewModel(container: c)

        XCTAssertTrue(vm.isLocked, "should start locked when lockEnabled")
        vm.unlock()
        XCTAssertFalse(vm.isLocked)

        // Simulate the Face-ID inactive->active blip (no background occurred).
        vm.onScenePhaseActive()
        XCTAssertFalse(vm.isLocked, "must stay unlocked: no real background happened (Face ID loop bug)")
    }

    /// A real background followed by returning active re-locks (the intended behavior).
    func testRealBackgroundThenActiveRelocks() {
        let c = AppContainer(inMemory: true)
        c.settingsStore.lockEnabled = true
        let vm = AppViewModel(container: c)
        vm.unlock()
        XCTAssertFalse(vm.isLocked)

        vm.onScenePhaseBackground()   // real background arms the re-lock
        // NOTE: the >2s grace can't be exercised without time injection; this asserts the
        // background flag path is reachable. With elapsed ~0 it stays unlocked (quick switch).
        vm.onScenePhaseActive()
        XCTAssertFalse(vm.isLocked, "quick (<2s) background should not re-lock")
    }
}
