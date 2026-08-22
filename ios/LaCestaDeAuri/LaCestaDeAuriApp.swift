import SwiftUI

@main
struct LaCestaDeAuriApp: App {
    @StateObject private var store = AuriStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
                .tint(AuriColors.purple)
        }
    }
}
