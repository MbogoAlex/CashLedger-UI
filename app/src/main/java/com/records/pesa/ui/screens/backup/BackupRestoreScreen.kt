package com.records.pesa.ui.screens.backup

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object BackupRestoreScreenDestination: AppNavigation {
    override val title: String = "Backup-Restore Screen"
    override val route: String = "backup-restore-screen"
}

@Composable
fun BackupRestoreScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToHomeScreenWithArgs: (screen: String) -> Unit,
    navigateToSmsFetchScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    BackHandler(onBack = {activity?.finish()})
    val viewModel: BackupRestoreScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.restoreStatus == RestoreStatus.SUCCESS) {
        navigateToSmsFetchScreen()
        viewModel.resetStatus()
    } else if(uiState.restoreStatus == RestoreStatus.FAIL) {
        Toast.makeText(context, "Restoring failed. Try to restore your data manually", Toast.LENGTH_LONG).show()
        navigateToHomeScreenWithArgs("backup-screen")
        viewModel.resetStatus()
    }


    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BackupRestoreScreen(
            totalItemsToRestore = uiState.totalItemsToRestore.toFloat(),
            totalItemsRestored = uiState.totalItemsRestored.toFloat(),
            onRestore = {
                viewModel.restore()
            },
            restoreStatus = uiState.restoreStatus
        )
    }

}

@Composable
fun BackupRestoreScreen(
    totalItemsToRestore: Float,
    totalItemsRestored: Float,
    restoreStatus: RestoreStatus,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable {
        mutableStateOf("Restoring your data...")
    }
    LaunchedEffect(Unit) {
        delay(5000)
        text = "Wait a minute..."
    }
    val scope = rememberCoroutineScope()
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
        if(restoreStatus == RestoreStatus.LOADING) {
            Text(
                text = "Restoring ${totalItemsRestored.toInt()} / ${totalItemsToRestore.toInt()} item(s)",
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            LinearProgressIndicator(
                progress = { if ((totalItemsRestored / totalItemsToRestore).isNaN()) 0f else (totalItemsRestored / totalItemsToRestore) },
                modifier = Modifier
                    .height(screenHeight(x = 20.0))
                    .fillMaxWidth(),
            )
        } else if(restoreStatus == RestoreStatus.FAIL) {
            Text(
                text = "Restoring failed. Check your internet connection and try to restore your data manually",
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            Button(
                onClick = {
                    text = "Restoring your data..."
                    onRestore()
                    scope.launch {
                        delay(2000)
                        text = "Wait a minute..."
                    }
                }
            ) {
                Text(text = "Restore manually")
            }
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BackupRestoreScreenPreview() {
    CashLedgerTheme {
        BackupRestoreScreen(
            totalItemsRestored = 20f,
            totalItemsToRestore = 10f,
            restoreStatus = RestoreStatus.INITIAL,
            onRestore = {}
        )
    }
}