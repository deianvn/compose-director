package com.github.deianvn.compose.director.core

import com.github.deianvn.compose.director.core.error.StageError


data class Stage<T : Scene, U : Plot, V : Decor>(
    val revision: Long = 0L,
    val scene: T,
    val status: Status,
    val plot: U,
    val decor: V,
    val fault: StageError? = null,
    val isSequence: Boolean = true,
    val isFinal: Boolean = false,
    val action: () -> Unit = {}
) {

    private var previousScene: Stage<T, U, V>? = null

    fun next(
        status: Status = this.status,
        scene: T = this.scene,
        plot: U = this.plot,
        decor: V = this.decor,
        fault: StageError? = null,
        isSequence: Boolean = true,
        action: () -> Unit = {}
    ) = Stage(
        revision = revision + 1L,
        scene = scene,
        status = status,
        plot = plot,
        decor = decor,
        fault = fault,
        action = action,
        isSequence = true,
    ).also {
        it.previousScene = if (isSequence) {
            this
        } else {
            previousScene
        }
    }

    fun hasPrevious(): Boolean {
        return if (isSequence) {
            previousScene != null
        } else {
            previousScene?.previousScene != null
        }
    }

    fun currentSequence(): Stage<T, U, V>? {
        return when {
            isSequence -> this
            else -> previousScene
        }
    }

    fun pop(): Stage<T, U, V>? {
        return currentSequence()?.previousScene
    }

    fun getDebugInfo(): String {
        val currentStage: Stage<*, *, *>? = this
        val chain = mutableListOf<Stage<*, *, *>>()
        var ptr = currentStage
        while (ptr != null) {
            chain.add(ptr)
            ptr = ptr.previousScene
        }
        chain.reverse()

        val result = StringBuilder()
        result.append("[*")

        chain.forEach {
            if (it.isSequence) {
                result.append("] --> [")
            } else {
                result.append(" -> ")
            }
            result.append(
                String.format(
                    "%s@%s",
                    it.scene::class.simpleName,
                    it.fault?.javaClass?.simpleName ?: it.status
                )
            )
        }

        result.append("]")

        return result.toString()
    }
}
