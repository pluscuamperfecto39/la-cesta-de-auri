import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct ContentView: View {
    @EnvironmentObject private var store: AuriStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab = 0
    @State private var showingImporter = false
    @State private var shareItem: ShareItem?
    @State private var activeAlert: AuriAlert?

    var body: some View {
        VStack(spacing: 0) {
            BrandHeader(completionTrigger: store.completionCelebration)

            Group {
                switch selectedTab {
                case 1:
                    FrequentView()
                case 2:
                    FutureView()
                default:
                    BasketView(
                        onImport: { showingImporter = true },
                        onShare: shareBasket
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            AuriTabBar(selection: $selectedTab)
        }
        .background(AuriColors.background)
        .fileImporter(
            isPresented: $showingImporter,
            allowedContentTypes: [.auriList, .json, .data],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                if let url = urls.first { prepareImport(from: url) }
            case .failure(let error):
                activeAlert = .error(error.localizedDescription)
            }
        }
        .onOpenURL(perform: prepareImport)
        .sheet(item: $shareItem) { item in
            ActivityView(
                activityItems: [
                    item.url,
                    "Lista de la compra creada con La Cesta de Auri"
                ]
            )
        }
        .alert(item: $activeAlert) { alert in
            switch alert {
            case .importConfirmation(let items):
                let preview = items.prefix(4).map { "• \($0.name)" }.joined(separator: "\n")
                let remainder = items.count > 4 ? "\n• …y \(items.count - 4) más" : ""
                return Alert(
                    title: Text("Importar lista compartida"),
                    message: Text("Se añadirán \(items.count) productos a tu cesta:\n\(preview)\(remainder)"),
                    primaryButton: .default(Text("Importar")) {
                        store.importBasketItems(items)
                        selectedTab = 0
                    },
                    secondaryButton: .cancel(Text("Cancelar"))
                )
            case .error(let message):
                return Alert(
                    title: Text("No se pudo completar"),
                    message: Text(message),
                    dismissButton: .default(Text("Aceptar"))
                )
            }
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                store.moveDueFuturePurchases()
            }
        }
        .task(id: store.futurePurchases.first?.id) {
            await waitForNextFuturePurchase()
        }
    }

    private func shareBasket() {
        do {
            shareItem = ShareItem(url: try AuriFileService.export(store.basket))
        } catch {
            activeAlert = .error(error.localizedDescription)
        }
    }

    private func prepareImport(from url: URL) {
        do {
            activeAlert = .importConfirmation(try AuriFileService.importList(from: url))
        } catch {
            activeAlert = .error(error.localizedDescription)
        }
    }

    private func waitForNextFuturePurchase() async {
        do {
            while !Task.isCancelled {
                store.moveDueFuturePurchases()
                guard let nextDate = store.futurePurchases.first?.date else { return }
                let seconds = min(max(0, nextDate.timeIntervalSinceNow), 24 * 60 * 60)
                try await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            }
        } catch {
            // La tarea se reinicia automáticamente cuando cambia la próxima compra.
        }
    }
}

private enum AuriAlert: Identifiable {
    case importConfirmation([BasketItem])
    case error(String)

    var id: String {
        switch self {
        case .importConfirmation:
            return "import"
        case .error(let message):
            return "error-\(message)"
        }
    }
}

private struct ShareItem: Identifiable {
    let id = UUID()
    let url: URL
}

private struct BrandHeader: View {
    let completionTrigger: Int

    @State private var wandRaised = false
    @State private var heartBurst = 0

    var body: some View {
        HStack(spacing: 9) {
            ZStack(alignment: .topLeading) {
                Image("AuriCharacter")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 66, height: 72)
                    .rotationEffect(
                        .degrees(wandRaised ? 7 : 0),
                        anchor: .bottomTrailing
                    )
                    .offset(y: wandRaised ? -1 : 0)
                    .accessibilityLabel("Auri, Reina de Corazones")

                if heartBurst > 0 {
                    AuriHeartBurst()
                        .id(heartBurst)
                        .offset(x: 13, y: 45)
                        .accessibilityHidden(true)
                }
            }
            .frame(width: 66, height: 72)

            VStack(alignment: .leading, spacing: 3) {
                Text("La Cesta de Auri")
                    .font(.custom("SnellRoundhand-Bold", size: 30, relativeTo: .title2))
                    .foregroundStyle(AuriColors.purple)
                    .minimumScaleFactor(0.72)
                    .lineLimit(1)

                Text("Estoy ya hasta el mismísimo !!")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(AuriColors.muted)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 18)
        .padding(.top, 8)
        .padding(.bottom, 10)
        .background(AuriColors.background)
        .task(id: completionTrigger) {
            guard completionTrigger > 0 else { return }

            do {
                for burst in 0..<3 {
                    withAnimation(.easeInOut(duration: 0.28)) {
                        wandRaised = true
                    }
                    try await Task.sleep(nanoseconds: 170_000_000)
                    heartBurst += 1
                    try await Task.sleep(nanoseconds: 650_000_000)
                    withAnimation(.easeInOut(duration: 0.30)) {
                        wandRaised = false
                    }
                    if burst < 2 {
                        try await Task.sleep(nanoseconds: 500_000_000)
                    }
                }
            } catch {
                wandRaised = false
            }
        }
    }
}

private struct AuriHeartBurst: View {
    private let particles: [(x: CGFloat, rise: CGFloat, delay: Double, size: CGFloat)] = [
        (-10, 34, 0.00, 7),
        (2, 43, 0.08, 8),
        (13, 36, 0.16, 6),
        (-3, 52, 0.24, 7),
        (18, 49, 0.31, 6)
    ]

    var body: some View {
        ZStack {
            ForEach(particles.indices, id: \.self) { index in
                AuriHeartParticle(
                    horizontalDrift: particles[index].x,
                    rise: particles[index].rise,
                    delay: particles[index].delay,
                    size: particles[index].size
                )
            }
        }
    }
}

private struct AuriHeartParticle: View {
    let horizontalDrift: CGFloat
    let rise: CGFloat
    let delay: Double
    let size: CGFloat
    @State private var floating = false

    var body: some View {
        Image(systemName: "heart.fill")
            .font(.system(size: size, weight: .bold))
            .foregroundStyle(indexedHeartColor)
            .scaleEffect(floating ? 1.18 : 0.55)
            .offset(
                x: floating ? horizontalDrift : 0,
                y: floating ? -rise : 0
            )
            .opacity(floating ? 0 : 0.92)
            .onAppear {
                withAnimation(.easeOut(duration: 0.82).delay(delay)) {
                    floating = true
                }
            }
    }

    private var indexedHeartColor: Color {
        delay.truncatingRemainder(dividingBy: 0.16) == 0
            ? Color(red: 0.80, green: 0.12, blue: 0.29)
            : AuriColors.violet
    }
}

private struct AuriTabBar: View {
    @Binding var selection: Int

    private let tabs = [
        ("basket.fill", "Mi cesta"),
        ("star.fill", "Frecuentes"),
        ("clock.fill", "Para luego")
    ]

    var body: some View {
        HStack(spacing: 7) {
            ForEach(tabs.indices, id: \.self) { index in
                Button {
                    selection = index
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tabs[index].0)
                            .font(.system(size: 19, weight: .bold))
                        Text(tabs[index].1)
                            .font(.caption)
                            .fontWeight(.bold)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                    .foregroundStyle(selection == index ? .white : AuriColors.purpleDark)
                    .frame(maxWidth: .infinity, minHeight: 58)
                    .background(selection == index ? AuriColors.purple : Color.clear)
                    .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == index ? .isSelected : [])
            }
        }
        .padding(.horizontal, 10)
        .padding(.top, 8)
        .padding(.bottom, 5)
        .background(AuriColors.navBlue)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(AuriColors.line)
                .frame(height: 1)
        }
    }
}

private struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
