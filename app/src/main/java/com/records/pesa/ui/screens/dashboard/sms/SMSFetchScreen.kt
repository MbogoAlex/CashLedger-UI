package com.records.pesa.ui.screens.dashboard.sms

import android.Manifest
import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay

object SMSFetchScreenDestination : AppNavigation {
    override val title: String = "SMS fetch Screen"
    override val route: String = "sms-fetch-screen"
    val fromLogin: String = "fromLogin"
    val routeWithArgs: String = "$route/{$fromLogin}"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmsFetchScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToBackupRestoreScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    BackHandler(onBack = { activity?.finish() })
    val viewModel: SmsFetchScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val smsReadPermissionState         = rememberPermissionState(Manifest.permission.READ_SMS)
    val smsReceivePermissionState      = rememberPermissionState(Manifest.permission.RECEIVE_SMS)
    val phoneStatePermissionState      = rememberPermissionState(Manifest.permission.READ_PHONE_STATE)
    val phoneNumbersPermissionState    = rememberPermissionState(Manifest.permission.READ_PHONE_NUMBERS)

    LaunchedEffect(uiState.loadingStatus) {
        when (uiState.loadingStatus) {
            LoadingStatus.SUCCESS -> { navigateToHomeScreen(); viewModel.resetLoadingStatus() }
            else -> if (uiState.errorCode == 401) {
                navigateToLoginScreenWithArgs(uiState.userDetails.phoneNumber, uiState.userDetails.password)
                viewModel.resetLoadingStatus()
            }
        }
    }

    val smsReceiveHandler   = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val phoneStateHandler   = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { Log.d("SMSFetchScreen", "READ_PHONE_STATE: $it") }
    val phoneNumbersHandler = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { Log.d("SMSFetchScreen", "READ_PHONE_NUMBERS: $it") }
    val smsReadHandler      = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { Log.d("SMSFetchScreen", "READ_SMS: $it") }

    LaunchedEffect(smsReadPermissionState.status.isGranted) {
        if (!smsReadPermissionState.status.isGranted) smsReadHandler.launch(Manifest.permission.READ_SMS)
    }
    LaunchedEffect(smsReadPermissionState.status.isGranted, smsReceivePermissionState.status.isGranted) {
        if (smsReadPermissionState.status.isGranted && !smsReceivePermissionState.status.isGranted)
            smsReceiveHandler.launch(Manifest.permission.RECEIVE_SMS)
    }
    LaunchedEffect(smsReadPermissionState.status.isGranted, phoneStatePermissionState.status.isGranted) {
        if (smsReadPermissionState.status.isGranted && !phoneStatePermissionState.status.isGranted)
            phoneStateHandler.launch(Manifest.permission.READ_PHONE_STATE)
    }
    LaunchedEffect(phoneStatePermissionState.status.isGranted, phoneNumbersPermissionState.status.isGranted) {
        if (phoneStatePermissionState.status.isGranted && !phoneNumbersPermissionState.status.isGranted)
            phoneNumbersHandler.launch(Manifest.permission.READ_PHONE_NUMBERS)
    }

    // Start fetch after 5s grace period for permissions to settle
    LaunchedEffect(smsReadPermissionState.status.isGranted) {
        if (smsReadPermissionState.status.isGranted) {
            delay(5_000)
            if (uiState.loadingStatus != LoadingStatus.LOADING) {
                while (uiState.userDetails.userId == 0) { delay(1_000) }
                viewModel.fetchSmsMessages(context)
            }
        }
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        SmsFetchScreen(
            messagesSize  = uiState.messagesSize,
            messagesSent  = uiState.messagesSent,
            loadingStatus = uiState.loadingStatus
        )
    }
}

@Composable
fun SmsFetchScreen(
    messagesSize: Float,
    messagesSent: Float,
    loadingStatus: LoadingStatus = LoadingStatus.LOADING,
    modifier: Modifier = Modifier
) {
    val primary  = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    var statusText by rememberSaveable { mutableStateOf("Reading your M-PESA SMS") }
    var subText    by rememberSaveable { mutableStateOf("Scanning your inbox for M-PESA transactions. This may take a moment on first launch.") }

    LaunchedEffect(loadingStatus) {
        when (loadingStatus) {
            LoadingStatus.LOADING -> {
                statusText = "Reading your M-PESA SMS"
                subText    = "Scanning your inbox for M-PESA transactions. Please keep the app open."
            }
            LoadingStatus.SUCCESS -> {
                statusText = "All done!"
                subText    = "Your transactions have been imported successfully."
            }
            LoadingStatus.FAIL -> {
                statusText = "Something went wrong"
                subText    = "Could not read SMS. Check permissions and try again."
            }
            else -> {}
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // ── Hero banner card ──────────────────────────────────────────────
        ElevatedCard(
            shape     = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.14f),
                                tertiary.copy(alpha = 0.08f),
                                primary.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.transactions),
                            contentDescription = null,
                            tint     = primary,
                            modifier = Modifier
                                .size(38.dp)
                                .then(
                                    if (loadingStatus == LoadingStatus.LOADING ||
                                        loadingStatus == LoadingStatus.INITIAL)
                                        Modifier.rotate(rotation)
                                    else Modifier
                                )
                        )
                    }
                    Text(
                        text      = statusText,
                        style     = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color     = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text       = subText,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign  = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Progress card (shown while loading) ───────────────────────────
        if (loadingStatus == LoadingStatus.LOADING || loadingStatus == LoadingStatus.INITIAL) {
            ElevatedCard(
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Progress",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text  = "${messagesSent.toInt()} / ${messagesSize.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            if ((messagesSent / messagesSize).isNaN()) 0f
                            else (messagesSent / messagesSize).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color      = primary,
                        trackColor = primary.copy(alpha = 0.12f)
                    )
                    Text(
                        text       = "Importing transactions, please keep the app open\u2026",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SmsFetchScreenPreview() {
    CashLedgerTheme {
        SmsFetchScreen(
            messagesSize  = 100f,
            messagesSent  = 50f,
            loadingStatus = LoadingStatus.LOADING
        )
    }
}
