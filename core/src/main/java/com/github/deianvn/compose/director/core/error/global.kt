package com.github.deianvn.compose.director.core.error

import java.io.IOException


sealed class StageError(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class FatalError(
    message: String? = null,
    cause: Throwable? = null
) : StageError(message = message, cause = cause)

class GeneralError(
    message: String? = null,
    cause: Throwable? = null
) : StageError(message = message, cause = cause)

class AuthenticationError(
    message: String? = null,
    cause: Throwable? = null
) : StageError(message = message, cause = cause)

class AuthorizationError(
    message: String? = null,
    cause: Throwable? = null
) : StageError(message = message, cause = cause)

class ClientError(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int
) : StageError(message = message, cause = cause)

class ServerError(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int
) : StageError(message = message, cause = cause)

class IOError(
    val ioException: IOException
) : StageError(cause = ioException)

class SerializationError(
    cause: Throwable? = null
) : StageError(cause = cause)
