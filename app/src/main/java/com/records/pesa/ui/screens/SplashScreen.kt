package com.records.pesa.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay

object SplashScreenDestination: AppNavigation {
    override val title: String = "Splash screen"
    override val route: String = "splash-screen"

}
@Composable
fun SplashScreenComposable(
    navigateToRegistrationScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    navigateToSmsFetchScreen: () -> Unit,
    modifier: Modifier = Modifier
) {

    val viewModel: SplashScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.launchStatus) {
        Log.d("launchStatus", uiState.launchStatus.toString())
        if(uiState.launchStatus == LoadingStatus.SUCCESS) {
            Log.d("SplashScreen", "Preferences: ${uiState.preferences}, UserDetails: ${uiState.userDetails}")
            delay(2000L)
            if(uiState.preferences?.loggedIn == true) {
                navigateToSmsFetchScreen()
            } else {
                if(!uiState.userDetails?.phoneNumber.isNullOrEmpty() && !uiState.userDetails?.password.isNullOrEmpty()) {
                    navigateToLoginScreenWithArgs(uiState.userDetails!!.phoneNumber, uiState.userDetails!!.password)
                } else {
                    if(uiState.userDetails == null) {
                        navigateToRegistrationScreen()
                    } else {
                        navigateToLoginScreen()
                    }
                }
            }
        }


    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SplashScreen()
    }
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.mpesa_ledge_playstore_logo_no_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(screenWidth(x = 150.0))
                .clip(CircleShape)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    CashLedgerTheme {
        SplashScreen()
    }
}