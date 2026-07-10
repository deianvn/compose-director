package com.github.deianvn.compose.director.example.ui

import androidx.lifecycle.viewModelScope
import com.github.deianvn.compose.director.example.state.MainStep
import com.github.deianvn.compose.director.state.StateNode
import com.github.deianvn.compose.director.state.Status
import com.github.deianvn.compose.director.android.viewmodel.SimpleStateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds


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

    fun submitText(input: String) = viewModelScope.launch {

        withContext(Dispatchers.IO) {
            navigate {
                it.chain(status = Status.WORKING, remembered = false)
            }

            delay(3000L.milliseconds)
        }

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
