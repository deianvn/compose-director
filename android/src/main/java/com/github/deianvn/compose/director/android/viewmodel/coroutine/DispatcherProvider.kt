package com.github.deianvn.compose.director.android.viewmodel.coroutine

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Abstraction over the coroutine dispatchers used by the library, allowing them to be swapped
 * (e.g. with test dispatchers) instead of referencing [kotlinx.coroutines.Dispatchers] directly.
 */
interface DispatcherProvider {
    /** Dispatcher for blocking IO work (network, disk). */
    val io: CoroutineDispatcher

    /** Main/UI-thread dispatcher. */
    val ui: CoroutineDispatcher

    /** Dispatcher for CPU-bound work. */
    val default: CoroutineDispatcher
}
