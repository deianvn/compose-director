package com.github.deianvn.compose.director.state.viewmodel

import androidx.lifecycle.ViewModel
import com.github.deianvn.compose.director.state.SideData
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber


abstract class StateViewModel<T : Step, U : SideData, V : SideData, W : SideData>(
    initialNode: StateNode<T, U, V, W>
) : ViewModel() {

    private val _node = MutableStateFlow<StateNode<T, U, V, W>?>(initialNode)

    val node get() = _node.asStateFlow()

    init {
        logStateDebugInfo()
    }

    @Throws(IllegalStateException::class)
    inline fun <reified S> requireStep(): S {
        val step = node.value?.step
        if (step is S) {
            return step
        }
        val errorMessage =
            "Fatal state error: expected step of type ${S::class.java.name} but received ${step?.let { (it::class as Any).javaClass.name }}"
        Timber.e(errorMessage)

        throw IllegalStateException(errorMessage)
    }

    fun navigate(compute: (StateNode<T, U, V, W>) -> StateNode<T, U, V, W>) {
        val currentNode = node.value
        if (currentNode == null) {
            Timber.w("${javaClass.simpleName}: navigate called on a terminated state, ignoring")
            return
        }
        publish(compute(currentNode))
    }

    fun back(): Boolean {
        val previous = node.value?.pop()
        publish(previous)
        return previous != null
    }

    fun hasPrevious(): Boolean {
        return node.value?.hasPrevious() == true
    }

    private fun publish(newNode: StateNode<T, U, V, W>?) {
        _node.value = newNode
        newNode?.action?.invoke()
        logStateDebugInfo()
    }

    private fun logStateDebugInfo() {
        Timber.i("${javaClass.simpleName}: ${node.value?.getDebugInfo() ?: "[terminated]"}")
    }

}
