import SwiftUI

struct RootView: View {
    let container: AppContainer
    @Environment(\.psyColors) private var colors

    var body: some View {
        TabView {
            HomeView(container: container)
                .tabItem { Label("Trang chủ", systemImage: "house.fill") }
            tabStub("Thống kê")
                .tabItem { Label("Thống kê", systemImage: "chart.bar.fill") }
            tabStub("Lịch")
                .tabItem { Label("Lịch", systemImage: "calendar") }
            tabStub("Ngân sách")
                .tabItem { Label("Ngân sách", systemImage: "banknote.fill") }
        }
    }

    private func tabStub(_ title: String) -> some View {
        ZStack {
            colors.background.ignoresSafeArea()
            Text(title).font(PsyFont.headlineMedium).foregroundStyle(colors.onSurface)
        }
    }
}
