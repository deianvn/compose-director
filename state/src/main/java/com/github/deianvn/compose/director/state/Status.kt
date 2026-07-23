package com.github.deianvn.compose.director.state

import com.github.deianvn.compose.director.state.error.StateError


sealed class Status {
    object WORKING : Status()
    object IDLE : Status()
    class ERROR(val error: StateError) : Status()


    fun isWorking() = this is WORKING

    fun isIdle() = this is IDLE

    fun isError() = this is ERROR

    inline fun onWorking(block: () -> Unit): Status {
        if (this is WORKING) block()
        return this
    }

    inline fun onIdle(block: () -> Unit): Status {
        if (this is IDLE) block()
        return this
    }

    inline fun onError(block: (StateError) -> Unit): Status {
        if (this is ERROR) block(error)
        return this
    }
}
