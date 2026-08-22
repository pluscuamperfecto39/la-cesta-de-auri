import Foundation

struct BasketItem: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String
    var category: String
    var quantity: String
    var isPurchased: Bool = false
    var countedAsFrequent: Bool = false
}

struct FrequentItem: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String
    var category: String
    var count: Int
}

struct FuturePurchase: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var title: String
    var date: Date
    var notificationEnabled: Bool
}

enum AuriCategories {
    static let all = [
        "Fruta y verdura",
        "Despensa",
        "Lácteos",
        "Panadería",
        "Carne y pescado",
        "Bebidas",
        "Hogar",
        "Higiene",
        "Otros"
    ]

    static func safe(_ value: String) -> String {
        all.contains(value) ? value : "Otros"
    }
}
