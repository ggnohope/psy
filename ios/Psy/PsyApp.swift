import SwiftUI

@main
struct PsyApp: App {
    @State private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView(container: container)
                .psyTheme(mode: .system, accent: .candyViolet)
                .task {
                    // Offline-first: ensure default data exists on first launch (Plan 5 moves this
                    // behind the login gate via restore-or-seed).
                    container.seeder.seedIfEmpty(now: Int64(Date().timeIntervalSince1970 * 1000))
                }
        }
    }
}
