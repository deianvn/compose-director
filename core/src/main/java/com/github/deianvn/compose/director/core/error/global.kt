package com.github.deianvn.compose.director.core.error

import java.io.IOException


open class StageError(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

sealed class GeneralError(
    message: String? = null,
    cause: Throwable? = null
) : StageError(message = message, cause = cause) {

    class UnknownError(
        message: String? = null,
        cause: Throwable? = null
    ) : GeneralError(message = message, cause = cause)

    class FatalError(
        message: String? = null,
        cause: Throwable? = null
    ) : GeneralError(message = message, cause = cause)

    class AuthenticationError(
        message: String? = null,
        cause: Throwable? = null
    ) : GeneralError(message = message, cause = cause)

    class AuthorizationError(
        message: String? = null,
        cause: Throwable? = null
    ) : GeneralError(message = message, cause = cause)

    class ClientError(
        message: String? = null,
        cause: Throwable? = null,
        val code: Int
    ) : GeneralError(message = message, cause = cause)

    class ServerError(
        message: String? = null,
        cause: Throwable? = null,
        val code: Int
    ) : GeneralError(message = message, cause = cause)

    class IOError(
        val ioException: IOException
    ) : GeneralError(cause = ioException)

    class SerializationError(
        cause: Throwable? = null
    ) : GeneralError(cause = cause)

}
