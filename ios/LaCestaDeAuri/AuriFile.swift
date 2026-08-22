import Foundation
import UniformTypeIdentifiers

extension UTType {
    static let auriList = UTType(
        exportedAs: "com.auri.cesta.list",
        conformingTo: .json
    )
}

struct SharedListPayload: Codable {
    let format: String
    let version: Int
    let createdAt: Int64
    let items: [SharedBasketItem]
}

struct SharedBasketItem: Codable {
    let name: String
    let category: String
    let quantity: String
    let checked: Bool?
}

enum AuriFileError: LocalizedError {
    case tooLarge
    case unsupported
    case empty
    case cannotCreate

    var errorDescription: String? {
        switch self {
        case .tooLarge:
            return "La lista es demasiado grande."
        case .unsupported:
            return "El archivo no es una lista válida de La Cesta de Auri."
        case .empty:
            return "La lista no contiene productos válidos."
        case .cannotCreate:
            return "No se pudo crear el archivo para compartir."
        }
    }
}

enum AuriFileService {
    private static let maximumBytes = 512 * 1024
    private static let maximumItems = 200

    static func export(_ basket: [BasketItem]) throws -> URL {
        guard !basket.isEmpty else { throw AuriFileError.empty }

        let payload = SharedListPayload(
            format: "la-cesta-de-auri",
            version: 1,
            createdAt: Int64(Date().timeIntervalSince1970 * 1_000),
            items: basket.map {
                SharedBasketItem(
                    name: $0.name,
                    category: $0.category,
                    quantity: $0.quantity,
                    checked: $0.isPurchased
                )
            }
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        let data = try encoder.encode(payload)
        guard data.count <= maximumBytes else { throw AuriFileError.tooLarge }

        let folder = FileManager.default.temporaryDirectory
            .appendingPathComponent("ListasDeAuri", isDirectory: true)
        do {
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)

            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.dateFormat = "yyyy-MM-dd_HH-mm"
            let file = folder.appendingPathComponent(
                "Cesta-de-Auri-\(formatter.string(from: Date())).auri"
            )
            try data.write(to: file, options: .atomic)
            return file
        } catch {
            throw AuriFileError.cannotCreate
        }
    }

    static func importList(from url: URL) throws -> [BasketItem] {
        let granted = url.startAccessingSecurityScopedResource()
        defer {
            if granted { url.stopAccessingSecurityScopedResource() }
        }

        if let values = try? url.resourceValues(forKeys: [.fileSizeKey]),
           let size = values.fileSize,
           size > maximumBytes {
            throw AuriFileError.tooLarge
        }

        let data = try Data(contentsOf: url, options: .mappedIfSafe)
        guard data.count <= maximumBytes else { throw AuriFileError.tooLarge }

        let payload: SharedListPayload
        do {
            payload = try JSONDecoder().decode(SharedListPayload.self, from: data)
        } catch {
            throw AuriFileError.unsupported
        }

        guard payload.format == "la-cesta-de-auri", payload.version == 1 else {
            throw AuriFileError.unsupported
        }
        guard !payload.items.isEmpty, payload.items.count <= maximumItems else {
            throw payload.items.isEmpty ? AuriFileError.empty : AuriFileError.tooLarge
        }

        let result = payload.items.compactMap { shared -> BasketItem? in
            let name = compact(shared.name, maximum: 100)
            guard !name.isEmpty else { return nil }
            let quantity = compact(shared.quantity, maximum: 40)
            return BasketItem(
                name: name,
                category: AuriCategories.safe(shared.category),
                quantity: quantity.isEmpty ? "1 ud." : quantity
            )
        }
        guard !result.isEmpty else { throw AuriFileError.empty }
        return result
    }

    private static func compact(_ value: String, maximum: Int) -> String {
        String(
            value
                .split(whereSeparator: { $0.isWhitespace })
                .joined(separator: " ")
                .prefix(maximum)
        )
    }
}
