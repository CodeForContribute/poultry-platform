package com.poultry.buyer.core.error

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handler for the application.
 * Provides error classification, logging, and user-friendly message generation.
 */
@Singleton
class ErrorHandler @Inject constructor() {

    private val _errorEvents = MutableSharedFlow<ErrorEvent>(extraBufferCapacity = 10)
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()

    companion object {
        private const val TAG = "ErrorHandler"
    }

    /**
     * Handles an exception and converts it to an AppError.
     * Logs the error and emits an error event.
     */
    fun handleException(
        throwable: Throwable,
        context: ErrorContext? = null,
        silent: Boolean = false
    ): AppError {
        // Don't process cancellation exceptions
        if (throwable is CancellationException) throw throwable

        val appError = classifyError(throwable, context)

        // Log the error
        logError(appError, throwable)

        // Emit error event unless silent
        if (!silent) {
            _errorEvents.tryEmit(ErrorEvent(appError, context))
        }

        return appError
    }

    /**
     * Classifies a throwable into an AppError
     */
    private fun classifyError(throwable: Throwable, context: ErrorContext?): AppError {
        val networkError = NetworkError.from(throwable)

        return when (networkError) {
            is NetworkError.NoConnection -> AppError.Network(
                type = NetworkErrorType.NO_CONNECTION,
                message = networkError.message,
                isRecoverable = true
            )
            is NetworkError.Timeout -> AppError.Network(
                type = NetworkErrorType.TIMEOUT,
                message = networkError.message,
                isRecoverable = true
            )
            is NetworkError.ServerError -> AppError.Server(
                httpCode = networkError.httpCode,
                message = networkError.message,
                isRecoverable = true
            )
            is NetworkError.Unauthorized -> AppError.Auth(
                type = AuthErrorType.SESSION_EXPIRED,
                message = networkError.message
            )
            is NetworkError.Forbidden -> AppError.Auth(
                type = AuthErrorType.FORBIDDEN,
                message = networkError.message
            )
            is NetworkError.ValidationError -> AppError.Validation(
                message = networkError.message,
                fieldErrors = networkError.fieldErrors
            )
            is NetworkError.NotFound -> AppError.NotFound(
                message = networkError.message
            )
            is NetworkError.RateLimited -> AppError.RateLimited(
                retryAfterSeconds = networkError.retryAfterSeconds,
                message = networkError.message
            )
            is NetworkError.SSLError -> AppError.Network(
                type = NetworkErrorType.SSL_ERROR,
                message = networkError.message,
                isRecoverable = false
            )
            is NetworkError.Unknown -> AppError.Unknown(
                message = networkError.message,
                cause = throwable
            )
        }
    }

    /**
     * Logs the error with appropriate severity
     */
    private fun logError(error: AppError, throwable: Throwable) {
        val logMessage = buildLogMessage(error, throwable)

        when (error) {
            is AppError.Network -> {
                if (error.type == NetworkErrorType.NO_CONNECTION) {
                    Log.w(TAG, logMessage)
                } else {
                    Log.e(TAG, logMessage, throwable)
                }
            }
            is AppError.Server -> Log.e(TAG, logMessage, throwable)
            is AppError.Auth -> Log.w(TAG, logMessage)
            is AppError.Validation -> Log.d(TAG, logMessage)
            is AppError.NotFound -> Log.w(TAG, logMessage)
            is AppError.RateLimited -> Log.w(TAG, logMessage)
            is AppError.Business -> Log.i(TAG, logMessage)
            is AppError.Unknown -> Log.e(TAG, logMessage, throwable)
        }

        // TODO: Send to crash reporting service (Crashlytics, Sentry, etc.)
        // reportToCrashlytics(error, throwable)
    }

    private fun buildLogMessage(error: AppError, throwable: Throwable): String {
        return buildString {
            append("Error: ${error::class.simpleName}")
            append(" | Message: ${error.userMessage}")
            append(" | Original: ${throwable.message}")
            if (error is AppError.Server) {
                append(" | HTTP Code: ${error.httpCode}")
            }
        }
    }

    /**
     * Gets a user-friendly message for display
     */
    fun getUserMessage(error: AppError): String = error.userMessage

    /**
     * Gets a title for the error dialog
     */
    fun getErrorTitle(error: AppError): String {
        return when (error) {
            is AppError.Network -> when (error.type) {
                NetworkErrorType.NO_CONNECTION -> "No Internet Connection"
                NetworkErrorType.TIMEOUT -> "Connection Timeout"
                NetworkErrorType.SSL_ERROR -> "Security Error"
            }
            is AppError.Server -> "Server Error"
            is AppError.Auth -> when (error.type) {
                AuthErrorType.SESSION_EXPIRED -> "Session Expired"
                AuthErrorType.FORBIDDEN -> "Access Denied"
                AuthErrorType.INVALID_CREDENTIALS -> "Login Failed"
            }
            is AppError.Validation -> "Invalid Input"
            is AppError.NotFound -> "Not Found"
            is AppError.RateLimited -> "Too Many Requests"
            is AppError.Business -> "Error"
            is AppError.Unknown -> "Error"
        }
    }

    /**
     * Checks if the error is recoverable (can be retried)
     */
    fun isRecoverable(error: AppError): Boolean {
        return when (error) {
            is AppError.Network -> error.isRecoverable
            is AppError.Server -> error.isRecoverable
            is AppError.RateLimited -> true
            is AppError.Unknown -> true
            else -> false
        }
    }
}

/**
 * Sealed class representing application-level errors
 */
sealed class AppError(
    open val userMessage: String,
    open val code: Int = 0
) {
    data class Network(
        val type: NetworkErrorType,
        override val userMessage: String,
        val isRecoverable: Boolean
    ) : AppError(userMessage, type.code)

    data class Server(
        val httpCode: Int,
        override val userMessage: String,
        val isRecoverable: Boolean
    ) : AppError(userMessage, ErrorCode.SERVER_ERROR)

    data class Auth(
        val type: AuthErrorType,
        override val userMessage: String
    ) : AppError(userMessage, type.code)

    data class Validation(
        override val userMessage: String,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : AppError(userMessage, ErrorCode.VALIDATION_ERROR)

    data class NotFound(
        override val userMessage: String
    ) : AppError(userMessage, ErrorCode.NOT_FOUND)

    data class RateLimited(
        val retryAfterSeconds: Int?,
        override val userMessage: String
    ) : AppError(userMessage, ErrorCode.RATE_LIMITED)

    data class Business(
        override val code: Int,
        override val userMessage: String
    ) : AppError(userMessage, code)

    data class Unknown(
        override val userMessage: String,
        val cause: Throwable? = null
    ) : AppError(userMessage, ErrorCode.UNKNOWN)
}

enum class NetworkErrorType(val code: Int) {
    NO_CONNECTION(ErrorCode.NO_CONNECTION),
    TIMEOUT(ErrorCode.TIMEOUT),
    SSL_ERROR(ErrorCode.SSL_ERROR)
}

enum class AuthErrorType(val code: Int) {
    SESSION_EXPIRED(ErrorCode.UNAUTHORIZED),
    FORBIDDEN(ErrorCode.FORBIDDEN),
    INVALID_CREDENTIALS(ErrorCode.UNAUTHORIZED)
}

/**
 * Context information for error handling
 */
data class ErrorContext(
    val screen: String? = null,
    val action: String? = null,
    val additionalInfo: Map<String, Any> = emptyMap()
)

/**
 * Event emitted when an error occurs
 */
data class ErrorEvent(
    val error: AppError,
    val context: ErrorContext?
)

/**
 * Extension function to execute a block safely and return a Result
 */
suspend fun <T> safeApiCall(
    errorHandler: ErrorHandler,
    context: ErrorContext? = null,
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        val appError = errorHandler.handleException(e, context)
        Result.failure(AppException(appError))
    }
}

/**
 * Exception wrapper for AppError
 */
class AppException(val appError: AppError) : Exception(appError.userMessage)

/**
 * Extension function to get AppError from Result failure
 */
fun <T> Result<T>.getAppError(): AppError? {
    return exceptionOrNull()?.let {
        if (it is AppException) it.appError else null
    }
}
