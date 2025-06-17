package com.github.deianvn.compose.director.core

import androidx.compose.runtime.Composable


interface PageController<S : Scene> {
    @Composable
    fun RenderPage(scene: S)
}
