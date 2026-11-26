package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ExactTimeApp
import com.example.ui.ExactTimeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ExactTimeApplication
        setContent {
            val viewModel: ExactTimeViewModel = viewModel(
                factory = ExactTimeViewModel.provideFactory(app)
            )
            ExactTimeApp(viewModel = viewModel)
        }
    }
}
