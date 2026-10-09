package com.github.deianvn.compose.director.android.viewmodel

import com.github.deianvn.compose.director.state.StateNode


/**
 * A [StateViewModel] specialization for flows that only care about the step type and need no side
 * data. The shared, persistent and temporary slots are fixed to [Unit].
 *
 * @param T the step type.
 */
abstract class SimpleStateViewModel<T : Any>(
    initialNode: StateNode<T, Unit, Unit, Unit>
) : StateViewModel<T, Unit, Unit, Unit>(initialNode)
