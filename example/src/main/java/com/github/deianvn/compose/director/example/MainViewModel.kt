package com.github.deianvn.compose.director.example

import com.github.deianvn.compose.director.example.state.MainStep
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Status
import com.github.deianvn.compose.director.state.viewmodel.SimpleStateViewModel


class MainViewModel : SimpleStateViewModel<MainStep>(
    StateNode.head(
        step = MainStep.Step1(),
        status = Status.WORKING
    )
) {

    init {
        navigate {
            it.chain(
                status = Status.IDLE
            )
        }
    }

    fun click1(input: String) {
        navigate {
            it.chain(
                step = MainStep.Step2(input),
                status = Status.IDLE
            )
        }
    }

    fun click2() {
        navigate {
            it.chain(
                step = MainStep.Step3(),
                status = Status.IDLE
            )
        }
    }

    fun click3() {
        navigate {
            it.head.chain(
                step = MainStep.Step1(),
                status = Status.IDLE
            )
        }
    }

}
