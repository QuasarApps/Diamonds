package com.example.diamonds.common.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import com.example.diamonds.domain.model.Result

/**
 * Convert a Flow<T> to Flow<Result<T>>
 */
fun <T> Flow<T>.asResultFlow(): Flow<Result<T>> =
    map<T, Result<T>> { Result.Success(it) }
        .catch { emit(Result.Error(it as? Exception ?: Exception(it))) }

/**
 * Filter Flow by predicate
 */
fun <T> Flow<T?>.filterNotNull(): Flow<T> =
    filter { it != null }
        .map { it as T }

/**
 * Collect flow with error handling
 */
suspend inline fun <T> Flow<Result<T>>.collectResult(
    crossinline onSuccess: suspend (T) -> Unit,
    crossinline onError: suspend (Exception) -> Unit,
    crossinline onLoading: suspend () -> Unit
) {
    collect { result ->
        when (result) {
            is Result.Success -> onSuccess(result.data)
            is Result.Error -> onError(result.exception)
            is Result.Loading -> onLoading()
        }
    }
}
