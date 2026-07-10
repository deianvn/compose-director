package com.github.deianvn.compose.director.state

import com.github.deianvn.compose.director.state.error.StateError


sealed class Status {
    object WORKING : Status()
    object IDLE : Status()
    class ERROR(val error: StateError) : Status()


    fun isWorking() = this is WORKING

    fun isIdle() = this is IDLE

    fun isError() = this is ERROR
}
