package com.github.deianvn.compose.director.android.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.github.deianvn.compose.director.state.StateNode


@Composable
fun <T : Any, U : Any, V : Any, W : Any> FinishOnNullState(
    node: StateNode<T, U, V, W>?,
    content: @Composable (StateNode<T, U, V, W>) -> Unit
) {
    val activity = LocalActivity.current

    if (node == null) {
        LaunchedEffect(Unit) {
            activity?.finish()
        }
    } else {
        content(node)
    }
}
