package com.poultry.buyer.core.error

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Sealed class representing different types of network errors.
 * Provides type-safe error handling with associated error codes and messages.
 */
sealed class NetworkError(
    val code: Int,
    val message: String,
    val cause: Throwable? = null
) {
    /**
     * No internet connection available
     */
    data class NoConnection(
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.NO_CONNECTION,
        message = "No internet connection. Please check your network settings.",
        cause = cause
    )

    /**
     * Request timed out
     */
    data class Timeout(
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.TIMEOUT,
        message = "Request timed out. Please try again.",
        cause = cause
    )

    /**
     * Server error (5xx status codes)
     */
    data class ServerError(
        val httpCode: Int,
        val serverMessage: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.SERVER_ERROR,
        message = serverMessage ?: "Server error. Please try again later.",
        cause = cause
    )

    /**
     * Authentication error (401)
     */
    data class Unauthorized(
        val serverMessage: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.UNAUTHORIZED,
        message = serverMessage ?: "Session expired. Please login again.",
        cause = cause
    )

    /**
     * Forbidden error (403)
     */
    data class Forbidden(
        val serverMessage: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.FORBIDDEN,
        message = serverMessage ?: "You don't have permission to perform this action.",
        cause = cause
    )

    /**
     * Resource not found (404)
     */
    data class NotFound(
        val serverMessage: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.NOT_FOUND,
        message = serverMessage ?: "Requested resource not found.",
        cause = cause
    )

    /**
     * Validation error (400, 422)
     */
    data class ValidationError(
        val httpCode: Int,
        val serverMessage: String? = null,
        val fieldErrors: Map<String, String> = emptyMap(),
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.VALIDATION_ERROR,
        message = serverMessage ?: "Invalid data provided.",
        cause = cause
    )

    /**
     * Rate limit exceeded (429)
     */
    data class RateLimited(
        val retryAfterSeconds: Int? = null,
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.RATE_LIMITED,
        message = "Too many requests. Please wait and try again.",
        cause = cause
    )

    /**
     * SSL/TLS error
     */
    data class SSLError(
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.SSL_ERROR,
        message = "Secure connection failed. Please check your device's date and time settings.",
        cause = cause
    )

    /**
     * Unknown error
     */
    data class Unknown(
        val originalMessage: String? = null,
        override val cause: Throwable? = null
    ) : NetworkError(
        code = ErrorCode.UNKNOWN,
        message = originalMessage ?: "An unexpected error occurred. Please try again.",
        cause = cause
    )

    companion object {
        /**
         * Creates a NetworkError from a Throwable
         */
        fun from(throwable: Throwable): NetworkError {
            return when (throwable) {
                is SocketTimeoutException -> Timeout(throwable)
                is UnknownHostException -> NoConnection(throwable)
                is IOException -> {
                    if (throwable is SSLException) {
                        SSLError(throwable)
                    } else {
                        NoConnection(throwable)
                    }
                }
                is HttpException -> fromHttpException(throwable)
                else -> Unknown(throwable.message, throwable)
            }
        }

        /**
         * Creates a NetworkError from an HttpException
         */
        private fun fromHttpException(exception: HttpException): NetworkError {
            val code = exception.code()
            val errorBody = exception.response()?.errorBody()?.string()

            return when (code) {
                400, 422 -> ValidationError(
                    httpCode = code,
                    serverMessage = errorBody,
                    cause = exception
                )
                401 -> Unauthorized(
                    serverMessage = errorBody,
                    cause = exception
                )
                403 -> Forbidden(
                    serverMessage = errorBody,
                    cause = exception
                )
                404 -> NotFound(
                    serverMessage = errorBody,
                    cause = exception
                )
                429 -> {
                    val retryAfter = exception.response()?.headers()?.get("Retry-After")?.toIntOrNull()
                    RateLimited(
                        retryAfterSeconds = retryAfter,
                        cause = exception
                    )
                }
                in 500..599 -> ServerError(
                    httpCode = code,
                    serverMessage = errorBody,
                    cause = exception
                )
                else -> Unknown(
                    originalMessage = errorBody ?: exception.message(),
                    cause = exception
                )
            }
        }
    }
}

/**
 * Error codes for categorizing errors
 */
object ErrorCode {
    const val NO_CONNECTION = 1001
    const val TIMEOUT = 1002
    const val SERVER_ERROR = 1003
    const val UNAUTHORIZED = 1004
    const val FORBIDDEN = 1005
    const val NOT_FOUND = 1006
    const val VALIDATION_ERROR = 1007
    const val RATE_LIMITED = 1008
    const val SSL_ERROR = 1009
    const val UNKNOWN = 1099

    // Business logic error codes
    const val INSUFFICIENT_STOCK = 2001
    const val PAYMENT_FAILED = 2002
    const val ORDER_CANCELLED = 2003
    const val INVALID_COUPON = 2004
    const val MINIMUM_ORDER_NOT_MET = 2005
}

/**
 * Extension function to check if error is recoverable (can be retried)
 */
fun NetworkError.isRecoverable(): Boolean {
    return when (this) {
        is NetworkError.NoConnection -> true
        is NetworkError.Timeout -> true
        is NetworkError.ServerError -> true
        is NetworkError.RateLimited -> true
        is NetworkError.Unauthorized -> false
        is NetworkError.Forbidden -> false
        is NetworkError.NotFound -> false
        is NetworkError.ValidationError -> false
        is NetworkError.SSLError -> false
        is NetworkError.Unknown -> true
    }
}

/**
 * Extension function to get user-friendly title for the error
 */
fun NetworkError.getTitle(): String {
    return when (this) {
        is NetworkError.NoConnection -> "No Internet Connection"
        is NetworkError.Timeout -> "Connection Timeout"
        is NetworkError.ServerError -> "Server Error"
        is NetworkError.Unauthorized -> "Session Expired"
        is NetworkError.Forbidden -> "Access Denied"
        is NetworkError.NotFound -> "Not Found"
        is NetworkError.ValidationError -> "Invalid Input"
        is NetworkError.RateLimited -> "Too Many Requests"
        is NetworkError.SSLError -> "Security Error"
        is NetworkError.Unknown -> "Error"
    }
}
