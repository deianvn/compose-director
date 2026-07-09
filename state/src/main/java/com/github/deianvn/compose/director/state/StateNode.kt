package com.github.deianvn.compose.director.state


class StateNode<T : Step, U : SideData, V : SideData, W : SideData> {

    val revision: Long
    val step: T
    val status: Status
    private var _sharedData: U
    val sharedData: U get() = _head._sharedData
    val persistentData: V
    val temporaryData: W
    val remembered: Boolean
    val action: () -> Unit
    private val previousNode: StateNode<T, U, V, W>?
    private val _head: StateNode<T, U, V, W>


    companion object {

        fun <T : Step, U : SideData, V : SideData, W : SideData> init(
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
        this._head = headNode ?: this
        this.previousNode = previousNode
    }

    fun owner(): StateNode<T, U, V, W>? = when {
        remembered -> this
        else -> previousNode
    }

    fun head() = _head

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
        headNode = _head,
        previousNode = previousNode,
        action = action
    )

    fun chain(
        status: Status = Status.IDLE,
        step: T = this.step,
        sharedData: U = this.sharedData,
        persistentData: V = this.persistentData,
        temporaryData: W = _head.temporaryData,
        remembered: Boolean = true,
        action: () -> Unit = {}
    ): StateNode<T, U, V, W> {
        _head._sharedData = sharedData

        return StateNode(
            revision = revision + 1L,
            step = step,
            status = status,
            sharedData = sharedData,
            persistentData = persistentData,
            temporaryData = temporaryData,
            remembered = remembered,
            headNode = _head,
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
        return owner()?.previousNode
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
