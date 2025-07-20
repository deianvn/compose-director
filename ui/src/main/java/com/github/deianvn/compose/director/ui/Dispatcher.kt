package com.github.deianvn.compose.director.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.deianvn.compose.director.core.PageControllerRegistry


@Composable
fun Dispatcher(
    viewModel: StageViewModel<*, *, *>,
    registry: PageControllerRegistry
) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val scene = stage.scene
    val controller = registry.getController(scene)

    controller?.Renderer(
        status = stage.status,
        scene = scene,
        fault = stage.fault
    )
}
