package com.github.deianvn.compose.director.viewmodel

import androidx.lifecycle.ViewModel
import com.github.deianvn.compose.director.core.Stage
import com.github.deianvn.compose.director.core.Decor
import com.github.deianvn.compose.director.core.Scene
import com.github.deianvn.compose.director.core.Plot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.collections.any
import kotlin.collections.none
import kotlin.jvm.Throws
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.reflect.KClass


abstract class StageViewModel<T : Scene, U : Plot, V: Decor>(
    private val initialStage: Stage<T, U, V>
) : ViewModel() {

    private val _stage = MutableStateFlow<Stage<T, U, V>>(initialStage)

    val act get() = _stage.asStateFlow()

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
        computeState: (state: Stage<T, U, V>) -> Stage<T, U, V>
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

        _stage.value = newAct
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

        _stage.value = newAct
        newAct.action()

        logStateDebugInfo()
    }

    fun hasPrevious(): Boolean {
        return act.value.hasPrevious()
    }

    private fun getPreviousActByReplace(
        targetSteps: Set<KClass<out T>>
    ): Stage<T, U, V> {

        val parentState = act.value.currentSequence()

        val state = when {
            targetSteps.isEmpty() -> act.value
            targetSteps.any { it.isInstance(parentState?.scene) } -> parentState?.pop()
            else -> act.value
        }

        return state ?: initialStage.copy(isFinal = true)
    }

    private fun getPreviousActByPop(
        targetSteps: Set<KClass<out T>>,
        inclusive: Boolean = false
    ): Stage<T, U, V> {
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

        return result ?: initialStage.copy(isFinal = true)
    }

    private fun logStateDebugInfo() {
        Timber.i("${javaClass.simpleName}: ${act.value.getDebugInfo()}")
    }

}
