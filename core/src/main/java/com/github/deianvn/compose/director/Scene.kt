package com.github.deianvn.compose.director

import com.github.deianvn.compose.director.error.SceneFault


data class Scene<T, U : Plot>(
    val revision: Long = 0L,
    val act: T,
    val status: Status,
    val plot: U,
    val fault: SceneFault? = null,
    val isChild: Boolean = false,
    val action: () -> Unit = {}
) {

    private var previousScene: Scene<T, U>? = null

    fun next(
        status: Status = this.status,
        act: T = this.act,
        plot: U = this.plot,
        fault: SceneFault? = null,
        action: () -> Unit = {}
    ) = Scene(
        revision = revision + 1L,
        act = act,
        status = status,
        plot = plot,
        fault = fault,
        action = action,
        isChild = false,
    ).also {
        it.previousScene = if (!isChild) {
            this
        } else {
            previousScene
        }
    }

    fun nextChild(
        status: Status = this.status,
        act: T = this.act,
        plot: U = this.plot,
        error: SceneFault? = null,
        action: () -> Unit = {}
    ) = Scene(
        revision = revision + 1L,
        act = act,
        status = status,
        plot = plot,
        fault = fault,
        isChild = true,
        action = action
    ).also {
        it.previousScene = if (!isChild) {
            this
        } else {
            previousScene
        }
    }

    fun hasPrevious(): Boolean {
        return if (!isChild) {
            previousScene != null
        } else {
            previousScene?.previousScene != null
        }
    }

    fun parentState(): Scene<T, U>? {
        return when {
            !isChild -> this
            else -> previousScene
        }
    }

    fun pop(): Scene<T, U>? {
        return parentState()?.previousScene
    }

    fun getDebugInfo(): String {
        val currentScene: Scene<*, *>? = this
        val chain = mutableListOf<Scene<*, *>>()
        var ptr = currentScene
        while (ptr != null) {
            chain.add(ptr)
            ptr = ptr.previousScene
        }
        chain.reverse()

        val result = StringBuilder()
        result.append("[*")

        chain.forEach {
            if (!it.isChild) {
                result.append("] --> [")
            } else {
                result.append(" -> ")
            }
            result.append(
                String.format(
                    "%s@%s",
                    it.act::class.simpleName,
                    it.fault?.javaClass?.simpleName ?: it.status
                )
            )
        }

        result.append("]")

        return result.toString()
    }
}
