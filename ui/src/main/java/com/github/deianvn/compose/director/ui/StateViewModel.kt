package com.github.deianvn.compose.director.ui

import androidx.lifecycle.ViewModel
import com.github.deianvn.compose.director.state.SideData
import com.github.deianvn.compose.director.state.Step
import com.github.deianvn.compose.director.state.StateNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.reflect.KClass


abstract class StateViewModel<T : Step, U : SideData, V : SideData, W : SideData>(
    private val initialNode: StateNode<T, U, V, W>
) : ViewModel() {

    private val _node = MutableStateFlow<StateNode<T, U, V, W>>(initialNode)

    val stateQueue get() = _node.asStateFlow()

    init {
        logStateDebugInfo()
    }

    @Throws(IllegalStateException::class)
    inline fun <reified T> requireStep(): T {
        val step = stateQueue.value.step
        if (step is T) {
            return step
        }
        val errorMessage =
            "Fatal state error: expected step of type ${T::class.javaClass.name} but received ${(step::class as Any).javaClass.name}"
        Timber.e(errorMessage)

        throw IllegalStateException(errorMessage)
    }

    fun publish(
        replace: Boolean = false,
        pop: Boolean = false,
        target: KClass<out T>? = null,
        targetSet: Set<KClass<out T>>? = null,
        inclusive: Boolean = false,
        computeNode: (node: StateNode<T, U>) -> StateNode<T, U>
    ) {
        val combinedTargetSet: Set<KClass<out T>> = buildSet {
            target?.let { add(it) }
            targetSet?.let { addAll(it) }
        }

        val newAct = computeNode(
            when {
                replace -> getPreviousActByReplace(
                    targetSteps = combinedTargetSet
                )

                pop -> getPreviousActByPop(
                    targetSteps = combinedTargetSet, inclusive = inclusive
                )

                else -> stateQueue.value
            }
        )

        _Node.value = newAct
        newAct.action()
        logStateDebugInfo()
    }

    fun pop(
        target: KClass<out T>? = null,
        targetSet: Set<KClass<out T>>? = null,
        inclusive: Boolean = false
    ) {
        val combinedTargetSet: Set<KClass<out T>> = buildSet {
            target?.let { add(it) }
            targetSet?.let { addAll(it) }
        }

        val newAct = getPreviousActByPop(
            targetSteps = combinedTargetSet,
            inclusive = inclusive
        )

        _Node.value = newAct
        newAct.action()

        logStateDebugInfo()
    }

    fun hasPrevious(): Boolean {
        return stateQueue.value.hasPrevious()
    }

    private fun getPreviousActByReplace(
        targetSteps: Set<KClass<out T>>
    ): StateNode<T, U> {

        val parentState = stateQueue.value.currentParentNode()

        val state = when {
            targetSteps.isEmpty() -> stateQueue.value
            targetSteps.any { it.isInstance(parentState?.step) } -> parentState?.pop()
            else -> stateQueue.value
        }

        return state ?: initialNode.copy(isFinal = true)
    }

    private fun getPreviousActByPop(
        targetSteps: Set<KClass<out T>>,
        inclusive: Boolean = false
    ): StateNode<T, U> {
        val result = if (targetSteps.isEmpty()) {
            stateQueue.value.pop()
        } else {
            var current = stateQueue.value.pop()
            while (current != null && targetSteps.none { it.isInstance(current.step) }) {
                current = current.pop()
            }

            when {
                current == null -> stateQueue.value
                inclusive -> current.pop()
                else -> current
            }
        }

        return result ?: initialNode.copy(isFinal = true)
    }

    private fun logStateDebugInfo() {
        Timber.Forest.i("${javaClass.simpleName}: ${stateQueue.value.getDebugInfo()}")
    }

}