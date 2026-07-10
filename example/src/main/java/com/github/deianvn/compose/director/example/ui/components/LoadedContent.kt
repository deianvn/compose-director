package com.github.deianvn.compose.director.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp


@Composable
fun LoadedContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    showContentWhileLoading: Boolean = false,
    composable: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        composable()

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
                    .background(Color.White.copy(alpha = if (showContentWhileLoading) 0.5f else 1f))
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
