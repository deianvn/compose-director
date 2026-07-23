package com.github.deianvn.compose.director.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.deianvn.compose.director.android.viewmodel.StateViewModel


@Composable
fun StateBackHandler(
    viewModel: StateViewModel<*, *, *, *>,
    onExitComposable: (@Composable () -> Unit)? = null
) {
    var showOnExitComposable by rememberSaveable { mutableStateOf(false) }

    if (showOnExitComposable) {
        onExitComposable?.invoke()
    }

    BackHandler {
        if (viewModel.hasPrevious()) {
            viewModel.back()
        } else if (onExitComposable != null) {
            showOnExitComposable = true
        } else {
            viewModel.back()
        }
    }
}
