import Foundation

/// Represents the user's profile information
struct UserProfile: Codable, Identifiable {
    let id: String
    var name: String?
    var email: String?
    var phone: String?
    var profileImageUrl: String?
    var createdAt: Date?
    var updatedAt: Date?

    // Statistics
    var orderCount: Int?
    var totalSpent: Double?
    var addressCount: Int?

    /// Display name with fallback
    var displayName: String {
        name ?? "User"
    }

    /// Formatted phone number
    var formattedPhone: String? {
        guard let phone = phone else { return nil }
        if phone.count == 10 {
            let areaCode = phone.prefix(5)
            let remaining = phone.suffix(5)
            return "\(areaCode) \(remaining)"
        }
        return phone
    }
}

/// Request model for updating profile
struct UpdateProfileRequest: Encodable {
    let name: String?
    let email: String?
}

/// Response model for profile statistics
struct ProfileStats: Codable {
    let totalOrders: Int
    let pendingOrders: Int
    let completedOrders: Int
    let totalSpent: Double
    let savedAddresses: Int
}
