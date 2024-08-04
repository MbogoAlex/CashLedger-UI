package com.records.pesa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay

object SplashScreenDestination: AppNavigation {
    override val title: String = "Splash screen"
    override val route: String = "splash-screen"

}
@Composable
fun SplashScreenComposable(
    navigateToSmsFetchScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(2000L)
        navigateToSmsFetchScreen()
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
            painter = painterResource(id = R.drawable.cashledger_logo),
            contentDescription = null,
            modifier = Modifier
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