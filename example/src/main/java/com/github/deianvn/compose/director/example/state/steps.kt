package com.github.deianvn.compose.director.example.state


sealed class MainStep {

    class FirstStep : MainStep()

    class SecondStep(val text: String) : MainStep()

    class ThirdStep : MainStep()

}
