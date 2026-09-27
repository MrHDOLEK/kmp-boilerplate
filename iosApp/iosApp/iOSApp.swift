import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        StartDependencyInjectionKt.startDependencyInjection()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
