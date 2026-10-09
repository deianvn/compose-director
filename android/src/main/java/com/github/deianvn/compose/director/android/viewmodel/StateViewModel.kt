package com.github.deianvn.compose.director.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.deianvn.compose.director.android.viewmodel.job.JobLauncher
import com.github.deianvn.compose.director.android.viewmodel.job.LaunchStrategy
import com.github.deianvn.compose.director.state.StateNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * Base [ViewModel] that drives a navigable [StateNode] chain and exposes it as observable [state].
 *
 * Subclasses mutate the flow through [navigate]/[back] and run coroutines through [launch], which is
 * backed by a per-instance [JobLauncher] for keyed, strategy-based job management.
 *
 * @param T the step type. See [StateNode] for the meaning of the four type parameters.
 * @param U the shared-data type.
 * @param V the persistent-data type.
 * @param W the temporary-data type.
 */
abstract class StateViewModel<T : Any, U : Any, V : Any, W : Any>(
    initialNode: StateNode<T, U, V, W>
) : ViewModel() {

    private val _node = MutableStateFlow<StateNode<T, U, V, W>?>(initialNode)

    /** The current state as a read-only flow; `null` once the chain is terminated. */
    val state get() = _node.asStateFlow()

    private val jobLauncher = JobLauncher()

    init {
        logStateDebugInfo()
    }

    /**
     * Runs [block] as a keyed coroutine managed by the internal [JobLauncher].
     *
     * @param key groups related launches; [strategy] and cancellation operate per key.
     * @param scope the scope the job runs in, defaulting to [viewModelScope] so it is cancelled with
     *   the ViewModel.
     * @param strategy how this launch interacts with existing jobs for the same key.
     * @param ignoreCancellationException when `true`, a [kotlinx.coroutines.CancellationException]
     *   from [block] is logged and swallowed.
     * @param block the work to run.
     */
    fun launch(
        key: String,
        scope: CoroutineScope = viewModelScope,
        strategy: LaunchStrategy = LaunchStrategy.SEQUENTIAL,
        ignoreCancellationException: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch {
            jobLauncher.launch(key, scope, strategy, ignoreCancellationException, block)
        }
    }

    /** Cancels every job previously launched under [key]. */
    fun cancel(key: String) {
        viewModelScope.launch {
            jobLauncher.cancel(key)
        }
    }

    /** Cancels every job launched through [launch]. */
    fun cancelAll() {
        viewModelScope.launch {
            jobLauncher.cancelAll()
        }
    }

    /**
     * Returns the current step cast to [S].
     *
     * @throws IllegalStateException if there is no current step or it is not of type [S].
     */
    @Throws(IllegalStateException::class)
    inline fun <reified S> requireStep(): S {
        val step = state.value?.step
        if (step is S) {
            return step
        }
        val errorMessage =
            "Fatal state error: expected step of type ${S::class.java.name} but received ${step?.let { (it::class as Any).javaClass.name }}"
        Timber.e(errorMessage)

        throw IllegalStateException(errorMessage)
    }

    /** Applies [compute] to the current node and publishes the result; ignored if terminated. */
    fun navigate(compute: (StateNode<T, U, V, W>) -> StateNode<T, U, V, W>) {
        val currentNode = state.value
        if (currentNode == null) {
            Timber.w("${javaClass.simpleName}: navigate called on a terminated state, ignoring")
            return
        }
        publish(compute(currentNode))
    }

    /** Navigates to the previous node. Returns `true` if there was one, `false` otherwise. */
    fun back(): Boolean {
        val previous = state.value?.pop()
        publish(previous)
        return previous != null
    }

    /** Returns `true` if there is a previous node to navigate [back] to. */
    fun hasPrevious(): Boolean {
        return state.value?.hasPrevious() == true
    }

    private fun publish(newNode: StateNode<T, U, V, W>?) {
        _node.value = newNode
        newNode?.action?.invoke()
        logStateDebugInfo()
    }

    private fun logStateDebugInfo() {
        Timber.i("${javaClass.simpleName}: ${state.value?.getDebugInfo() ?: "[terminated]"}")
    }

}
