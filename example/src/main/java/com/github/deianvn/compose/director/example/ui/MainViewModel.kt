package com.github.deianvn.compose.director.example.ui

import com.github.deianvn.compose.director.example.state.MainStep
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Status
import com.github.deianvn.compose.director.android.viewmodel.SimpleStateViewModel


class MainViewModel : SimpleStateViewModel<MainStep>(
    StateNode.head(
        step = MainStep.FirstStep(),
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

    fun submitText(input: String) {
        navigate {
            it.chain(
                step = MainStep.SecondStep(input),
                status = Status.IDLE
            )
        }
    }

    fun openThirdPage() {
        navigate {
            it.chain(
                step = MainStep.ThirdStep(),
                status = Status.IDLE
            )
        }
    }

    fun reset() {
        navigate {
            it.head.chain(
                step = MainStep.FirstStep(),
                status = Status.IDLE
            )
        }
    }

}
