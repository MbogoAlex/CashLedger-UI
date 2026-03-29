package com.records.pesa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val _newIntent = MutableStateFlow<Intent?>(null)
    val newIntentState: StateFlow<Intent?> = _newIntent.asStateFlow()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)        // keep getIntent() consistent
        _newIntent.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val viewModel: MainActivityViewModel = viewModel(factory = AppViewModelFactory.Factory)
            val uiState by viewModel.uiState.collectAsState()

            // Forward deep-link intents that arrive while the app is already running
            LaunchedEffect(navController) {
                newIntentState.collect { intent ->
                    if (intent != null) {
                        navController.handleDeepLink(intent)
                        _newIntent.value = null
                    }
                }
            }

            CashLedgerTheme(
                darkTheme = uiState.preferences?.darkMode == true && uiState.preferences?.paid == true,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.navigate) {
                        StartScreen(
                            navController = navController,
                            onSwitchTheme = { viewModel.switchDarkTheme() }
                        )
                    }
                }
            }
        }
    }
}