import Foundation
import Combine

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
            fcmToken: nil // TODO: Get FCM token
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
