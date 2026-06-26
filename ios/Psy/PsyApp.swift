import SwiftUI
import GoogleSignIn

@main
struct PsyApp: App {
    @State private var container = AppContainer()
    var body: some Scene {
        WindowGroup {
            AppRoot(container: container)
                .dismissKeyboardOnTapOutside()
                .task {
                    container.seeder.seedIfEmpty(now: Int64(Date().timeIntervalSince1970 * 1000))
                    SampleData.seedIfRequested(container, calendar: Calendar(identifier: .gregorian), now: Date())
                }
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
