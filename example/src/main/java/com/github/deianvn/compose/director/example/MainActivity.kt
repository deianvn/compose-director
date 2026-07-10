package com.github.deianvn.compose.director.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import com.github.deianvn.compose.director.example.state.MainStep
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(viewModel)
                }
            }
        }
    }
}

@Composable
fun App(viewModel: MainViewModel) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            Dispatcher(viewModel)
        }
    }
}

@Composable
fun Dispatcher(viewModel: MainViewModel) {
    val activity = LocalActivity.current
    val node by viewModel.node.collectAsStateWithLifecycle()
    val step = node?.step

    when (step) {
        is MainStep.Step1 -> {
            var text by remember { mutableStateOf("") }

            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it }
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.click1(text) }
                ) {
                    Text(text = "Click 1")
                }
            }
        }

        is MainStep.Step2 -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = step.text
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::click2
                ) {
                    Text(text = "Click 2")
                }
            }
        }

        is MainStep.Step3 -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::click3
                ) {
                    Text(text = "Reset")
                }
            }
        }

        null -> LaunchedEffect(Unit) {
            activity?.finish()
        }
    }

    BackHandler(onBack = viewModel::back)
}
