import SwiftUI

struct FrequentView: View {
    @EnvironmentObject private var store: AuriStore

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                AuriHero(colors: [AuriColors.lavender, AuriColors.violet.opacity(0.8)]) {
                    VStack(alignment: .leading, spacing: 7) {
                        Text("TUS IMPRESCINDIBLES")
                            .font(.caption.bold())
                            .tracking(1.5)
                            .foregroundStyle(AuriColors.purpleDark)

                        Text("Lo de siempre, en un toque")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundStyle(AuriColors.purpleDark)

                        Text("Cada producto que tachas aprende su lugar aquí.")
                            .font(.subheadline)
                            .foregroundStyle(AuriColors.purpleDark.opacity(0.75))
                    }
                }

                if store.frequent.isEmpty {
                    EmptyStateView(
                        symbol: "star.fill",
                        title: "Aquí aparecerán tus favoritos",
                        detail: "Cuando marques productos como comprados, Auri recordará cuáles repites más."
                    )
                } else {
                    HStack {
                        Text("Tus compras más repetidas")
                            .font(.title3.bold())
                            .foregroundStyle(AuriColors.ink)
                        Spacer()
                    }

                    ForEach(store.frequent) { item in
                        FrequentRow(item: item)
                    }
                }
            }
            .padding(.horizontal, 17)
            .padding(.top, 5)
            .padding(.bottom, 30)
        }
        .background(AuriColors.background)
    }
}

private struct FrequentRow: View {
    @EnvironmentObject private var store: AuriStore
    let item: FrequentItem

    var body: some View {
        Button {
            store.addFrequentToBasket(item)
        } label: {
            HStack(spacing: 13) {
                Image(systemName: "star.fill")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(AuriColors.purple)
                    .frame(width: 42, height: 42)
                    .background(AuriColors.purpleTint)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                VStack(alignment: .leading, spacing: 3) {
                    Text(item.name)
                        .font(.body.bold())
                        .foregroundStyle(AuriColors.ink)
                    Text("\(item.category) · \(item.count) \(item.count == 1 ? "vez" : "veces")")
                        .font(.caption)
                        .foregroundStyle(AuriColors.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "plus")
                    .font(.system(size: 17, weight: .black))
                    .foregroundStyle(AuriColors.purple)
                    .frame(width: 38, height: 38)
                    .background(AuriColors.purpleTint)
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
            }
            .padding(13)
            .auriCard()
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Añadir \(item.name) a la cesta")
    }
}
