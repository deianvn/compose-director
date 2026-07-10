package com.github.deianvn.compose.director.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.deianvn.compose.director.android.state.StateBackHandler
import com.github.deianvn.compose.director.android.state.finishOnNull
import com.github.deianvn.compose.director.example.state.MainStep
import com.github.deianvn.compose.director.example.ui.components.LoadedContent


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        Content(viewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun Content(viewModel: MainViewModel) {
        val state by viewModel.state.collectAsStateWithLifecycle()

        StateBackHandler(viewModel)

        state.finishOnNull { state ->
            LoadedContent(
                modifier = Modifier.fillMaxSize(),
                isLoading = state.status.isWorking()
            ) {
                when (val step = state.step) {
                    is MainStep.FirstStep -> FirstPage(onSubmit = viewModel::submitText)
                    is MainStep.SecondStep -> SecondPage(
                        text = step.text, onNext = viewModel::openThirdPage
                    )

                    is MainStep.ThirdStep -> ThirdPage(onReset = viewModel::reset)
                }
            }
        }
    }

    @Composable
    private fun FirstPage(
        onSubmit: (String) -> Unit
    ) {
        var text by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it }
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSubmit(text) }
            ) {
                Text(text = "Click")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(text = "Page 1")
        }
    }

    @Composable
    private fun SecondPage(
        text: String,
        onNext: () -> Unit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = text)

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNext
            ) {
                Text(text = "Click")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(text = "Page 2")
        }
    }

    @Composable
    private fun ThirdPage(
        onReset: () -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onReset
            ) {
                Text(text = "Reset")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(text = "Page 3")
        }
    }
}
