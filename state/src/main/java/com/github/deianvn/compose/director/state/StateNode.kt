package com.github.deianvn.compose.director.state

import kotlin.reflect.KClass


class StateNode<T : Step, U : SideData, V : SideData, W : SideData> {

    val revision: Long
    val step: T
    val status: Status
    private var _sharedData: U
    val sharedData: U get() = head._sharedData
    val persistentData: V
    val temporaryData: W
    val remembered: Boolean
    val action: () -> Unit
    private val previousNode: StateNode<T, U, V, W>?
    val head: StateNode<T, U, V, W>


    companion object {

        fun <T : Step> head(
            step: T,
            status: Status,
            action: () -> Unit = {}
        ): StateNode<T, EmptySideData, EmptySideData, EmptySideData> = head(
            step = step,
            status = status,
            sharedData = EmptySideData,
            persistentData = EmptySideData,
            temporaryData = EmptySideData,
            action = action
        )

        fun <T : Step, U : SideData, V : SideData, W : SideData> head(
            step: T,
            status: Status,
            sharedData: U,
            persistentData: V,
            temporaryData: W,
            action: () -> Unit = {}
        ): StateNode<T, U, V, W> = StateNode(
            revision = 0L,
            step = step,
            status = status,
            sharedData = sharedData,
            persistentData = persistentData,
            temporaryData = temporaryData,
            remembered = false,
            action = action
        )

    }

    private constructor(
        revision: Long,
        step: T,
        status: Status,
        sharedData: U,
        persistentData: V,
        temporaryData: W,
        remembered: Boolean = true,
        headNode: StateNode<T, U, V, W>? = null,
        previousNode: StateNode<T, U, V, W>? = null,
        action: () -> Unit = {}
    ) {
        this.revision = revision
        this.step = step
        this.status = status
        this._sharedData = sharedData
        this.persistentData = persistentData
        this.temporaryData = temporaryData
        this.remembered = remembered
        this.action = action
        this.head = headNode ?: this
        this.previousNode = previousNode
    }

    val owner: StateNode<T, U, V, W>? get() = when {
        remembered -> this
        else -> previousNode
    }

    fun copy(
        step: T = this.step,
        status: Status = this.status,
        sharedData: U = this.sharedData,
        persistentData: V = this.persistentData,
        temporaryData: W = this.temporaryData,
        action: () -> Unit = this.action
    ): StateNode<T, U, V, W> = StateNode(
        revision = revision,
        step = step,
        status = status,
        sharedData = sharedData,
        persistentData = persistentData,
        temporaryData = temporaryData,
        remembered = remembered,
        headNode = head,
        previousNode = previousNode,
        action = action
    )

    fun chain(
        status: Status = Status.IDLE,
        step: T = this.step,
        sharedData: U = this.sharedData,
        persistentData: V = this.persistentData,
        temporaryData: W = head.temporaryData,
        remembered: Boolean = true,
        action: () -> Unit = {}
    ): StateNode<T, U, V, W> {
        head._sharedData = sharedData

        return StateNode(
            revision = revision + 1L,
            step = step,
            status = status,
            sharedData = sharedData,
            persistentData = persistentData,
            temporaryData = temporaryData,
            remembered = remembered,
            headNode = head,
            previousNode = if (this.remembered) this else previousNode,
            action = action,
        )
    }

    fun hasPrevious(): Boolean {
        return if (remembered) {
            previousNode != null
        } else {
            previousNode?.previousNode != null
        }
    }

    fun pop(): StateNode<T, U, V, W>? {
        return owner?.previousNode
    }

    fun seek(
        target: KClass<out T>? = null,
        targetSet: Set<KClass<out T>> = emptySet(),
        limit: Int? = null,
        inclusive: Boolean = false,
        currentIfMissing: Boolean = true
    ): StateNode<T, U, V, W> {
        val miss = if (currentIfMissing) this else head

        val targets = buildSet {
            target?.let { add(it) }
            addAll(targetSet)
        }

        if (targets.isEmpty()) {
            return miss
        }

        var current: StateNode<T, U, V, W>? = owner
        var remaining = limit

        while (current != null) {
            val node = current
            if (targets.any { it.isInstance(node.step) }) {
                return if (inclusive) node.pop() ?: head else node
            }
            if (remaining != null && --remaining <= 0) {
                return miss
            }
            current = node.pop()
        }

        return miss
    }

    fun getDebugInfo(): String {
        val currentNode: StateNode<*, *, *, *> = this
        val chain = mutableListOf<StateNode<*, *, *, *>>()
        var ptr: StateNode<*, *, *, *>? = currentNode
        while (ptr != null) {
            chain.add(ptr)
            ptr = ptr.previousNode
        }
        chain.reverse()

        val result = StringBuilder()
        result.append("[*")

        chain.forEach {
            if (it.remembered) {
                result.append("] --> [")
            } else {
                result.append(" -> ")
            }
            result.append(
                String.format(
                    "%s@%s",
                    it.step::class.simpleName,
                    when (it.status) {
                        is Status.ERROR -> it.status.error.javaClass.simpleName
                        else -> it.status::class.simpleName
                    }
                )
            )
        }

        result.append("]")

        return result.toString()
    }
}
