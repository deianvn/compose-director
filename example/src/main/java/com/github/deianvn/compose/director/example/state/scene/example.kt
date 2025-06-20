package com.github.deianvn.compose.director.example.state.scene

import com.github.deianvn.compose.director.core.Scene
import com.github.deianvn.compose.director.processor.Prop


sealed class ExampleScene : Scene {

    class FirstScene(
        @Prop(persistent = false)
        val showInputDialog: Boolean = false
    ) : ExampleScene()

    class SecondScene(
        @Prop(persistent = true)
        val name: String = ""
    ) : ExampleScene()

}
