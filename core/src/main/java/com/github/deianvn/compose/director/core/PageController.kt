package com.github.deianvn.compose.director.core

import androidx.compose.runtime.Composable
import com.github.deianvn.compose.director.error.SceneError


interface PageController<S : Scene> {

    @Composable
    fun Renderer(status: Status, scene: S, fault: SceneError)
}
