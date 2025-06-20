package com.github.deianvn.compose.director.viewmodel

import androidx.lifecycle.ViewModel
import com.github.deianvn.compose.director.core.Act
import com.github.deianvn.compose.director.core.Decor
import com.github.deianvn.compose.director.core.Scene
import com.github.deianvn.compose.director.core.Plot
import com.github.deianvn.compose.director.core.Status
import com.github.deianvn.compose.director.error.SceneFault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.collections.any
import kotlin.collections.none
import kotlin.jvm.Throws
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.reflect.KClass


abstract class ActViewModel<T : Scene, U : Plot, V: Decor>(
    private val initialAct: Act<T, U, V>
) : ViewModel() {

    private val _act = MutableStateFlow<Act<T, U, V>>(initialAct)

    val act get() = _act.asStateFlow()

    init {
        logStateDebugInfo()
    }

    @Throws(IllegalStateException::class)
    inline fun <reified T> requireScene(): T {
        val scene = act.value.scene
        if (scene is T) {
            return scene
        }
        val errorMessage =
            "Fatal state error: expected scene of type ${T::class.javaClass.name} but received ${(scene::class as Any).javaClass.name}"
        Timber.e(errorMessage)

        throw IllegalStateException(errorMessage)
    }

    fun publish(
        replace: Boolean = false,
        pop: Boolean = false,
        target: KClass<out T>? = null,
        targetSet: Set<KClass<out T>>? = null,
        inclusive: Boolean = false,
        computeState: (state: Act<T, U, V>) -> Act<T, U, V>
    ) {
        val combinedTargetSet: Set<KClass<out T>> = buildSet {
            target?.let { add(it) }
            targetSet?.let { addAll(it) }
        }

        val newAct = computeState(
            when {
                replace -> getPreviousActByReplace(
                    targetSteps = combinedTargetSet
                )

                pop -> getPreviousActByPop(
                    targetSteps = combinedTargetSet, inclusive = inclusive
                )

                else -> act.value
            }
        )

        _act.value = newAct
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

        _act.value = newAct
        newAct.action()

        logStateDebugInfo()
    }

    fun publishError(
        fault: SceneFault,
        isSequence: Boolean = false
    ) {
        publish {
            it.next(status = Status.IDLE, fault = fault, isSequence = isSequence)
        }
    }

    fun publishLoading(
        isSequence: Boolean = false
    ) {
        publish {
            it.next(status = Status.WORKING, isSequence = isSequence)
        }
    }

    fun publishSuccess(
        isSequence: Boolean = false
    ) {
        publish {
            it.next(status = Status.IDLE, isSequence = isSequence)
        }
    }

    fun hasPrevious(): Boolean {
        return act.value.hasPrevious()
    }

    private fun getPreviousActByReplace(
        targetSteps: Set<KClass<out T>>
    ): Act<T, U, V> {

        val parentState = act.value.currentSequence()

        val state = when {
            targetSteps.isEmpty() -> act.value
            targetSteps.any { it.isInstance(parentState?.scene) } -> parentState?.pop()
            else -> act.value
        }

        return state ?: initialAct.copy(isFinal = true)
    }

    private fun getPreviousActByPop(
        targetSteps: Set<KClass<out T>>,
        inclusive: Boolean = false
    ): Act<T, U, V> {
        val result = if (targetSteps.isEmpty()) {
            act.value.pop()
        } else {
            var current = act.value.pop()
            while (current != null && targetSteps.none { it.isInstance(current.scene) }) {
                current = current.pop()
            }

            when {
                current == null -> act.value
                inclusive -> current.pop()
                else -> current
            }
        }

        return result ?: initialAct.copy(isFinal = true)
    }

    private fun logStateDebugInfo() {
        Timber.i("${javaClass.simpleName}: ${act.value.getDebugInfo()}")
    }

}
