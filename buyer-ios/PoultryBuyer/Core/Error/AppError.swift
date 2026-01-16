import Foundation

/// Comprehensive error type for the application with associated values
/// for detailed error information and user-facing messages.
enum AppError: Error, Equatable {
    // MARK: - Network Errors
    case noConnection
    case timeout
    case sslError(String?)

    // MARK: - Server Errors
    case serverError(statusCode: Int, message: String?)

    // MARK: - Authentication Errors
    case unauthorized(message: String?)
    case forbidden(message: String?)
    case sessionExpired

    // MARK: - Client Errors
    case notFound(message: String?)
    case validationError(message: String?, fieldErrors: [String: String])
    case rateLimited(retryAfter: Int?)

    // MARK: - Business Logic Errors
    case insufficientStock(productName: String?)
    case paymentFailed(reason: String?)
    case orderCancelled(reason: String?)
    case minimumOrderNotMet(minimum: Double?)
    case invalidCoupon(reason: String?)

    // MARK: - Generic Errors
    case decodingError(Error?)
    case unknown(Error?)

    // MARK: - Equatable
    static func == (lhs: AppError, rhs: AppError) -> Bool {
        switch (lhs, rhs) {
        case (.noConnection, .noConnection),
             (.timeout, .timeout),
             (.sessionExpired, .sessionExpired):
            return true
        case (.sslError(let l), .sslError(let r)):
            return l == r
        case (.serverError(let ls, let lm), .serverError(let rs, let rm)):
            return ls == rs && lm == rm
        case (.unauthorized(let l), .unauthorized(let r)),
             (.forbidden(let l), .forbidden(let r)),
             (.notFound(let l), .notFound(let r)):
            return l == r
        case (.validationError(let lm, let lf), .validationError(let rm, let rf)):
            return lm == rm && lf == rf
        case (.rateLimited(let l), .rateLimited(let r)):
            return l == r
        case (.insufficientStock(let l), .insufficientStock(let r)):
            return l == r
        case (.paymentFailed(let l), .paymentFailed(let r)),
             (.orderCancelled(let l), .orderCancelled(let r)),
             (.invalidCoupon(let l), .invalidCoupon(let r)):
            return l == r
        case (.minimumOrderNotMet(let l), .minimumOrderNotMet(let r)):
            return l == r
        case (.decodingError, .decodingError),
             (.unknown, .unknown):
            return true
        default:
            return false
        }
    }
}

// MARK: - User-Facing Messages
extension AppError {
    /// Returns a user-friendly error message
    var localizedMessage: String {
        switch self {
        case .noConnection:
            return NSLocalizedString(
                "error.no_connection",
                value: "No internet connection. Please check your network settings.",
                comment: "No internet connection error"
            )
        case .timeout:
            return NSLocalizedString(
                "error.timeout",
                value: "Request timed out. Please try again.",
                comment: "Request timeout error"
            )
        case .sslError:
            return NSLocalizedString(
                "error.ssl",
                value: "Secure connection failed. Please check your device's date and time settings.",
                comment: "SSL error"
            )
        case .serverError(let statusCode, let message):
            if let message = message, !message.isEmpty {
                return message
            }
            return String(
                format: NSLocalizedString(
                    "error.server",
                    value: "Server error (Code: %d). Please try again later.",
                    comment: "Server error"
                ),
                statusCode
            )
        case .unauthorized(let message):
            return message ?? NSLocalizedString(
                "error.unauthorized",
                value: "Your session has expired. Please login again.",
                comment: "Unauthorized error"
            )
        case .forbidden(let message):
            return message ?? NSLocalizedString(
                "error.forbidden",
                value: "You don't have permission to perform this action.",
                comment: "Forbidden error"
            )
        case .sessionExpired:
            return NSLocalizedString(
                "error.session_expired",
                value: "Your session has expired. Please login again.",
                comment: "Session expired error"
            )
        case .notFound(let message):
            return message ?? NSLocalizedString(
                "error.not_found",
                value: "The requested resource was not found.",
                comment: "Not found error"
            )
        case .validationError(let message, _):
            return message ?? NSLocalizedString(
                "error.validation",
                value: "Please check your input and try again.",
                comment: "Validation error"
            )
        case .rateLimited(let retryAfter):
            if let seconds = retryAfter {
                return String(
                    format: NSLocalizedString(
                        "error.rate_limited_retry",
                        value: "Too many requests. Please wait %d seconds and try again.",
                        comment: "Rate limited with retry after"
                    ),
                    seconds
                )
            }
            return NSLocalizedString(
                "error.rate_limited",
                value: "Too many requests. Please wait and try again.",
                comment: "Rate limited error"
            )
        case .insufficientStock(let productName):
            if let name = productName {
                return String(
                    format: NSLocalizedString(
                        "error.insufficient_stock_product",
                        value: "Sorry, %@ is currently out of stock.",
                        comment: "Insufficient stock for product"
                    ),
                    name
                )
            }
            return NSLocalizedString(
                "error.insufficient_stock",
                value: "Sorry, this product is currently out of stock.",
                comment: "Insufficient stock error"
            )
        case .paymentFailed(let reason):
            return reason ?? NSLocalizedString(
                "error.payment_failed",
                value: "Payment failed. Please try again or use a different payment method.",
                comment: "Payment failed error"
            )
        case .orderCancelled(let reason):
            return reason ?? NSLocalizedString(
                "error.order_cancelled",
                value: "This order has been cancelled.",
                comment: "Order cancelled error"
            )
        case .minimumOrderNotMet(let minimum):
            if let min = minimum {
                return String(
                    format: NSLocalizedString(
                        "error.minimum_order_amount",
                        value: "Minimum order amount is Rs. %.2f",
                        comment: "Minimum order amount not met"
                    ),
                    min
                )
            }
            return NSLocalizedString(
                "error.minimum_order",
                value: "Minimum order amount not met.",
                comment: "Minimum order error"
            )
        case .invalidCoupon(let reason):
            return reason ?? NSLocalizedString(
                "error.invalid_coupon",
                value: "This coupon code is invalid or has expired.",
                comment: "Invalid coupon error"
            )
        case .decodingError:
            return NSLocalizedString(
                "error.decoding",
                value: "Unable to process the response. Please try again.",
                comment: "Decoding error"
            )
        case .unknown:
            return NSLocalizedString(
                "error.unknown",
                value: "An unexpected error occurred. Please try again.",
                comment: "Unknown error"
            )
        }
    }

    /// Returns a short title for the error
    var title: String {
        switch self {
        case .noConnection:
            return NSLocalizedString("error.title.no_connection", value: "No Connection", comment: "")
        case .timeout:
            return NSLocalizedString("error.title.timeout", value: "Timeout", comment: "")
        case .sslError:
            return NSLocalizedString("error.title.ssl", value: "Security Error", comment: "")
        case .serverError:
            return NSLocalizedString("error.title.server", value: "Server Error", comment: "")
        case .unauthorized, .sessionExpired:
            return NSLocalizedString("error.title.session", value: "Session Expired", comment: "")
        case .forbidden:
            return NSLocalizedString("error.title.forbidden", value: "Access Denied", comment: "")
        case .notFound:
            return NSLocalizedString("error.title.not_found", value: "Not Found", comment: "")
        case .validationError:
            return NSLocalizedString("error.title.validation", value: "Invalid Input", comment: "")
        case .rateLimited:
            return NSLocalizedString("error.title.rate_limited", value: "Too Many Requests", comment: "")
        case .insufficientStock:
            return NSLocalizedString("error.title.stock", value: "Out of Stock", comment: "")
        case .paymentFailed:
            return NSLocalizedString("error.title.payment", value: "Payment Failed", comment: "")
        case .orderCancelled:
            return NSLocalizedString("error.title.cancelled", value: "Order Cancelled", comment: "")
        case .minimumOrderNotMet:
            return NSLocalizedString("error.title.minimum", value: "Minimum Not Met", comment: "")
        case .invalidCoupon:
            return NSLocalizedString("error.title.coupon", value: "Invalid Coupon", comment: "")
        case .decodingError, .unknown:
            return NSLocalizedString("error.title.error", value: "Error", comment: "")
        }
    }
}

// MARK: - Error Classification
extension AppError {
    /// Error code for logging and analytics
    var errorCode: Int {
        switch self {
        case .noConnection: return 1001
        case .timeout: return 1002
        case .sslError: return 1003
        case .serverError(let code, _): return code
        case .unauthorized: return 401
        case .forbidden: return 403
        case .sessionExpired: return 401
        case .notFound: return 404
        case .validationError: return 400
        case .rateLimited: return 429
        case .insufficientStock: return 2001
        case .paymentFailed: return 2002
        case .orderCancelled: return 2003
        case .minimumOrderNotMet: return 2004
        case .invalidCoupon: return 2005
        case .decodingError: return 3001
        case .unknown: return 9999
        }
    }

    /// Whether the error can be retried
    var isRecoverable: Bool {
        switch self {
        case .noConnection, .timeout, .serverError, .rateLimited, .unknown:
            return true
        case .unauthorized, .sessionExpired, .forbidden, .notFound,
             .validationError, .sslError, .decodingError,
             .insufficientStock, .paymentFailed, .orderCancelled,
             .minimumOrderNotMet, .invalidCoupon:
            return false
        }
    }

    /// Whether the user needs to re-authenticate
    var requiresAuthentication: Bool {
        switch self {
        case .unauthorized, .sessionExpired:
            return true
        default:
            return false
        }
    }

    /// Error category for analytics
    var category: ErrorCategory {
        switch self {
        case .noConnection, .timeout, .sslError:
            return .network
        case .serverError:
            return .server
        case .unauthorized, .forbidden, .sessionExpired:
            return .authentication
        case .notFound, .validationError, .rateLimited:
            return .client
        case .insufficientStock, .paymentFailed, .orderCancelled,
             .minimumOrderNotMet, .invalidCoupon:
            return .business
        case .decodingError, .unknown:
            return .unknown
        }
    }
}

// MARK: - Error Category
enum ErrorCategory: String {
    case network = "Network"
    case server = "Server"
    case authentication = "Authentication"
    case client = "Client"
    case business = "Business"
    case unknown = "Unknown"
}

// MARK: - Factory Methods
extension AppError {
    /// Creates an AppError from a URLError
    static func from(urlError: URLError) -> AppError {
        switch urlError.code {
        case .notConnectedToInternet, .networkConnectionLost:
            return .noConnection
        case .timedOut:
            return .timeout
        case .secureConnectionFailed, .serverCertificateHasBadDate,
             .serverCertificateUntrusted, .serverCertificateHasUnknownRoot:
            return .sslError(urlError.localizedDescription)
        case .cannotFindHost, .cannotConnectToHost:
            return .noConnection
        default:
            return .unknown(urlError)
        }
    }

    /// Creates an AppError from an HTTP status code
    static func from(statusCode: Int, message: String? = nil) -> AppError {
        switch statusCode {
        case 400:
            return .validationError(message: message, fieldErrors: [:])
        case 401:
            return .unauthorized(message: message)
        case 403:
            return .forbidden(message: message)
        case 404:
            return .notFound(message: message)
        case 422:
            return .validationError(message: message, fieldErrors: [:])
        case 429:
            return .rateLimited(retryAfter: nil)
        case 500...599:
            return .serverError(statusCode: statusCode, message: message)
        default:
            return .unknown(nil)
        }
    }

    /// Creates an AppError from any Error
    static func from(_ error: Error) -> AppError {
        if let appError = error as? AppError {
            return appError
        }
        if let urlError = error as? URLError {
            return from(urlError: urlError)
        }
        if let decodingError = error as? DecodingError {
            return .decodingError(decodingError)
        }
        return .unknown(error)
    }
}
