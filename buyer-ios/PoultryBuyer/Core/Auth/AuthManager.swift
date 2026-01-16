import Foundation
import Combine
import UserNotifications

// MARK: - FCM Token Manager
class FCMTokenManager {
    static let shared = FCMTokenManager()

    private var fcmToken: String?

    private init() {}

    /// Get the current FCM token
    /// Returns nil if Firebase is not configured or token not available
    func getToken() -> String? {
        // When Firebase is configured, uncomment the following:
        // return Messaging.messaging().fcmToken
        return fcmToken
    }

    /// Update FCM token (called from AppDelegate when token refreshes)
    func updateToken(_ token: String) {
        fcmToken = token
        // Notify server of token update if user is authenticated
        if AuthManager.shared.isAuthenticated {
            Task {
                await updateTokenOnServer(token)
            }
        }
    }

    private func updateTokenOnServer(_ token: String) async {
        do {
            struct FCMTokenUpdate: Codable {
                let fcmToken: String
            }
            let _: APIResponse<EmptyResponse> = try await APIClient.shared.request(
                endpoint: "/auth/buyer/fcm-token",
                method: .put,
                body: FCMTokenUpdate(fcmToken: token),
                requiresAuth: true
            )
        } catch {
            print("Failed to update FCM token on server: \(error)")
        }
    }
}

class AuthManager: ObservableObject {
    static let shared = AuthManager()

    @Published var isAuthenticated = false
    @Published var currentUser: Buyer?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let keychain = KeychainManager.shared

    init() {
        // Check if user is already authenticated
        if keychain.getAccessToken() != nil {
            isAuthenticated = true
        }
    }

    // MARK: - OTP Request

    func requestOTP(phone: String) async throws -> OtpSentResponse {
        isLoading = true
        defer { isLoading = false }

        let request = BuyerOtpRequest(
            phone: phone,
            deviceId: getDeviceId(),
            deviceFingerprint: nil,
            deviceInfo: getDeviceInfo()
        )

        let response: APIResponse<OtpSentResponse> = try await APIClient.shared.request(
            endpoint: "/auth/buyer/otp/request",
            method: .post,
            body: request,
            requiresAuth: false
        )

        guard response.success, let data = response.data else {
            throw APIError.serverError(response.message ?? "Failed to send OTP")
        }

        return data
    }

    // MARK: - OTP Verification

    func verifyOTP(phone: String, otp: String) async throws {
        isLoading = true
        defer { isLoading = false }

        let request = BuyerOtpVerifyRequest(
            phone: phone,
            otp: otp,
            deviceId: getDeviceId(),
            deviceFingerprint: nil,
            deviceInfo: getDeviceInfo(),
            fcmToken: FCMTokenManager.shared.getToken()
        )

        let response: APIResponse<TokenResponse> = try await APIClient.shared.request(
            endpoint: "/auth/buyer/otp/verify",
            method: .post,
            body: request,
            requiresAuth: false
        )

        guard response.success, let data = response.data else {
            throw APIError.serverError(response.message ?? "Failed to verify OTP")
        }

        // Save tokens
        keychain.saveAccessToken(data.accessToken)
        keychain.saveRefreshToken(data.refreshToken)

        // Update state on main thread
        await MainActor.run {
            self.currentUser = Buyer(
                id: data.userId,
                name: data.name,
                email: data.email
            )
            self.isAuthenticated = true
        }
    }

    // MARK: - Token Refresh

    func refreshToken() async throws -> Bool {
        guard let refreshToken = keychain.getRefreshToken() else {
            await logout()
            return false
        }

        let request = RefreshTokenRequest(
            refreshToken: refreshToken,
            deviceId: getDeviceId()
        )

        do {
            let response: APIResponse<TokenResponse> = try await APIClient.shared.request(
                endpoint: "/auth/buyer/refresh",
                method: .post,
                body: request,
                requiresAuth: false
            )

            guard response.success, let data = response.data else {
                await logout()
                return false
            }

            keychain.saveAccessToken(data.accessToken)
            keychain.saveRefreshToken(data.refreshToken)
            return true
        } catch {
            await logout()
            return false
        }
    }

    // MARK: - Logout

    func logout() async {
        // Try to call logout API
        do {
            let _: APIResponse<EmptyResponse> = try await APIClient.shared.request(
                endpoint: "/auth/buyer/logout",
                method: .post,
                requiresAuth: true
            )
        } catch {
            // Ignore logout errors
        }

        // Clear tokens
        keychain.clearTokens()

        // Update state on main thread
        await MainActor.run {
            self.isAuthenticated = false
            self.currentUser = nil
        }
    }

    // MARK: - Helpers

    private func getDeviceId() -> String {
        if let id = UserDefaults.standard.string(forKey: "deviceId") {
            return id
        }
        let newId = UUID().uuidString
        UserDefaults.standard.set(newId, forKey: "deviceId")
        return newId
    }

    private func getDeviceInfo() -> String {
        return "iOS \(UIDevice.current.systemVersion) - \(UIDevice.current.model)"
    }
}

struct EmptyResponse: Codable {}
