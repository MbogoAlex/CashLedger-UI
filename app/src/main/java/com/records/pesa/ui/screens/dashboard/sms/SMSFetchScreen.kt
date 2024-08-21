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

}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmsFetchScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    BackHandler(onBack = {activity?.finish()})
    val viewModel: SmsFetchScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val smsReadPermissionState = rememberPermissionState(permission = Manifest.permission.READ_SMS)
    val smsReceivePermissionState = rememberPermissionState(permission = Manifest.permission.RECEIVE_SMS)
    val scope = rememberCoroutineScope()

    var timer by rememberSaveable {
        mutableFloatStateOf(0.0f)
    }

    if(uiState.counterOn) {
        Log.d("COUNTER", "COUNTER")
        LaunchedEffect(Unit) {
            while (uiState.messagesSize != timer) {
                delay(5)
                timer += 1.0f
            }
            viewModel.resetTimer()
            navigateToHomeScreen()
            viewModel.resetLoadingStatus()
        }
    }

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {

    } else if(uiState.errorCode == 401) {
        navigateToLoginScreenWithArgs(uiState.userDetails.phoneNumber, uiState.userDetails.password)
        viewModel.resetLoadingStatus()
    }


    val smsReadRequestPermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {
            scope.launch {
                while (uiState.userDetails.userId == 0) {
                    delay(1000)
                }
                viewModel.getLatestTransactionCodes(context)

            }
        }
    }

    val smsReceivePermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {

        } else {

        }
    }

    LaunchedEffect(smsReadPermissionState, smsReceivePermissionState) {

        if(!smsReadPermissionState.status.isGranted) {
//            Todo: Show rationale if needed
            smsReadRequestPermissionHandler.launch(Manifest.permission.READ_SMS)
        } else {
            while (uiState.userDetails.userId == 0) {
                delay(1000)
            }
            viewModel.getLatestTransactionCodes(context)
        }

    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SmsFetchScreen(
            messagesSize = uiState.messagesSize,
            messagesSent = uiState.messagesSent,
            timer = timer
        )
    }

}

@Composable
fun SmsFetchScreen(
    messagesSize: Float,
    messagesSent: Float,
    timer: Float,
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
            text = "Processing ${messagesSize.toInt()} item(s)",
            fontSize = screenFontSize(x = 14.0).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        LinearProgressIndicator(
            progress = { if ((timer / messagesSize).isNaN()) 0f else (timer / messagesSize) },
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
            timer = 0.1f
        )
    }
}