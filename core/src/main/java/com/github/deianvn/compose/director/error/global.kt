package com.github.deianvn.compose.director.error

import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException


sealed class SceneFault(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class FatalFault(
    message: String? = null,
    cause: Throwable? = null
) : SceneFault(message = message, cause = cause)

class GeneralFault(
    message: String? = null,
    cause: Throwable? = null
) : SceneFault(message = message, cause = cause)

class AuthenticationFault(
    message: String? = null,
    cause: Throwable? = null
) : SceneFault(message = message, cause = cause)

class AuthorizationFault(
    message: String? = null,
    cause: Throwable? = null
) : SceneFault(message = message, cause = cause)

class ClientFault(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int
) : SceneFault(message = message, cause = cause)

class ServerFault(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int
) : SceneFault(message = message, cause = cause)

class NetworkFault(
    val ioException: IOException
) : SceneFault(cause = ioException)

class SerializationFault(
    cause: Throwable? = null
) : SceneFault(cause = cause)

class DiskReadWriteFault(
    cause: Throwable? = null
) : SceneFault(cause = cause)


fun toSceneFault(
    error: Throwable,
    errorContext: ErrorContext
): SceneFault {
    val stateError = when {
        error is SceneFault -> error
        error is IOException -> {
            when (errorContext) {
                ErrorContext.API -> NetworkFault(ioException = error)
                ErrorContext.SHARED_PREFERENCES -> DiskReadWriteFault(cause = error)
            }
        }

        error is HttpException && error.code() == 401 -> AuthenticationFault(cause = error)
        error is HttpException && error.code() == 403 -> AuthorizationFault(cause = error)
        error is HttpException && error.code() in 400..499 -> ClientFault(
            cause = error, code = error.code()
        )
        error is HttpException && error.code() in 500..599 -> ServerFault(
            cause = error, code = error.code()
        )
        error is JSONException -> SerializationFault(error)
        else -> GeneralFault(cause = error)
    }

    return stateError
}
