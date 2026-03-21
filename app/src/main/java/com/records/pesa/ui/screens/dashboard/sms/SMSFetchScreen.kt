package com.records.pesa.ui.screens.dashboard.sms

import android.Manifest
import android.app.Activity
import android.util.Log
import android.widget.Space
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SMSFetchScreenDestination: AppNavigation {
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
    BackHandler(onBack = {activity?.finish()})
    val viewModel: SmsFetchScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val smsReadPermissionState = rememberPermissionState(permission = Manifest.permission.READ_SMS)
    val smsReceivePermissionState = rememberPermissionState(permission = Manifest.permission.RECEIVE_SMS)
    val phoneStatePermissionState = rememberPermissionState(permission = Manifest.permission.READ_PHONE_STATE)
    val phoneNumbersPermissionState = rememberPermissionState(permission = Manifest.permission.READ_PHONE_NUMBERS)
    val scope = rememberCoroutineScope()


    LaunchedEffect(uiState.loadingStatus) {
        if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
            navigateToHomeScreen()
            viewModel.resetLoadingStatus()
        } else if(uiState.errorCode == 401) {
            navigateToLoginScreenWithArgs(uiState.userDetails.phoneNumber, uiState.userDetails.password)
            viewModel.resetLoadingStatus()
        }
    }

    val smsReceivePermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {

        } else {

        }
    }

    val phoneStatePermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        Log.d("SMSFetchScreen", "READ_PHONE_STATE permission granted: $isGranted")
        if (isGranted) {
            // Check if all permissions are now granted and start SMS fetch if ready
            if (smsReadPermissionState.status.isGranted &&
                phoneNumbersPermissionState.status.isGranted) {
//                scope.launch {
//                    while (uiState.userDetails.userId == 0) {
//                        delay(1000)
//                    }
//                    Log.d("SMSFetchScreen", "Called 1st Time")
//                    viewModel.fetchSmsMessages(context)
//                }
            }
        } else {
            // Phone state permission denied - continue with SMS fetching anyway
            Log.d("SMSFetchScreen", "READ_PHONE_STATE permission denied, continuing with SMS fetch")
            if (smsReadPermissionState.status.isGranted) {
//                scope.launch {
//                    while (uiState.userDetails.userId == 0) {
//                        delay(1000)
//                    }
//                    viewModel.fetchSmsMessages(context)
//                }
            }
        }
    }

    val phoneNumbersPermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        Log.d("SMSFetchScreen", "READ_PHONE_NUMBERS permission granted: $isGranted")
        if (isGranted) {
            // Check if all permissions are now granted and start SMS fetch if ready
            if (smsReadPermissionState.status.isGranted &&
                phoneStatePermissionState.status.isGranted) {
//                scope.launch {
//                    while (uiState.userDetails.userId == 0) {
//                        delay(1000)
//                    }
//                    viewModel.fetchSmsMessages(context)
//                }
            }
        } else {
            // Phone numbers permission denied - continue with SMS fetching anyway
            Log.d("SMSFetchScreen", "READ_PHONE_NUMBERS permission denied, continuing with SMS fetch")
            if (smsReadPermissionState.status.isGranted) {
//                scope.launch {
//                    while (uiState.userDetails.userId == 0) {
//                        delay(1000)
//                    }
//                    viewModel.fetchSmsMessages(context)
//                }
            }
        }
    }

    val smsReadRequestPermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {
            Log.d("SMSFetchScreen", "READ_SMS permission granted, checking other permissions")
            // After SMS permission is granted, check and request phone permissions
//            scope.launch {
//                while (uiState.userDetails.userId == 0) {
//                    delay(1000)
//                }
//                viewModel.fetchSmsMessages(context)
//            }
        }
    }

    // Request SMS permission first
    LaunchedEffect(smsReadPermissionState.status.isGranted) {
        if (!smsReadPermissionState.status.isGranted) {
            Log.d("SMSFetchScreen", "Requesting READ_SMS permission")
            smsReadRequestPermissionHandler.launch(Manifest.permission.READ_SMS)
        }
    }

    // After SMS permission is granted, request phone state permission
    LaunchedEffect(smsReadPermissionState.status.isGranted, phoneStatePermissionState.status.isGranted) {
        if (smsReadPermissionState.status.isGranted && !phoneStatePermissionState.status.isGranted) {
            Log.d("SMSFetchScreen", "Requesting READ_PHONE_STATE permission")
            phoneStatePermissionHandler.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    // After phone state permission is granted, request phone numbers permission
    LaunchedEffect(phoneStatePermissionState.status.isGranted, phoneNumbersPermissionState.status.isGranted) {
        if (phoneStatePermissionState.status.isGranted && !phoneNumbersPermissionState.status.isGranted) {
            Log.d("SMSFetchScreen", "Requesting READ_PHONE_NUMBERS permission")
            phoneNumbersPermissionHandler.launch(Manifest.permission.READ_PHONE_NUMBERS)
        }
    }





    // Fallback: Start SMS fetching if SMS permission is granted, regardless of phone permissions
    LaunchedEffect(smsReadPermissionState.status.isGranted) {
        if (smsReadPermissionState.status.isGranted) {
            // Wait a bit for phone permissions to be requested
            delay(5000) // 5 second grace period for phone permissions

            // If we reach here and haven't started SMS fetching yet, start it anyway
            if (uiState.loadingStatus != LoadingStatus.LOADING) {
                Log.d("SMSFetchScreen", "Starting SMS fetch after timeout (phone permissions may be denied)")
                while (uiState.userDetails.userId == 0) {
                    delay(1000)
                }
                viewModel.fetchSmsMessages(context)
            }
        }
    }

    // When all permissions are granted, start SMS fetching immediately
//    LaunchedEffect(smsReadPermissionState.status.isGranted, phoneStatePermissionState.status.isGranted, phoneNumbersPermissionState.status.isGranted) {
//        if (smsReadPermissionState.status.isGranted &&
//            phoneStatePermissionState.status.isGranted &&
//            phoneNumbersPermissionState.status.isGranted) {
//            Log.d("SMSFetchScreen", "All permissions granted, starting SMS fetch")
//            while (uiState.userDetails.userId == 0) {
//                delay(1000)
//            }
//            viewModel.fetchSmsMessages(context)
//        }
//    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SmsFetchScreen(
            messagesSize = uiState.messagesSize,
            messagesSent = uiState.messagesSent,
        )
    }

}

@Composable
fun SmsFetchScreen(
    messagesSize: Float,
    messagesSent: Float,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable {
        mutableStateOf("Fetching transactions...")
    }
    LaunchedEffect(Unit) {
        delay(2000)
        text = "Wait a minute..."
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 8.0)
            )
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.undraw_person),
            contentDescription = null,
//            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(screenWidth(x = 450.0))
                .aspectRatio(16f / 9f)
        )

        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = screenFontSize(x = 18.0).sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Processing ${messagesSent.toInt()} / ${messagesSize.toInt()} item(s)",
            fontSize = screenFontSize(x = 14.0).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        LinearProgressIndicator(
            progress = { if ((messagesSent / messagesSize).isNaN()) 0f else (messagesSent / messagesSize) },
            modifier = Modifier
                .height(screenHeight(x = 20.0))
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SmsFetchScreenPreview() {
    CashLedgerTheme {
        SmsFetchScreen(
            messagesSize = 100f,
            messagesSent = 50f,
        )
    }
}