import Foundation

/// App configuration that varies by environment (Debug vs Release)
enum AppConfig {

    /// Current environment
    enum Environment {
        case development
        case staging
        case production
    }

    /// The current build environment
    static var environment: Environment {
        #if DEBUG
        return .development
        #else
        // Check for staging flag in Info.plist or use production
        if let env = Bundle.main.infoDictionary?["APP_ENVIRONMENT"] as? String,
           env.lowercased() == "staging" {
            return .staging
        }
        return .production
        #endif
    }

    /// API base URL for the current environment
    static var apiBaseURL: String {
        switch environment {
        case .development:
            // Use localhost for simulator, or your local machine's IP for device
            #if targetEnvironment(simulator)
            return "http://localhost:8080/v1"
            #else
            // Replace with your machine's local IP when testing on device
            return "http://192.168.1.100:8080/v1"
            #endif
        case .staging:
            return "https://staging-api.poultry-platform.com/v1"
        case .production:
            return "https://api.poultry-platform.com/v1"
        }
    }

    /// Whether to enable verbose logging
    static var isLoggingEnabled: Bool {
        #if DEBUG
        return true
        #else
        return false
        #endif
    }

    /// App version string
    static var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    /// Build number
    static var buildNumber: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }
}
