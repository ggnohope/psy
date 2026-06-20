import SwiftUI

struct RootView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var colors

    // Initial selected tab. Reads PSY_TAB (0 home, 1 stats, 2 calendar, 3 budget) once for
    // screenshot testability; defaults to Home.
    @State private var selection: Int = {
        if let raw = ProcessInfo.processInfo.environment["PSY_TAB"], let n = Int(raw) { return n }
        return 0
    }()

    var body: some View {
        TabView(selection: $selection) {
            HomeView(container: container)
                .tabItem { Label("Trang chủ", systemImage: "house.fill") }
                .tag(0)
            StatsView(container: container)
                .tabItem { Label("Thống kê", systemImage: "chart.bar.fill") }
                .tag(1)
            CalendarView(container: container)
                .tabItem { Label("Lịch", systemImage: "calendar") }
                .tag(2)
            tabStub("Ngân sách")
                .tabItem { Label("Ngân sách", systemImage: "banknote.fill") }
                .tag(3)
        }
    }

    private func tabStub(_ title: String) -> some View {
        ZStack {
            colors.background.ignoresSafeArea()
            Text(title).font(PsyFont.headlineMedium).foregroundStyle(colors.onSurface)
        }
    }
}
