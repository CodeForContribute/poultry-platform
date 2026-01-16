package com.poultry.buyer.core.error

/**
 * Generic sealed class representing different UI states for data loading operations.
 * Provides a type-safe way to handle loading, success, and error states.
 */
sealed class UiState<out T> {
    /**
     * Initial state before any data loading
     */
    data object Idle : UiState<Nothing>()

    /**
     * Data is being loaded
     */
    data class Loading<T>(val cachedData: T? = null) : UiState<T>()

    /**
     * Data loaded successfully
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Error occurred during data loading
     */
    data class Error<T>(
        val error: AppError,
        val cachedData: T? = null
    ) : UiState<T>()

    /**
     * Check if currently loading
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Check if there's an error
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Check if successful
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Get data if available (from success or cached in error/loading states)
     */
    fun getDataOrNull(): T? = when (this) {
        is Idle -> null
        is Loading -> cachedData
        is Success -> data
        is Error -> cachedData
    }

    /**
     * Get error if available
     */
    fun getErrorOrNull(): AppError? = when (this) {
        is Error -> error
        else -> null
    }

    /**
     * Map the success data to a new type
     */
    fun <R> map(transform: (T) -> R): UiState<R> = when (this) {
        is Idle -> Idle
        is Loading -> Loading(cachedData?.let(transform))
        is Success -> Success(transform(data))
        is Error -> Error(error, cachedData?.let(transform))
    }

    /**
     * Execute action on success
     */
    inline fun onSuccess(action: (T) -> Unit): UiState<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Execute action on error
     */
    inline fun onError(action: (AppError) -> Unit): UiState<T> {
        if (this is Error) action(error)
        return this
    }

    /**
     * Execute action on loading
     */
    inline fun onLoading(action: () -> Unit): UiState<T> {
        if (this is Loading) action()
        return this
    }
}

/**
 * Converts a Result to UiState
 */
fun <T> Result<T>.toUiState(): UiState<T> {
    return fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { throwable ->
            val appError = when (throwable) {
                is AppException -> throwable.appError
                else -> AppError.Unknown(
                    userMessage = throwable.message ?: "An unexpected error occurred",
                    cause = throwable
                )
            }
            UiState.Error(appError)
        }
    )
}

/**
 * Action result for operations that don't return data
 */
sealed class ActionResult {
    data object Idle : ActionResult()
    data object Loading : ActionResult()
    data object Success : ActionResult()
    data class Error(val error: AppError) : ActionResult()

    val isLoading: Boolean get() = this is Loading
    val isError: Boolean get() = this is Error
    val isSuccess: Boolean get() = this is Success
}

/**
 * Converts a Result<Unit> to ActionResult
 */
fun Result<Unit>.toActionResult(): ActionResult {
    return fold(
        onSuccess = { ActionResult.Success },
        onFailure = { throwable ->
            val appError = when (throwable) {
                is AppException -> throwable.appError
                else -> AppError.Unknown(
                    userMessage = throwable.message ?: "An unexpected error occurred",
                    cause = throwable
                )
            }
            ActionResult.Error(appError)
        }
    )
}

/**
 * Data class for paginated data loading states
 */
data class PaginatedState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 0
) {
    val isEmpty: Boolean get() = items.isEmpty() && !isLoading

    fun startLoading(isRefresh: Boolean = false) = copy(
        isLoading = isRefresh || items.isEmpty(),
        isLoadingMore = !isRefresh && items.isNotEmpty(),
        error = null
    )

    fun success(newItems: List<T>, hasMore: Boolean, page: Int) = copy(
        items = if (page == 0) newItems else items + newItems,
        isLoading = false,
        isLoadingMore = false,
        hasMore = hasMore,
        currentPage = page
    )

    fun error(appError: AppError) = copy(
        isLoading = false,
        isLoadingMore = false,
        error = appError
    )

    fun clearError() = copy(error = null)
}
