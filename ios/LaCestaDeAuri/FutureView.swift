import SwiftUI

struct FutureView: View {
    @EnvironmentObject private var store: AuriStore
    @State private var showingNewPurchase = false

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                AuriHero(colors: [AuriColors.violet, AuriColors.purple]) {
                    VStack(alignment: .leading, spacing: 7) {
                        Text("TENGO QUE COMPRAR")
                            .font(.caption.bold())
                            .tracking(1.5)
                            .foregroundStyle(AuriColors.lavender)

                        Text("Que no se te escape nada")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundStyle(.white)

                        Text("Al llegar la fecha pasará sola a tu cesta.")
                            .font(.subheadline)
                            .foregroundStyle(Color.white.opacity(0.86))
                    }
                }

                Button {
                    showingNewPurchase = true
                } label: {
                    Label("Añadir una compra futura", systemImage: "plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(AuriPrimaryButtonStyle())

                if store.futurePurchases.isEmpty {
                    EmptyStateView(
                        symbol: "clock.fill",
                        title: "Nada pendiente para después",
                        detail: "Guarda aquí algo que quieras comprar otro día y activa un aviso si lo necesitas."
                    )
                } else {
                    HStack {
                        Text("Próximas compras")
                            .font(.title3.bold())
                            .foregroundStyle(AuriColors.ink)
                        Spacer()
                    }

                    ForEach(store.futurePurchases) { item in
                        FuturePurchaseRow(item: item)
                    }
                }
            }
            .padding(.horizontal, 17)
            .padding(.top, 5)
            .padding(.bottom, 30)
        }
        .background(AuriColors.background)
        .sheet(isPresented: $showingNewPurchase) {
            NewFuturePurchaseView()
                .environmentObject(store)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
    }
}

private struct FuturePurchaseRow: View {
    @EnvironmentObject private var store: AuriStore
    let item: FuturePurchase

    private var isPast: Bool {
        item.date < Date()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 5) {
                    Text(item.title)
                        .font(.body.bold())
                        .foregroundStyle(AuriColors.ink)

                    Text((isPast ? "Fecha pasada · " : "") + item.date.formatted(
                        .dateTime
                            .locale(Locale(identifier: "es_ES"))
                            .weekday(.wide)
                            .day()
                            .month(.abbreviated)
                            .hour()
                            .minute()
                    ))
                    .font(.subheadline.bold())
                    .foregroundStyle(isPast ? AuriColors.violet : AuriColors.purple)

                    Label(
                        item.notificationEnabled ? "Aviso activado" : "Aviso desactivado",
                        systemImage: item.notificationEnabled ? "bell.fill" : "bell.slash"
                    )
                    .font(.caption)
                    .foregroundStyle(AuriColors.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(role: .destructive) {
                    store.removeFuturePurchase(item.id)
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(AuriColors.muted)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Eliminar \(item.title)")
            }

            HStack {
                Spacer()
                Button {
                    store.moveFutureToBasket(item)
                } label: {
                    Label("Pasar a mi cesta", systemImage: "basket")
                        .font(.subheadline.bold())
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .frame(minHeight: 44)
                        .background(AuriColors.purple)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(15)
        .auriCard()
    }
}

private struct NewFuturePurchaseView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AuriStore

    @State private var title = ""
    @State private var date = Date().addingTimeInterval(24 * 60 * 60)
    @State private var notificationEnabled = true
    @FocusState private var titleFocused: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section("¿Qué tienes que comprar?") {
                    TextField("Ej. Regalo, café, pilas…", text: $title)
                        .textInputAutocapitalization(.sentences)
                        .focused($titleFocused)
                }

                Section("Cuándo") {
                    DatePicker(
                        "Fecha y hora",
                        selection: $date,
                        in: Date()...,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .datePickerStyle(.compact)
                }

                Section {
                    Toggle("Avisarme con una notificación", isOn: $notificationEnabled)
                } footer: {
                    Text("La compra pasará a tu cesta aunque no actives el aviso.")
                }
            }
            .scrollContentBackground(.hidden)
            .background(AuriColors.background)
            .navigationTitle("Compra futura")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        store.addFuturePurchase(
                            title: title,
                            date: date,
                            notificationEnabled: notificationEnabled
                        )
                        dismiss()
                    }
                    .fontWeight(.bold)
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .onAppear { titleFocused = true }
        }
    }
}
