package com.github.deianvn.compose.director.example.state

import com.github.deianvn.compose.director.state.Step

sealed class MainStep : Step {

    class FirstStep : MainStep()

    class SecondStep(val text: String) : MainStep()

    class ThirdStep : MainStep()

}
