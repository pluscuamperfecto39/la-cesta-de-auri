import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct ContentView: View {
    @EnvironmentObject private var store: AuriStore
    @State private var selectedTab = 0
    @State private var showingImporter = false
    @State private var shareItem: ShareItem?
    @State private var activeAlert: AuriAlert?

    var body: some View {
        VStack(spacing: 0) {
            BrandHeader()

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
    var body: some View {
        HStack(spacing: 9) {
            Image("AuriCharacter")
                .resizable()
                .scaledToFit()
                .frame(width: 66, height: 72)
                .accessibilityLabel("Auri, Reina de Corazones")

            VStack(alignment: .leading, spacing: 3) {
                Text("La Cesta de Auri")
                    .font(.custom("SnellRoundhand-Bold", size: 27, relativeTo: .title2))
                    .foregroundStyle(AuriColors.purple)
                    .minimumScaleFactor(0.72)
                    .lineLimit(1)

                Text("Compra con calma, recuerda con cariño")
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
