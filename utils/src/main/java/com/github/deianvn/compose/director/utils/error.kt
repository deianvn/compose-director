package com.github.deianvn.compose.director.utils

import com.github.deianvn.compose.director.core.error.AuthenticationError
import com.github.deianvn.compose.director.core.error.AuthorizationError
import com.github.deianvn.compose.director.core.error.ClientError
import com.github.deianvn.compose.director.core.error.GeneralError
import com.github.deianvn.compose.director.core.error.IOError
import com.github.deianvn.compose.director.core.error.SerializationError
import com.github.deianvn.compose.director.core.error.ServerError
import com.github.deianvn.compose.director.core.error.StageError
import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException


fun toStageError(
    error: Throwable,
    handler: (Throwable) -> StageError? = { _ -> null }
): StageError {

    val stageError = handler(error) ?: when {
        error is StageError -> error
        error is IOException -> IOError(error)
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

    return stageError
}
