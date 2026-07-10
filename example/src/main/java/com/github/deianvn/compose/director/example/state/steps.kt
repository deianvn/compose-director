package com.github.deianvn.compose.director.example.state

import com.github.deianvn.compose.director.state.Step

sealed class MainStep : Step {

    class Step1 : MainStep()

    class Step2(val text: String) : MainStep()

    class Step3 : MainStep()

}
