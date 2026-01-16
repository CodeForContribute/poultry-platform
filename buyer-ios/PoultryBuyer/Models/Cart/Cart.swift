import Foundation

struct Cart: Codable, Identifiable {
    let id: String
    let buyerId: String
    let sellerId: String
    let sellerName: String
    let sellerBusinessName: String?
    let items: [CartItem]
    let itemCount: Int
    let totalAmount: Double
    let createdAt: Date
    let updatedAt: Date
}

struct CartItem: Codable, Identifiable {
    let id: String
    let productId: String
    let productName: String
    let productSku: String
    let productUnit: String
    let quantity: Double
    let unitPrice: Double
    let lineTotal: Double
    let notes: String?
    let createdAt: Date
}

struct AddToCartRequest: Codable {
    let productId: String
    let quantity: Double
    let notes: String?
}

struct UpdateCartItemRequest: Codable {
    let quantity: Double
    let notes: String?
}
