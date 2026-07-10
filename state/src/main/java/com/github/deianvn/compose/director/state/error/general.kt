package com.github.deianvn.compose.director.state.error

import com.github.deianvn.compose.director.state.error.GeneralError.ClientError
import com.github.deianvn.compose.director.state.error.GeneralError.IOError
import com.github.deianvn.compose.director.state.error.GeneralError.ServerError
import com.github.deianvn.compose.director.state.error.GeneralError.UnknownError
import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException


open class StateError(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

sealed class GeneralError(
    message: String? = null,
    cause: Throwable? = null
) : StateError(message = message, cause = cause) {

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


fun toStateError(
    error: Throwable,
    handler: (Throwable) -> StateError? = { _ -> null }
): StateError {

    val stateError = handler(error) ?: when {
        error is StateError -> error
        error is IOException -> IOError(error)
        error is HttpException && error.code() == 401 -> GeneralError.AuthenticationError(cause = error)
        error is HttpException && error.code() == 403 -> GeneralError.AuthorizationError(cause = error)
        error is HttpException && error.code() in 400..499 -> ClientError(
            cause = error, code = error.code()
        )

        error is HttpException && error.code() in 500..599 -> ServerError(
            cause = error, code = error.code()
        )

        error is JSONException -> GeneralError.SerializationError(error)
        else -> UnknownError(cause = error)
    }

    return stateError
}
