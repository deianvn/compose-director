package com.github.deianvn.compose.director.android.viewmodel

import com.github.deianvn.compose.director.state.StateNode


abstract class SimpleStateViewModel<T : Any>(
    initialNode: StateNode<T, Unit, Unit, Unit>
) : StateViewModel<T, Unit, Unit, Unit>(initialNode)
