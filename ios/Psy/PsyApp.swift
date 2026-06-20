import SwiftUI

@main
struct PsyApp: App {
    var body: some Scene {
        WindowGroup {
            PlaceholderRootView()
        }
    }
}

private struct PlaceholderRootView: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("Psy 🐷")
                .font(.largeTitle.bold())
            Text(BuildConfig.baseURL)
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}
