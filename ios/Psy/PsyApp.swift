import SwiftUI

@main
struct PsyApp: App {
    var body: some Scene {
        WindowGroup {
            PlaceholderRootView()
                .psyTheme(mode: .system, accent: .candyViolet)
        }
    }
}

private struct PlaceholderRootView: View {
    @Environment(\.psyColors) private var colors
    var body: some View {
        VStack(spacing: 12) {
            Text("Psy 🐷").font(PsyFont.headlineMedium)
            Text(BuildConfig.baseURL)
                .font(PsyFont.labelSmall)
                .foregroundStyle(colors.onSurface.opacity(0.6))
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(colors.background)
    }
}
