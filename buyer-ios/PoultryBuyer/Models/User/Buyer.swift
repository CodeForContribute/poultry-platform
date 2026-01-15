import Foundation

struct Buyer: Codable, Identifiable {
    let id: String
    var name: String?
    var email: String?
    var phone: String?
    var addresses: [Address]?
}

struct Address: Codable, Identifiable {
    let id: String
    var label: String?
    var line1: String
    var line2: String?
    var city: String
    var state: String
    var pincode: String
    var landmark: String?
    var latitude: Double?
    var longitude: Double?
    var contactName: String
    var contactPhone: String
    var isDefault: Bool?
}

struct BuyerOtpRequest: Encodable {
    let phone: String
    let deviceId: String?
    let deviceFingerprint: String?
    let deviceInfo: String?
}

struct BuyerOtpVerifyRequest: Encodable {
    let phone: String
    let otp: String
    let deviceId: String
    let deviceFingerprint: String?
    let deviceInfo: String?
    let fcmToken: String?
}

struct OtpSentResponse: Decodable {
    let maskedPhone: String
    let expiresInSeconds: Int
    let attemptsRemaining: Int
    let newUser: Bool
}

struct TokenResponse: Decodable {
    let accessToken: String
    let refreshToken: String
    let tokenType: String
    let expiresIn: Int
    let expiresAt: Date
    let userId: String
    let userType: String
    let name: String?
    let email: String?
    let role: String
    let mustChangePassword: Bool
    let newUser: Bool
}

struct RefreshTokenRequest: Encodable {
    let refreshToken: String
    let deviceId: String?
}
