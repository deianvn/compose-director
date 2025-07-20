package com.github.deianvn.compose.director.ui

import androidx.compose.runtime.Composable
import com.github.deianvn.compose.director.core.Scene
import com.github.deianvn.compose.director.core.Status
import com.github.deianvn.compose.director.core.error.StageError


interface PageController<S : Scene> {

    @Composable
    fun Renderer(status: Status, scene: S, fault: StageError?)
}