import Foundation
import Combine
import os.log

/// Centralized error handler for the application.
/// Provides error classification, logging, and user-friendly message generation.
@MainActor
final class ErrorHandler: ObservableObject {
    static let shared = ErrorHandler()

    // MARK: - Published Properties
    @Published private(set) var currentError: AppError?
    @Published private(set) var errorHistory: [ErrorLogEntry] = []

    // MARK: - Private Properties
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "PoultryBuyer", category: "ErrorHandler")
    private let maxHistorySize = 50

    // MARK: - Error Event Publisher
    private let errorSubject = PassthroughSubject<ErrorEvent, Never>()
    var errorPublisher: AnyPublisher<ErrorEvent, Never> {
        errorSubject.eraseToAnyPublisher()
    }

    private init() {}

    // MARK: - Public Methods

    /// Handles an error and converts it to an AppError
    /// - Parameters:
    ///   - error: The error to handle
    ///   - context: Optional context information
    ///   - silent: If true, won't emit error events
    /// - Returns: The converted AppError
    @discardableResult
    func handle(_ error: Error, context: ErrorContext? = nil, silent: Bool = false) -> AppError {
        let appError = AppError.from(error)

        // Log the error
        log(appError, originalError: error, context: context)

        // Emit error event
        if !silent {
            let event = ErrorEvent(error: appError, context: context, timestamp: Date())
            errorSubject.send(event)
            currentError = appError
        }

        return appError
    }

    /// Handles an API response error
    /// - Parameters:
    ///   - statusCode: HTTP status code
    ///   - message: Optional error message from server
    ///   - context: Optional context information
    /// - Returns: The converted AppError
    @discardableResult
    func handleAPIError(statusCode: Int, message: String? = nil, context: ErrorContext? = nil) -> AppError {
        let appError = AppError.from(statusCode: statusCode, message: message)

        // Log the error
        log(appError, originalError: nil, context: context)

        // Emit error event
        let event = ErrorEvent(error: appError, context: context, timestamp: Date())
        errorSubject.send(event)
        currentError = appError

        return appError
    }

    /// Clears the current error
    func clearError() {
        currentError = nil
    }

    /// Gets a user-friendly message for the error
    func getMessage(for error: AppError) -> String {
        return error.localizedMessage
    }

    /// Gets a title for the error
    func getTitle(for error: AppError) -> String {
        return error.title
    }

    /// Checks if the error is recoverable
    func isRecoverable(_ error: AppError) -> Bool {
        return error.isRecoverable
    }

    /// Checks if the error requires re-authentication
    func requiresAuthentication(_ error: AppError) -> Bool {
        return error.requiresAuthentication
    }

    // MARK: - Private Methods

    private func log(_ appError: AppError, originalError: Error?, context: ErrorContext?) {
        let entry = ErrorLogEntry(
            error: appError,
            context: context,
            timestamp: Date()
        )

        // Add to history
        errorHistory.insert(entry, at: 0)
        if errorHistory.count > maxHistorySize {
            errorHistory.removeLast()
        }

        // Log based on severity
        let message = buildLogMessage(appError: appError, originalError: originalError, context: context)

        switch appError.category {
        case .network:
            if case .noConnection = appError {
                logger.info("\(message)")
            } else {
                logger.warning("\(message)")
            }
        case .server:
            logger.error("\(message)")
        case .authentication:
            logger.warning("\(message)")
        case .client:
            logger.info("\(message)")
        case .business:
            logger.info("\(message)")
        case .unknown:
            logger.error("\(message)")
        }

        // TODO: Send to crash reporting service (Firebase Crashlytics, Sentry, etc.)
        // CrashlyticsManager.shared.logError(appError, context: context)
    }

    private func buildLogMessage(appError: AppError, originalError: Error?, context: ErrorContext?) -> String {
        var components: [String] = []

        components.append("Error: \(appError.title)")
        components.append("Code: \(appError.errorCode)")
        components.append("Message: \(appError.localizedMessage)")

        if let context = context {
            if let screen = context.screen {
                components.append("Screen: \(screen)")
            }
            if let action = context.action {
                components.append("Action: \(action)")
            }
        }

        if let originalError = originalError {
            components.append("Original: \(originalError.localizedDescription)")
        }

        return components.joined(separator: " | ")
    }
}

// MARK: - Supporting Types

/// Context information for error handling
struct ErrorContext: Sendable {
    let screen: String?
    let action: String?
    let additionalInfo: [String: String]

    init(
        screen: String? = nil,
        action: String? = nil,
        additionalInfo: [String: String] = [:]
    ) {
        self.screen = screen
        self.action = action
        self.additionalInfo = additionalInfo
    }
}

/// Event emitted when an error occurs
struct ErrorEvent: Sendable {
    let error: AppError
    let context: ErrorContext?
    let timestamp: Date
}

/// Entry in the error history log
struct ErrorLogEntry: Identifiable {
    let id = UUID()
    let error: AppError
    let context: ErrorContext?
    let timestamp: Date
}

// MARK: - Result Extension

extension Result {
    /// Converts a Result to handle errors through ErrorHandler
    @MainActor
    func handleError(
        handler: ErrorHandler = .shared,
        context: ErrorContext? = nil,
        silent: Bool = false
    ) -> Result<Success, AppError> {
        switch self {
        case .success(let value):
            return .success(value)
        case .failure(let error):
            let appError = handler.handle(error, context: context, silent: silent)
            return .failure(appError)
        }
    }
}

// MARK: - Safe API Call

/// Executes an async operation safely and returns a Result
/// - Parameters:
///   - context: Optional error context
///   - operation: The async operation to execute
/// - Returns: Result containing success value or AppError
func safeAPICall<T>(
    context: ErrorContext? = nil,
    _ operation: @escaping () async throws -> T
) async -> Result<T, AppError> {
    do {
        let result = try await operation()
        return .success(result)
    } catch {
        let appError = await ErrorHandler.shared.handle(error, context: context)
        return .failure(appError)
    }
}

/// Executes an async operation safely without returning a result
/// - Parameters:
///   - context: Optional error context
///   - onError: Callback when error occurs
///   - operation: The async operation to execute
func performSafely(
    context: ErrorContext? = nil,
    onError: ((AppError) -> Void)? = nil,
    _ operation: @escaping () async throws -> Void
) async {
    do {
        try await operation()
    } catch {
        let appError = await ErrorHandler.shared.handle(error, context: context)
        onError?(appError)
    }
}
