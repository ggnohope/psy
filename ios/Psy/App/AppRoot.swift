import SwiftUI

struct AppRoot: View {
    let container: AppContainer
    @State private var vm: AppViewModel
    @Environment(\.scenePhase) private var scenePhase

    init(container: AppContainer) {
        self.container = container
        _vm = State(initialValue: AppViewModel(container: container))
    }

    var body: some View {
        Group {
            switch vm.isSignedIn {
            case .none:
                ProgressView()
            case .some(false):
                LoginView(vm: vm)
            case .some(true):
                if vm.isLocked {
                    LockView(biometricEnabled: vm.settings.biometricEnabled,
                             verifyPin: { vm.verifyPin($0) },
                             onUnlock: { vm.unlock() })
                } else {
                    RootView(container: container, appVM: vm)
                }
            }
        }
        .psyTheme(mode: vm.settings.themeMode, accent: vm.settings.accent)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { vm.onScenePhaseActive() }
            else if phase == .background { vm.onScenePhaseBackground() }
        }
    }
}
