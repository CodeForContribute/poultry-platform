import Foundation

struct Product: Codable, Identifiable {
    let id: String
    let sellerId: String
    let sellerName: String
    let category: Category
    let name: String
    let nameHi: String?
    let sku: String
    let description: String?
    let unit: ProductUnit
    let minOrderQty: Double
    let maxOrderQty: Double?
    let imageUrls: [String]
    let status: ProductStatus
    let currentPrice: Price?
    let createdAt: Date
    let updatedAt: Date
}

struct Category: Codable, Identifiable {
    let id: String
    let code: String
    let name: String
    let nameHi: String?
    let hsnCode: String?
    let description: String?
    let gstRate: Double
}

struct Price: Codable, Identifiable {
    let id: String
    let productId: String
    let basePrice: Double
    let bulkDiscountSlabs: [BulkDiscountSlab]?
    let effectiveFrom: Date
    let effectiveTo: Date?
    let isCurrentlyActive: Bool
    let isScheduled: Bool
    let createdAt: Date
}

struct BulkDiscountSlab: Codable {
    let minQty: Double
    let maxQty: Double?
    let discountPercent: Double
}

enum ProductUnit: String, Codable {
    case kg = "KG"
    case piece = "PIECE"
    case tray = "TRAY"
    case bag = "BAG"
    case bottle = "BOTTLE"
    case box = "BOX"
}

enum ProductStatus: String, Codable {
    case active = "ACTIVE"
    case inactive = "INACTIVE"
    case outOfStock = "OUT_OF_STOCK"
    case deleted = "DELETED"
}
