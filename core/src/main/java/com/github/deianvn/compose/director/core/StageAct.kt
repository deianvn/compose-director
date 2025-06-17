package com.github.deianvn.compose.director.core

import com.github.deianvn.compose.director.error.SceneFault


data class StageAct<T : Scene, U : Plot>(
    val revision: Long = 0L,
    val scene: T,
    val status: Status,
    val plot: U,
    val fault: SceneFault? = null,
    val isSequence: Boolean = true,
    val decors: Set<Decor> = emptySet(),
    val action: () -> Unit = {}
): Act {

    private var previousScene: StageAct<T, U>? = null

    fun next(
        status: Status = this.status,
        scene: T = this.scene,
        plot: U = this.plot,
        fault: SceneFault? = null,
        isSequence: Boolean = true,
        action: () -> Unit = {}
    ) = StageAct(
        revision = revision + 1L,
        scene = scene,
        status = status,
        plot = plot,
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

    fun currentSequence(): StageAct<T, U>? {
        return when {
            isSequence -> this
            else -> previousScene
        }
    }

    fun pop(): StageAct<T, U>? {
        return currentSequence()?.previousScene
    }

    inline fun <reified V : Decor> getDecor(): V? {
        return decors.firstOrNull { it is V } as? V
    }

    fun getDebugInfo(): String {
        val currentStageAct: StageAct<*, *>? = this
        val chain = mutableListOf<StageAct<*, *>>()
        var ptr = currentStageAct
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
