package com.example.core.common

import kotlinx.coroutines.CancellationException

sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null,
) {
    data class Network(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Unauthorized(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class NotFound(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Storage(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class PdfRender(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Cancelled(
        override val message: String = "Operation cancelled",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause)
}

sealed interface AppResult<out T> {
    data object Loading : AppResult<Nothing>
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Loading -> AppResult.Loading
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

suspend inline fun <T> safeSuspendCall(
    crossinline errorMapper: (Throwable) -> AppError = { throwable ->
        AppError.Unknown(
            message = throwable.localizedMessage ?: "Unexpected error",
            cause = throwable,
        )
    },
    crossinline block: suspend () -> T,
): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (throwable: Throwable) {
        AppLogger.e("safeSuspendCall", throwable.message ?: "Call failed", throwable)
        AppResult.Error(errorMapper(throwable))
    }
}
