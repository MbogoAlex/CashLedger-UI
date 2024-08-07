package com.records.pesa.ui.screens.dashboard.sms

import android.Manifest
import android.app.Activity
import android.widget.Space
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme

object SMSFetchScreenDestination: AppNavigation {
    override val title: String = "SMS fetch Screen"
    override val route: String = "sms-fetch-screen"

}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmsFetchScreenComposable(
    navigateToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    BackHandler(onBack = {activity?.finish()})
    val viewModel: SmsFetchScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val smsReadPermissionState = rememberPermissionState(permission = Manifest.permission.READ_SMS)
    val smsReceivePermissionState = rememberPermissionState(permission = Manifest.permission.RECEIVE_SMS)

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        navigateToHomeScreen()
        viewModel.resetLoadingStatus()
    }

    val smsReadRequestPermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {
            viewModel.getLatestTransactionCodes(context)
        } else {

        }
    }

    val smsReceivePermissionHandler = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {

        } else {

        }
    }

    LaunchedEffect(smsReadPermissionState, smsReceivePermissionState) {
//        if(!smsReceivePermissionState.status.isGranted) {
//            smsReceivePermissionHandler.launch(Manifest.permission.RECEIVE_SMS)
//        }

        if(!smsReadPermissionState.status.isGranted) {
//            Todo: Show rationale if needed
            smsReadRequestPermissionHandler.launch(Manifest.permission.READ_SMS)
        } else {
            viewModel.getLatestTransactionCodes(context)
        }

    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SmsFetchScreen(
            messagesSize = uiState.messagesSize,
            messagesSent = uiState.messagesSent
        )
    }

}

@Composable
fun SmsFetchScreen(
    messagesSize: Float,
    messagesSent: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.cashledger_logo),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
        )
//        Spacer(modifier = Modifier.weight(1f))
        LinearProgressIndicator(
            progress = { (messagesSent / messagesSize).toFloat() },
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SmsFetchScreenPreview() {
    CashLedgerTheme {
        SmsFetchScreen(
            messagesSize = 100f,
            messagesSent = 50f
        )
    }
}