package com.github.deianvn.compose.director.android.state

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.github.deianvn.compose.director.android.viewmodel.StateViewModel
import com.github.deianvn.compose.director.state.SideData
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Step


@Composable
fun <T : Step, U : SideData, V : SideData, W : SideData> StateNode<T, U, V, W>?.finishOnNull(
    composable: @Composable (StateNode<T, U, V, W>) -> Unit
) {
    val activity = LocalActivity.current

    if (this == null) {
        LaunchedEffect(Unit) {
            activity?.finish()
        }
    } else {
        composable(this)
    }
}

@Composable
fun StateBackHandler(viewModel: StateViewModel<*, *, *, *>, enabled: Boolean = true) {
    BackHandler(enabled = enabled) { viewModel.back() }
}

