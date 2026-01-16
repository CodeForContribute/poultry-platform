import Foundation

struct Order: Codable, Identifiable {
    let id: String
    let orderNumber: String
    let buyerId: String
    let buyerName: String
    let sellerId: String
    let sellerName: String
    let type: OrderType
    let status: OrderStatus
    let subtotal: Double
    let discountAmount: Double
    let gstAmount: Double
    let deliveryCharge: Double
    let totalAmount: Double
    let platformFee: Double
    let deliveryDate: String?
    let deliverySlot: String?
    let deliveryAddress: DeliveryAddress
    let deliveryInstructions: String?
    let items: [OrderItem]
    let version: Int
    let expiresAt: Date?
    let cancelledAt: Date?
    let cancelledBy: String?
    let cancellationReason: String?
    let createdAt: Date
    let updatedAt: Date
    let allowedNextStates: [String]
    let canCancel: Bool
}

struct OrderItem: Codable, Identifiable {
    let id: String
    let productId: String
    let productName: String
    let productSku: String
    let quantity: Double
    let unitPrice: Double
    let discountPercent: Double?
    let discountAmount: Double
    let gstPercent: Double
    let gstAmount: Double
    let lineTotal: Double
    let deliveredQuantity: Double?
    let status: String
}

struct DeliveryAddress: Codable {
    let label: String?
    let line1: String
    let line2: String?
    let city: String
    let state: String
    let pincode: String
    let landmark: String?
    let latitude: Double?
    let longitude: Double?
    let contactName: String
    let contactPhone: String
}

struct CreateOrderRequest: Codable {
    let idempotencyKey: String
    let sellerId: String
    let type: String
    let items: [CreateOrderItemRequest]
    let deliveryAddress: DeliveryAddress
    let deliveryDate: String?
    let deliverySlot: String?
    let deliveryInstructions: String?
}

struct CreateOrderItemRequest: Codable {
    let productId: String
    let quantity: Double
}

enum OrderType: String, Codable {
    case regular = "REGULAR"
    case advance = "ADVANCE"
}

enum OrderStatus: String, Codable, CaseIterable {
    case draft = "DRAFT"
    case placed = "PLACED"
    case sellerConfirmed = "SELLER_CONFIRMED"
    case sellerRejected = "SELLER_REJECTED"
    case dispatched = "DISPATCHED"
    case delivered = "DELIVERED"
    case cancelledByBuyer = "CANCELLED_BY_BUYER"
    case cancelledBySeller = "CANCELLED_BY_SELLER"
    case paymentPending = "PAYMENT_PENDING"

    var displayName: String {
        switch self {
        case .draft: return "Draft"
        case .placed: return "Placed"
        case .sellerConfirmed: return "Confirmed"
        case .sellerRejected: return "Rejected"
        case .dispatched: return "Dispatched"
        case .delivered: return "Delivered"
        case .cancelledByBuyer: return "Cancelled"
        case .cancelledBySeller: return "Cancelled"
        case .paymentPending: return "Payment Pending"
        }
    }

    var color: String {
        switch self {
        case .draft: return "gray"
        case .placed: return "blue"
        case .sellerConfirmed: return "green"
        case .sellerRejected: return "red"
        case .dispatched: return "orange"
        case .delivered: return "green"
        case .cancelledByBuyer, .cancelledBySeller: return "red"
        case .paymentPending: return "yellow"
        }
    }
}
