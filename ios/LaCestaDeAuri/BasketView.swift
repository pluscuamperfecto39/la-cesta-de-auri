import SwiftUI
import UIKit

struct BasketView: View {
    @EnvironmentObject private var store: AuriStore

    let onImport: () -> Void
    let onShare: () -> Void

    @State private var product = ""
    @State private var category = "Otros"
    @State private var quantity = "1 ud."
    @State private var showingClearConfirmation = false
    @State private var cameraItem: BasketItem?
    @State private var cameraError: String?
    @State private var photoPresentation: ProductPhotoPresentation?
    @FocusState private var productFocused: Bool

    private var pending: [BasketItem] {
        store.basket.filter { !$0.isPurchased }
    }

    private var purchased: [BasketItem] {
        store.basket.filter(\.isPurchased)
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                basketHero
                composer

                if store.basket.isEmpty {
                    EmptyStateView(
                        symbol: "sparkles",
                        title: "Tu cesta espera ideas",
                        detail: "Añade arriba todo lo que necesites. Podrás ir tachándolo mientras compras."
                    )
                    .padding(.top, 2)
                } else {
                    listHeader

                    ForEach(pending) { item in
                        BasketRow(item: item, onPhoto: { handlePhotoTap(for: $0) })
                    }

                    if !purchased.isEmpty {
                        HStack {
                            Text("Ya está en la cesta · \(purchased.count)")
                                .font(.headline)
                                .foregroundStyle(AuriColors.ink)
                            Spacer()
                        }
                        .padding(.top, 8)

                        ForEach(purchased) { item in
                            BasketRow(item: item, onPhoto: { handlePhotoTap(for: $0) })
                        }

                        Button("Limpiar productos comprados") {
                            showingClearConfirmation = true
                        }
                        .font(.subheadline.bold())
                        .foregroundStyle(AuriColors.purple)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(AuriColors.purpleTint)
                        .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                    }
                }
            }
            .padding(.horizontal, 17)
            .padding(.top, 5)
            .padding(.bottom, 30)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(AuriColors.background)
        .alert("¿Limpiar lo comprado?", isPresented: $showingClearConfirmation) {
            Button("Cancelar", role: .cancel) {}
            Button("Limpiar", role: .destructive, action: store.clearPurchased)
        } message: {
            Text("Se quitarán de esta cesta, pero seguirán contando entre tus productos frecuentes.")
        }
        .alert(
            "Cámara",
            isPresented: Binding(
                get: { cameraError != nil },
                set: { if !$0 { cameraError = nil } }
            )
        ) {
            Button("Aceptar", role: .cancel) { cameraError = nil }
        } message: {
            Text(cameraError ?? "No se pudo usar la cámara.")
        }
        .fullScreenCover(item: $cameraItem) { item in
            ProductCameraView(
                onCapture: { image in reviewPhoto(image, for: item) },
                onCancel: { cameraItem = nil }
            )
            .ignoresSafeArea()
        }
        .sheet(item: $photoPresentation) { presentation in
            switch presentation {
            case .review(let photo):
                ProductPhotoReviewSheet(
                    item: photo.item,
                    image: photo.image,
                    onCancel: { photoPresentation = nil },
                    onRetake: { retakePhoto(for: photo.item) },
                    onUse: { savePhoto(photo.image, for: photo.item) }
                )
            case .saved(let photo):
                ProductPhotoPreviewSheet(
                    item: photo.item,
                    image: photo.image,
                    onClose: { photoPresentation = nil },
                    onChange: { retakePhoto(for: photo.item) }
                )
            }
        }
    }

    private var basketHero: some View {
        AuriHero(colors: [AuriColors.purple, AuriColors.purpleDark]) {
            VStack(alignment: .leading, spacing: 7) {
                Text("CESTA ACTUAL")
                    .font(.caption.bold())
                    .tracking(1.5)
                    .foregroundStyle(AuriColors.lavender)

                Text(heroTitle)
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundStyle(.white)

                Text(heroDetail)
                    .font(.subheadline)
                    .foregroundStyle(Color.white.opacity(0.86))

                ProgressView(value: Double(store.purchasedCount), total: Double(max(1, store.basket.count)))
                    .tint(AuriColors.lavender)
                    .padding(.top, 9)
            }
        }
    }

    private var heroTitle: String {
        if store.pendingCount == 0 { return "Todo en orden" }
        return "\(store.pendingCount) \(store.pendingCount == 1 ? "cosa pendiente" : "cosas pendientes")"
    }

    private var heroDetail: String {
        if store.basket.isEmpty { return "Añade el primer producto y empieza tu compra." }
        return "\(store.purchasedCount) de \(store.basket.count) productos en la cesta"
    }

    private var composer: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 8) {
                Text("¿Qué ponemos en la cesta?")
                    .font(.title3.bold())
                    .foregroundStyle(AuriColors.ink)
                    .minimumScaleFactor(0.85)

                Spacer(minLength: 4)

                Button(action: onImport) {
                    Label("Importar", systemImage: "square.and.arrow.down")
                        .font(.caption.bold())
                        .foregroundStyle(AuriColors.purpleDark)
                        .padding(.horizontal, 11)
                        .frame(minHeight: 38)
                        .background(AuriColors.navBlue)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 9) {
                    productField
                    addButton
                        .frame(minWidth: 126)
                }

                VStack(spacing: 9) {
                    productField
                    addButton
                        .frame(maxWidth: .infinity)
                }
            }

            HStack(spacing: 9) {
                Picker("Categoría", selection: $category) {
                    ForEach(AuriCategories.all, id: \.self) { value in
                        Text(value).tag(value)
                    }
                }
                .pickerStyle(.menu)
                .foregroundStyle(AuriColors.ink)
                .frame(maxWidth: .infinity, minHeight: 50, alignment: .leading)
                .padding(.horizontal, 9)
                .background(AuriColors.surface)
                .overlay {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(AuriColors.line)
                }

                TextField("1 ud.", text: $quantity)
                    .textInputAutocapitalization(.never)
                    .frame(minWidth: 98, maxWidth: 98, minHeight: 50)
                    .padding(.horizontal, 12)
                    .background(AuriColors.surface)
                    .overlay {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(AuriColors.line)
                    }
            }
        }
        .padding(16)
        .auriCard()
    }

    private var productField: some View {
        TextField("Ej. Tomates, arroz, jabón…", text: $product)
            .textInputAutocapitalization(.sentences)
            .submitLabel(.done)
            .focused($productFocused)
            .onSubmit(addProduct)
            .frame(maxWidth: .infinity, minHeight: 54)
            .padding(.horizontal, 13)
            .background(AuriColors.surface)
            .overlay {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .stroke(AuriColors.line)
            }
    }

    private var addButton: some View {
        Button(action: addProduct) {
            Label("Añadir", systemImage: "plus")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(AuriPrimaryButtonStyle())
        .disabled(product.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        .opacity(product.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.62 : 1)
    }

    private var listHeader: some View {
        HStack {
            Text("Tu lista")
                .font(.title3.bold())
                .foregroundStyle(AuriColors.ink)
            Spacer()
            Button(action: onShare) {
                Label("Compartir", systemImage: "square.and.arrow.up")
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .padding(.horizontal, 13)
                    .frame(minHeight: 40)
                    .background(AuriColors.purple)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
    }

    private func addProduct() {
        guard !product.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            productFocused = true
            return
        }
        store.addBasketItem(name: product, category: category, quantity: quantity)
        product = ""
        quantity = "1 ud."
        productFocused = false
    }

    private func openCamera(for item: BasketItem) {
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            cameraError = "La cámara no está disponible en este dispositivo. Pruébalo en un iPhone real."
            return
        }
        productFocused = false
        cameraItem = item
    }

    private func handlePhotoTap(for item: BasketItem) {
        if let url = store.photoURL(for: item),
           let image = UIImage(contentsOfFile: url.path) {
            photoPresentation = .saved(ProductPhoto(item: item, image: image))
        } else {
            openCamera(for: item)
        }
    }

    private func reviewPhoto(_ image: UIImage, for item: BasketItem) {
        cameraItem = nil
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) {
            photoPresentation = .review(ProductPhoto(item: item, image: image))
        }
    }

    private func retakePhoto(for item: BasketItem) {
        photoPresentation = nil
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) {
            openCamera(for: item)
        }
    }

    private func savePhoto(_ image: UIImage, for item: BasketItem) {
        photoPresentation = nil
        guard let data = image.jpegData(compressionQuality: 0.88) else {
            cameraError = "No se pudo preparar la fotografía."
            return
        }
        do {
            try store.setBasketPhoto(data, for: item.id)
        } catch {
            cameraError = "No se pudo guardar la fotografía en el iPhone."
        }
    }
}

private struct BasketRow: View {
    @EnvironmentObject private var store: AuriStore
    let item: BasketItem
    let onPhoto: (BasketItem) -> Void

    var body: some View {
        HStack(spacing: 11) {
            Button {
                store.toggleBasketItem(item.id)
            } label: {
                Image(systemName: item.isPurchased ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 27, weight: .semibold))
                    .foregroundStyle(item.isPurchased ? AuriColors.purple : AuriColors.muted)
                    .frame(width: 42, height: 46)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(item.isPurchased ? "Marcar \(item.name) como pendiente" : "Marcar \(item.name) como comprado")

            VStack(alignment: .leading, spacing: 3) {
                Text(item.name)
                    .font(.body.bold())
                    .foregroundStyle(item.isPurchased ? AuriColors.muted : AuriColors.ink)
                    .strikethrough(item.isPurchased)

                Text("\(item.quantity) · \(item.category)")
                    .font(.caption)
                    .foregroundStyle(AuriColors.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                onPhoto(item)
            } label: {
                Group {
                    if let url = store.photoURL(for: item),
                       let image = UIImage(contentsOfFile: url.path) {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                    } else {
                        Image(systemName: "camera.fill")
                            .font(.system(size: 19, weight: .semibold))
                            .foregroundStyle(AuriColors.purpleDark)
                    }
                }
                .frame(width: 50, height: 50)
                .background(AuriColors.navBlue)
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .stroke(AuriColors.line, lineWidth: 1)
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(
                store.photoURL(for: item) == nil
                    ? "Hacer una foto de \(item.name)"
                    : "Ver la foto de \(item.name)"
            )

            Button(role: .destructive) {
                store.removeBasketItem(item.id)
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(AuriColors.muted)
                    .frame(width: 42, height: 46)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Eliminar \(item.name)")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .auriCard()
    }
}

private struct ProductPhoto: Identifiable {
    let id = UUID()
    let item: BasketItem
    let image: UIImage
}

private enum ProductPhotoPresentation: Identifiable {
    case review(ProductPhoto)
    case saved(ProductPhoto)

    var id: UUID {
        switch self {
        case .review(let photo), .saved(let photo):
            return photo.id
        }
    }
}

private struct ProductPhotoReviewSheet: View {
    let item: BasketItem
    let image: UIImage
    let onCancel: () -> Void
    let onRetake: () -> Void
    let onUse: () -> Void

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                VStack(spacing: 15) {
                    Text("¿Ha quedado bien o quieres repetirla?")
                        .font(.subheadline)
                        .foregroundStyle(AuriColors.muted)
                        .multilineTextAlignment(.center)

                    productImage(maxHeight: max(220, proxy.size.height - 180))

                    HStack(spacing: 9) {
                        Button("Cancelar", role: .cancel, action: onCancel)
                            .buttonStyle(.bordered)

                        Button("Repetir", action: onRetake)
                            .buttonStyle(.bordered)
                            .tint(AuriColors.purple)

                        Button("Usar", action: onUse)
                            .buttonStyle(.borderedProminent)
                            .tint(AuriColors.purple)
                    }
                    .font(.subheadline.bold())
                    .controlSize(.large)
                    .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
            }
            .navigationTitle("Revisa la foto")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled()
    }

    private func productImage(maxHeight: CGFloat) -> some View {
        Image(uiImage: image)
            .resizable()
            .scaledToFit()
            .frame(maxWidth: .infinity, maxHeight: maxHeight)
            .background(Color.black.opacity(0.9))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .accessibilityLabel("Fotografía recién hecha de \(item.name)")
    }
}

private struct ProductPhotoPreviewSheet: View {
    let item: BasketItem
    let image: UIImage
    let onClose: () -> Void
    let onChange: () -> Void

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                VStack(spacing: 15) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity, maxHeight: max(240, proxy.size.height - 120))
                        .background(Color.black.opacity(0.9))
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .accessibilityLabel("Fotografía ampliada de \(item.name)")

                    HStack(spacing: 10) {
                        Button("Cerrar", role: .cancel, action: onClose)
                            .buttonStyle(.bordered)

                        Button("Cambiar foto", action: onChange)
                            .buttonStyle(.borderedProminent)
                            .tint(AuriColors.purple)
                    }
                    .font(.subheadline.bold())
                    .controlSize(.large)
                    .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
            }
            .navigationTitle(item.name)
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}

private struct ProductCameraView: UIViewControllerRepresentable {
    let onCapture: (UIImage) -> Void
    let onCancel: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraCaptureMode = .photo
        picker.allowsEditing = false
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        private let parent: ProductCameraView

        init(parent: ProductCameraView) {
            self.parent = parent
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            guard let image = info[.originalImage] as? UIImage else {
                parent.onCancel()
                return
            }
            parent.onCapture(image)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onCancel()
        }
    }
}
