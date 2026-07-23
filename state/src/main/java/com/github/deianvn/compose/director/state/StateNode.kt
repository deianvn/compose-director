package com.github.deianvn.compose.director.state

import kotlin.reflect.KClass


/**
 * An immutable node in a navigable state chain.
 *
 * Each node captures a single point in the flow (a [step] plus its [status]) and links back to the
 * previous node, forming a history that can be navigated forward via [chain] and backward via [pop].
 *
 * The four type parameters are all unconstrained (any non-null class may be used); use [Unit] for any
 * slot you don't need:
 *
 * @param T the step type — describes *where* the flow currently is (e.g. a `sealed class` of screens).
 * @param U the shared-data type — mutable data shared across the whole chain, always read from and
 *   written to the [head] node so every node observes the same value.
 * @param V the persistent-data type — data attached to this node that survives being carried forward
 *   by [chain] (defaults to the previous node's value).
 * @param W the temporary-data type — data scoped to this node only; [chain] resets it from [head]
 *   rather than propagating the current node's value.
 */
class StateNode<T : Any, U : Any, V : Any, W : Any> {

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

        /**
         * Creates a root node with no side data ([Unit] for the shared, persistent and temporary
         * slots).
         *
         * @param step the initial step.
         * @param status the initial [Status].
         * @param action side effect invoked when this node becomes current.
         */
        fun <T : Any> head(
            step: T,
            status: Status,
            action: () -> Unit = {}
        ): StateNode<T, Unit, Unit, Unit> = head(
            step = step,
            status = status,
            sharedData = Unit,
            persistentData = Unit,
            temporaryData = Unit,
            action = action
        )

        /**
         * Creates a root node carrying the given side data.
         *
         * @param step the initial step.
         * @param status the initial [Status].
         * @param sharedData initial shared data (see [StateNode]'s `U` parameter).
         * @param persistentData initial persistent data (see `V`).
         * @param temporaryData initial temporary data (see `W`).
         * @param action side effect invoked when this node becomes current.
         */
        fun <T : Any, U : Any, V : Any, W : Any> head(
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

    /**
     * Returns a copy of this node with the given fields overridden, keeping the same [revision],
     * [head] and [previousNode] (i.e. an in-place update rather than a new step in the chain).
     */
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

    /**
     * Advances the chain by producing the next node (incrementing [revision]).
     *
     * [sharedData] is written back to [head] so it stays visible to the whole chain. When
     * [remembered] is `true` this node becomes the new node's predecessor; when `false` the current
     * node is skipped, so it won't appear in back-navigation.
     */
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

    /**
     * Walks backward through the chain looking for a node whose step matches [target] or any entry
     * in [targetSet].
     *
     * @param target a single step class to match (matched via [KClass.isInstance]).
     * @param targetSet additional step classes to match.
     * @param limit maximum number of nodes to inspect before giving up; `null` means no limit.
     * @param inclusive when `true`, returns the node *before* the match instead of the match itself.
     * @param currentIfMissing when `true`, returns this node if no match is found; otherwise returns
     *   [head].
     */
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
