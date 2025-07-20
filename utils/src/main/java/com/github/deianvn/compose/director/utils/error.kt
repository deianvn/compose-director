package com.github.deianvn.compose.director.utils

import com.github.deianvn.compose.director.core.error.GeneralError
import com.github.deianvn.compose.director.core.error.GeneralError.ClientError
import com.github.deianvn.compose.director.core.error.GeneralError.IOError
import com.github.deianvn.compose.director.core.error.GeneralError.ServerError
import com.github.deianvn.compose.director.core.error.GeneralError.UnknownError
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

    return stageError
}
