package com.records.pesa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.ui.theme.CashLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainActivityViewModel = viewModel(factory = AppViewModelFactory.Factory)
            val uiState by viewModel.uiState.collectAsState()

            CashLedgerTheme(
                darkTheme = uiState.preferences?.darkMode == true && uiState.preferences?.paid == true
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if(uiState.navigate) {
                        StartScreen(
                            onSwitchTheme = {
                                viewModel.switchDarkTheme()
                            }
                        )
                    }
                }
            }
        }
    }
}