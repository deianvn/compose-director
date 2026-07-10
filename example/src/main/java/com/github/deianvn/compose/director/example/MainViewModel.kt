package com.github.deianvn.compose.director.example

import com.github.deianvn.compose.director.example.state.MainStep
import com.github.deianvn.compose.director.state.EmptySideData
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Status
import com.github.deianvn.compose.director.state.viewmodel.StateViewModel


class MainViewModel : StateViewModel<MainStep, EmptySideData, EmptySideData, EmptySideData>(
    StateNode.head(
        step = MainStep.Step1(),
        status = Status.WORKING,
        EmptySideData(),
        EmptySideData(),
        EmptySideData()
    )
) {

    init {

    }

    fun click1() {
        navigate {
            chain(
                step = MainStep.Step2(),
                status = Status.IDLE
            )
        }
    }

    fun click2() {
        navigate {
            chain(
                step = MainStep.Step3(),
                status = Status.IDLE
            )
        }
    }

    fun click3() {
        navigate {
            seek().chain(
                step = MainStep.Step1(),
                status = Status.IDLE
            )
        }
    }

}
