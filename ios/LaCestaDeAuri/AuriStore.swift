import Combine
import Foundation
import UserNotifications

@MainActor
final class AuriStore: ObservableObject {
    @Published private(set) var basket: [BasketItem] = []
    @Published private(set) var frequent: [FrequentItem] = []
    @Published private(set) var futurePurchases: [FuturePurchase] = []
    @Published private(set) var completionCelebration = 0

    private let defaults: UserDefaults
    private let photoDirectory: URL
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private enum Key {
        static let basket = "auri.ios.basket"
        static let frequent = "auri.ios.frequent"
        static let future = "auri.ios.future"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        photoDirectory = FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("ProductPhotos", isDirectory: true)
        try? FileManager.default.createDirectory(
            at: photoDirectory,
            withIntermediateDirectories: true
        )
        loadAll()
        moveDueFuturePurchases()
    }

    var purchasedCount: Int {
        basket.filter(\.isPurchased).count
    }

    var pendingCount: Int {
        basket.count - purchasedCount
    }

    func addBasketItem(name: String, category: String, quantity: String) {
        let cleanName = Self.cleanName(name)
        guard !cleanName.isEmpty else { return }

        basket.insert(
            BasketItem(
                name: cleanName,
                category: AuriCategories.safe(category),
                quantity: Self.cleanQuantity(quantity)
            ),
            at: 0
        )
        saveBasket()
    }

    func addBasketItems(names: [String], category: String, quantity: String) {
        let cleanItems = names.compactMap { name -> BasketItem? in
            let cleanName = Self.cleanName(name)
            guard !cleanName.isEmpty else { return nil }
            return BasketItem(
                name: cleanName,
                category: AuriCategories.safe(category),
                quantity: Self.cleanQuantity(quantity)
            )
        }
        guard !cleanItems.isEmpty else { return }

        basket.insert(contentsOf: cleanItems, at: 0)
        saveBasket()
    }

    func importBasketItems(_ imported: [BasketItem]) {
        let cleanItems = imported.map {
            BasketItem(
                name: Self.cleanName($0.name),
                category: AuriCategories.safe($0.category),
                quantity: Self.cleanQuantity($0.quantity)
            )
        }.filter { !$0.name.isEmpty }

        basket.insert(contentsOf: cleanItems, at: 0)
        saveBasket()
    }

    func toggleBasketItem(_ id: UUID) {
        guard let index = basket.firstIndex(where: { $0.id == id }) else { return }
        let wasComplete = !basket.isEmpty && basket.allSatisfy(\.isPurchased)
        basket[index].isPurchased.toggle()

        if basket[index].isPurchased && !basket[index].countedAsFrequent {
            basket[index].countedAsFrequent = true
            incrementFrequent(name: basket[index].name, category: basket[index].category)
        }
        saveBasket()

        if !wasComplete && basket.allSatisfy(\.isPurchased) {
            completionCelebration += 1
        }
    }

    func photoURL(for item: BasketItem) -> URL? {
        guard let filename = safePhotoFilename(item.photoFilename) else { return nil }
        let url = photoDirectory.appendingPathComponent(filename, isDirectory: false)
        return FileManager.default.fileExists(atPath: url.path) ? url : nil
    }

    func setBasketPhoto(_ jpegData: Data, for id: UUID) throws {
        guard let index = basket.firstIndex(where: { $0.id == id }) else { return }
        try FileManager.default.createDirectory(
            at: photoDirectory,
            withIntermediateDirectories: true
        )

        let previousFilename = basket[index].photoFilename
        let filename = "product_\(id.uuidString)_\(UUID().uuidString).jpg"
        let destination = photoDirectory.appendingPathComponent(filename, isDirectory: false)
        try jpegData.write(to: destination, options: .atomic)

        basket[index].photoFilename = filename
        saveBasket()
        deletePhoto(previousFilename)
    }

    func removeBasketItem(_ id: UUID) {
        let photos = basket
            .filter { $0.id == id }
            .compactMap(\.photoFilename)
        basket.removeAll { $0.id == id }
        saveBasket()
        photos.forEach { deletePhoto($0) }
    }

    func clearPurchased() {
        let photos = basket
            .filter(\.isPurchased)
            .compactMap(\.photoFilename)
        basket.removeAll { $0.isPurchased }
        saveBasket()
        photos.forEach { deletePhoto($0) }
    }

    func addFrequentToBasket(_ item: FrequentItem) {
        addBasketItem(name: item.name, category: item.category, quantity: "1 ud.")
    }

    func addFuturePurchase(title: String, date: Date, notificationEnabled: Bool) {
        let cleanTitle = Self.cleanName(title)
        guard !cleanTitle.isEmpty else { return }

        let item = FuturePurchase(
            title: cleanTitle,
            date: date,
            notificationEnabled: notificationEnabled
        )
        futurePurchases.append(item)
        futurePurchases.sort { $0.date < $1.date }
        saveFuture()

        if notificationEnabled {
            NotificationService.schedule(item)
        }
    }

    func removeFuturePurchase(_ id: UUID) {
        futurePurchases.removeAll { $0.id == id }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id.uuidString])
        saveFuture()
    }

    func moveFutureToBasket(_ item: FuturePurchase) {
        addBasketItem(name: item.title, category: "Otros", quantity: "1 ud.")
        removeFuturePurchase(item.id)
    }

    /// Pasa a la cesta principal todo lo que ya ha alcanzado su fecha programada.
    @discardableResult
    func moveDueFuturePurchases(referenceDate: Date = Date()) -> Int {
        let due = futurePurchases.filter { $0.date <= referenceDate }
        guard !due.isEmpty else { return 0 }

        for item in due {
            basket.insert(
                BasketItem(
                    id: item.id,
                    name: item.title,
                    category: "Otros",
                    quantity: "1 ud."
                ),
                at: 0
            )
        }
        futurePurchases.removeAll { $0.date <= referenceDate }
        saveBasket()
        saveFuture()

        let identifiers = due.map { $0.id.uuidString }
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
        center.removeDeliveredNotifications(withIdentifiers: identifiers)
        return due.count
    }

    private func incrementFrequent(name: String, category: String) {
        let key = Self.normalized(name)
        if let index = frequent.firstIndex(where: { Self.normalized($0.name) == key }) {
            frequent[index].count += 1
            frequent[index].category = category
        } else {
            frequent.append(FrequentItem(name: name, category: category, count: 1))
        }
        frequent.sort { left, right in
            if left.count == right.count {
                return left.name.localizedCaseInsensitiveCompare(right.name) == .orderedAscending
            }
            return left.count > right.count
        }
        saveFrequent()
    }

    private func loadAll() {
        basket = load([BasketItem].self, key: Key.basket) ?? []
        frequent = load([FrequentItem].self, key: Key.frequent) ?? []
        futurePurchases = (load([FuturePurchase].self, key: Key.future) ?? [])
            .sorted { $0.date < $1.date }
    }

    private func load<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? decoder.decode(type, from: data)
    }

    private func save<T: Encodable>(_ value: T, key: String) {
        guard let data = try? encoder.encode(value) else { return }
        defaults.set(data, forKey: key)
    }

    private func saveBasket() {
        save(basket, key: Key.basket)
    }

    private func saveFrequent() {
        save(frequent, key: Key.frequent)
    }

    private func saveFuture() {
        save(futurePurchases, key: Key.future)
    }

    private func safePhotoFilename(_ filename: String?) -> String? {
        guard let filename, !filename.isEmpty else { return nil }
        guard URL(fileURLWithPath: filename).lastPathComponent == filename else { return nil }
        guard filename.hasPrefix("product_"), filename.hasSuffix(".jpg") else { return nil }
        return filename
    }

    private func deletePhoto(_ filename: String?) {
        guard let filename = safePhotoFilename(filename) else { return }
        try? FileManager.default.removeItem(
            at: photoDirectory.appendingPathComponent(filename, isDirectory: false)
        )
    }

    private static func cleanName(_ value: String) -> String {
        let compact = value
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
            .prefix(100)
        guard let first = compact.first else { return "" }
        return first.uppercased() + String(compact.dropFirst())
    }

    private static func cleanQuantity(_ value: String) -> String {
        let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return clean.isEmpty ? "1 ud." : String(clean.prefix(40))
    }

    private static func normalized(_ value: String) -> String {
        value.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: Locale(identifier: "es_ES"))
    }
}

private enum NotificationService {
    static func schedule(_ item: FuturePurchase) {
        guard item.date > Date() else { return }

        Task {
            let center = UNUserNotificationCenter.current()
            let settings = await center.notificationSettings()
            var allowed = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional

            if settings.authorizationStatus == .notDetermined {
                allowed = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
            }
            guard allowed else { return }

            let content = UNMutableNotificationContent()
            content.title = "La Cesta de Auri"
            content.body = "Recuerda comprar: \(item.title)"
            content.sound = .default

            let components = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute],
                from: item.date
            )
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            let request = UNNotificationRequest(
                identifier: item.id.uuidString,
                content: content,
                trigger: trigger
            )
            try? await center.add(request)
        }
    }
}
