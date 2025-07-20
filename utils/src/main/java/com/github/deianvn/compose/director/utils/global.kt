package com.github.deianvn.compose.director.utils

import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException


sealed class SceneError(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class FatalError(
    message: String? = null,
    cause: Throwable? = null
) : SceneError(message = message, cause = cause)

class GeneralError(
    message: String? = null,
    cause: Throwable? = null
) : SceneError(message = message, cause = cause)

class AuthenticationError(
    message: String? = null,
    cause: Throwable? = null
) : SceneError(message = message, cause = cause)

class AuthorizationError(
    message: String? = null,
    cause: Throwable? = null
) : SceneError(message = message, cause = cause)

class ClientError(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int
) : SceneError(message = message, cause = cause)

class ServerError(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int
) : SceneError(message = message, cause = cause)

class NetworkError(
    val ioException: IOException
) : SceneError(cause = ioException)

class SerializationError(
    cause: Throwable? = null
) : SceneError(cause = cause)

class DiskReadWriteError(
    cause: Throwable? = null
) : SceneError(cause = cause)


fun toSceneError(
    error: Throwable,
    errorContext: ErrorContext
): SceneError {
    val stateError = when {
        error is SceneError -> error
        error is IOException -> {
            when (errorContext) {
                ErrorContext.API -> NetworkError(ioException = error)
                ErrorContext.SHARED_PREFERENCES -> DiskReadWriteError(cause = error)
            }
        }

        error is HttpException && error.code() == 401 -> AuthenticationError(cause = error)
        error is HttpException && error.code() == 403 -> AuthorizationError(cause = error)
        error is HttpException && error.code() in 400..499 -> ClientError(
            cause = error, code = error.code()
        )
        error is HttpException && error.code() in 500..599 -> ServerError(
            cause = error, code = error.code()
        )
        error is JSONException -> SerializationError(error)
        else -> GeneralError(cause = error)
    }

    return stateError
}
