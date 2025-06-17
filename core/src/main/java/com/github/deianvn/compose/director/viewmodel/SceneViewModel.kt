package com.github.deianvn.compose.director.viewmodel

import androidx.lifecycle.ViewModel
import com.github.deianvn.compose.director.core.Scene
import com.github.deianvn.compose.director.core.Plot
import com.github.deianvn.compose.director.core.StageAct
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


abstract class SceneViewModel<T : Scene, U : Plot>(
    private val initialStageAct: StageAct<T, U>
) : ViewModel() {

    private val _stageAct = MutableStateFlow<StageAct<T, U>>(initialStageAct)

    val scene get() = _stageAct.asStateFlow()

    init {
        logStateDebugInfo()
    }

    /**
     * Returns the current step cast to the expected type [T], or throws if the type does not match.
     *
     * This is useful when a specific step type is required at a certain point in the flow.
     * If the current step is not of type [T], an [IllegalStateException] is thrown with a detailed
     * error message.
     *
     * @return The current step cast to type [T].
     * @throws IllegalStateException if the current step is not an instance of [T].
     */
    @Throws(IllegalStateException::class)
    inline fun <reified T> requireAct(): T {
        val act = scene.value.scene
        if (act is T) {
            return act
        }
        val errorMessage =
            "Fatal state error: expected step of type ${T::class.javaClass.name} but received ${(act::class as Any).javaClass.name}"
        Timber.e(errorMessage)

        throw IllegalStateException(errorMessage)
    }

    /**
     * Publishes a new state by optionally modifying the navigation stack and applying a transformation.
     *
     * Depending on the flags, the stack may be popped or a parent state may be replaced before applying [computeState].
     * The [computeState] function receives the resolved base state and should return a modified copy.
     *
     * @param replace If true, replaces the parent state if its step matches a target.
     * @param pop If true, pops the stack until a target step is found.
     * @param target A single step class to match (converted to a set).
     * @param targetSet A set of step classes to match.
     * @param inclusive Whether to remove the matched step itself when popping.
     * @param computeState Function that takes the resolved base state and returns the new state.
     */
    fun publish(
        replace: Boolean = false,
        pop: Boolean = false,
        target: KClass<out T>? = null,
        targetSet: Set<KClass<out T>>? = null,
        inclusive: Boolean = false,
        computeState: (state: StageAct<T, U>) -> StageAct<T, U>
    ) {
        val combinedTargetSet: Set<KClass<out T>> = buildSet {
            target?.let { add(it) }
            targetSet?.let { addAll(it) }
        }

        val newState = computeState(
            when {
                replace -> getPreviousSceneByReplace(
                    targetSteps = combinedTargetSet
                )

                pop -> getPreviousSceneByPop(
                    targetSteps = combinedTargetSet, inclusive = inclusive
                )

                else -> scene.value
            }
        )

        _stageAct.value = newState
        newState.action()
        logStateDebugInfo()
    }

    /**
     * Pops the navigation stack to a previous state that matches one of the specified target steps.
     *
     * Combines [target] and [targetSet] into a single set of steps to match against.
     * If a matching step is found, the state is popped back to that step (optionally skipping it if [inclusive] is true),
     * and the new state's action is triggered.
     *
     * @param target A single step class to pop to.
     * @param targetSet A set of step classes to pop to.
     * @param inclusive If true, also removes the matched target step from the stack.
     */
    fun pop(
        target: KClass<out T>? = null,
        targetSet: Set<KClass<out T>>? = null,
        inclusive: Boolean = false
    ) {
        val combinedTargetSet: Set<KClass<out T>> = buildSet {
            target?.let { add(it) }
            targetSet?.let { addAll(it) }
        }

        val newScene = getPreviousSceneByPop(
            targetSteps = combinedTargetSet,
            inclusive = inclusive
        )

        _stageAct.value = newScene
        newScene.action()

        logStateDebugInfo()
    }

    /**
     * Publishes an error state with the given [fault], marking the status as [Status.IDLE].
     *
     * The error can be applied to a new child state or to the main (parent) state,
     * depending on [isChildState].
     *
     * @param fault The [SceneFault] to attach to the new state.
     * @param isChildState If true, the error is published to a child state; otherwise,
     * to a new parent state.
     */
    fun publishError(
        fault: SceneFault,
        isChildState: Boolean = true
    ) {
        publish {
            if (isChildState) {
                it.nextChild(status = Status.IDLE, fault = fault)
            } else {
                it.next(status = Status.IDLE, fault = fault)
            }
        }
    }

    /**
     * Publishes a loading state with status set to [Status.WORKING].
     *
     * The loading state can be published as a child or parent state based on [isChildState].
     *
     * @param isChildState If true, publishes the loading state as a child; otherwise,
     * as a parent state.
     */
    fun publishLoading(
        isChildState: Boolean = true
    ) {
        publish {
            if (isChildState) {
                it.nextChild(status = Status.WORKING)
            } else {
                it.next(status = Status.WORKING)
            }
        }
    }

    /**
     * Publishes a success state with status set to [Status.SUCCESS].
     *
     * The success state can be published as a child or parent state depending on [isChildState].
     *
     * @param isChildState If true, publishes the success state as a child; otherwise,
     * as a parent state.
     */
    fun publishSuccess(
        isChildState: Boolean = true
    ) {
        publish {
            if (isChildState) {
                it.nextChild(status = Status.IDLE)
            } else {
                it.next(status = Status.IDLE)
            }
        }
    }

    /**
     * Checks whether there is a previous state available in the navigation stack.
     *
     * @return `true` if a previous state exists, `false` otherwise.
     */
    fun hasPrevious(): Boolean {
        return scene.value.hasPrevious()
    }

    /**
     * Replaces the current state with the parent if its step matches one of the targetSteps.
     *
     * If no match is found, returns the current state or marks it as finished.
     */
    private fun getPreviousSceneByReplace(
        targetSteps: Set<KClass<out T>>
    ): StageAct<T, U> {

        val parentState = scene.value.parentState()

        val state = when {
            targetSteps.isEmpty() -> scene.value
            targetSteps.any { it.isInstance(parentState?.scene) } -> parentState?.pop()
            else -> scene.value
        }

        return state ?: initialStageAct.copy(isFinishState = true)
    }

    /**
     * Pops the navigation stack (State<T, U>) until it finds a step matching one
     * of the targetSteps.
     *
     * If a match is found and inclusive is true, that step is also skipped.
     * If no target is found, returns the current state or marks it as finished.
     */
    private fun getPreviousSceneByPop(
        targetSteps: Set<KClass<out T>>,
        inclusive: Boolean = false
    ): StageAct<T, U> {
        val result = if (targetSteps.isEmpty()) {
            scene.value.pop()
        } else {
            var current = scene.value.pop()
            while (current != null && targetSteps.none { it.isInstance(current.scene) }) {
                current = current.pop()
            }

            when {
                current == null -> scene.value
                inclusive -> current.pop()
                else -> current
            }
        }

        return result ?: initialStageAct.copy(isFinishState = true)
    }

    private fun logStateDebugInfo() {
        Timber.i("${javaClass.simpleName}: ${scene.value.getDebugInfo()}")
    }

}
