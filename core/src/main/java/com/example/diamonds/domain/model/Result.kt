package com.example.diamonds.domain.model

/**
 * Sealed class for type-safe result handling
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    val valueOrNull: T? get() = (this as? Success)?.data
}

/**
 * Custom exception for offline operations
 */
class OfflineException(message: String = "Operation requires internet connection") : Exception(message)

/**
 * Custom exception for sync failures
 */
class SyncException(message: String, cause: Exception? = null) : Exception(message, cause)

/**
 * Custom exception for server errors
 */
data class ServerException(val statusCode: Int, val errorMessage: String) : Exception("$statusCode: $errorMessage")
