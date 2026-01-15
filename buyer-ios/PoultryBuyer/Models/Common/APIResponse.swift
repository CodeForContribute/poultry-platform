import Foundation

struct APIResponse<T: Decodable>: Decodable {
    let success: Bool
    let message: String?
    let data: T?
    let error: ErrorDetails?
    let timestamp: Date?
    let correlationId: String?
}

struct ErrorDetails: Decodable {
    let code: String
    let message: String
    let details: [String: String]?
}

struct PaginatedResponse<T: Decodable>: Decodable {
    let content: [T]
    let totalElements: Int
    let totalPages: Int
    let size: Int
    let number: Int
    let first: Bool
    let last: Bool
    let empty: Bool
}
