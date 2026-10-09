package com.github.deianvn.compose.director.android.viewmodel.job

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber


/**
 * Coordinates keyed coroutine jobs, enforcing a [LaunchStrategy] per key.
 *
 * All jobs for a given key are tracked together in a list, so [cancel] and [cancelAll] can stop every
 * coroutine associated with that key. Access to the internal map is guarded by a [Mutex]; completed
 * jobs are pruned lazily on the next launch for the same key rather than removing themselves on
 * completion (the completion callback cannot acquire the suspend [Mutex]).
 */
class JobLauncher {
    private val jobs = mutableMapOf<String, MutableList<Job>>()
    private val mutex = Mutex()

    /**
     * Launches [block] under [key] using the given [strategy].
     *
     * @param key groups related jobs; strategies and cancellation operate per key.
     * @param scope the [CoroutineScope] the job runs in.
     * @param strategy how this launch interacts with existing jobs for the same key.
     * @param ignoreCancellationException when `true`, a [CancellationException] thrown by [block] is
     *   logged and swallowed instead of propagated.
     * @param block the work to run.
     */
    suspend fun launch(
        key: String,
        scope: CoroutineScope,
        strategy: LaunchStrategy = LaunchStrategy.SEQUENTIAL,
        ignoreCancellationException: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        when (strategy) {
            LaunchStrategy.LATEST -> launchLatest(key, scope, ignoreCancellationException, block)
            LaunchStrategy.IF_IDLE -> launchIfIdle(key, scope, ignoreCancellationException, block)
            LaunchStrategy.SEQUENTIAL -> launchSequential(key, scope, ignoreCancellationException, block)
            LaunchStrategy.PARALLEL -> launchParallel(key, scope, ignoreCancellationException, block)
        }
    }

    /** Cancels every job registered under [key] and forgets the key. */
    suspend fun cancel(key: String) {
        mutex.withLock {
            jobs[key]?.forEach { it.cancel() }
            jobs.remove(key)
        }
    }

    /** Cancels every tracked job across all keys. */
    suspend fun cancelAll() {
        mutex.withLock {
            jobs.values.forEach { list -> list.forEach { it.cancel() } }
            jobs.clear()
        }
    }

    /** Cancels any running jobs for [key] and starts a single fresh one. */
    private suspend fun launchLatest(
        key: String,
        scope: CoroutineScope,
        ignoreCancellationException: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        mutex.withLock {
            jobs[key]?.forEach { it.cancel() }
            jobs[key] = mutableListOf(scope.trackedLaunch(key, ignoreCancellationException, block = block))
        }
    }

    /** Starts a new job only if no job for [key] is currently active; otherwise does nothing. */
    private suspend fun launchIfIdle(
        key: String,
        scope: CoroutineScope,
        ignoreCancellationException: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        mutex.withLock {
            if (jobs[key]?.any { it.isActive } == true) return
            jobs[key] = mutableListOf(scope.trackedLaunch(key, ignoreCancellationException, block = block))
        }
    }

    /** Queues the new job to run after the most recent job for [key] completes. */
    private suspend fun launchSequential(
        key: String,
        scope: CoroutineScope,
        ignoreCancellationException: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        mutex.withLock {
            val previousJob = jobs[key]?.lastOrNull()
            val newJob = scope.trackedLaunch(key, ignoreCancellationException, joinFirst = previousJob, block = block)
            jobs.getOrPut(key) { mutableListOf() }.apply {
                pruneCompleted()
                add(newJob)
            }
        }
    }

    /** Starts a new job for [key] concurrently, leaving existing jobs running. */
    private suspend fun launchParallel(
        key: String,
        scope: CoroutineScope,
        ignoreCancellationException: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        mutex.withLock {
            jobs.getOrPut(key) { mutableListOf() }.apply {
                pruneCompleted()
                add(scope.trackedLaunch(key, ignoreCancellationException, block = block))
            }
        }
    }

    /**
     * Launches [block] in this scope, optionally awaiting [joinFirst] first, and applies the shared
     * [CancellationException] handling.
     */
    private fun CoroutineScope.trackedLaunch(
        key: String,
        ignoreCancellationException: Boolean,
        joinFirst: Job? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job = launch {
        joinFirst?.join()
        try {
            block()
        } catch (e: CancellationException) {
            if (ignoreCancellationException) {
                Timber.w(e, "Job was cancelled, will ignore exception: $key")
            } else {
                throw e
            }
        }
    }

    /** Drops jobs that are no longer active, keeping each key's list from growing unbounded. */
    private fun MutableList<Job>.pruneCompleted() {
        removeAll { !it.isActive }
    }
}