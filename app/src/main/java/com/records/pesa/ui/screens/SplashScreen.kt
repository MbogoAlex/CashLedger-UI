package com.records.pesa.ui.screens

import android.util.Log
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay

object SplashScreenDestination : AppNavigation {
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
        if (uiState.launchStatus == LoadingStatus.SUCCESS) {
            // Minimum visual time so animations complete — data is already loaded at this point
            delay(800L)
            if (uiState.preferences?.loggedIn == true) {
                navigateToSmsFetchScreen()
            } else {
                if (!uiState.userDetails?.phoneNumber.isNullOrEmpty() && !uiState.userDetails?.password.isNullOrEmpty()) {
                    navigateToLoginScreenWithArgs(uiState.userDetails!!.phoneNumber, uiState.userDetails!!.password)
                } else {
                    if (uiState.userDetails == null) {
                        navigateToRegistrationScreen()
                    } else {
                        navigateToLoginScreen()
                    }
                }
            }
        }
    }

    SplashScreen()
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    // Entry animation trigger
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Logo fade + scale in
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.75f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    // Text slide up + fade in (slightly delayed)
    val textAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "textAlpha"
    )
    val textOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 18.dp,
        animationSpec = tween(600, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "textOffset"
    )

    // Continuous glow ring pulse
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        background,
                        primary.copy(alpha = 0.07f),
                        primary.copy(alpha = 0.12f),
                        primary.copy(alpha = 0.05f),
                        background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with animated glow rings
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(screenWidth(x = 160.0))
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = ringAlpha * 0.5f))
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(screenWidth(x = 140.0))
                        .scale(ringScale * 0.97f)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = ringAlpha * 0.7f))
                )
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.mpesa_ledge_playstore_logo_no_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(screenWidth(x = 120.0))
                        .scale(logoScale)
                        .alpha(logoAlpha)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                text = "Mpesa Ledger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .offset(y = textOffset)
                    .alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Track every shilling, effortlessly",
                style = MaterialTheme.typography.bodyMedium,
                color = primary.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = textOffset)
                    .alpha(textAlpha)
            )
        }

        // Animated loading dots at the bottom
        SplashLoadingDots(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
        )
    }
}

@Composable
private fun SplashLoadingDots(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 160, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = dotAlpha))
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    CashLedgerTheme {
        SplashScreen()
    }
}
