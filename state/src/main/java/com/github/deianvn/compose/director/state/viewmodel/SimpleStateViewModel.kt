package com.github.deianvn.compose.director.state.viewmodel

import com.github.deianvn.compose.director.state.EmptySideData
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Step


abstract class SimpleStateViewModel<T : Step>(
    initialNode: StateNode<T, EmptySideData, EmptySideData, EmptySideData>
) : StateViewModel<T, EmptySideData, EmptySideData, EmptySideData>(initialNode)
