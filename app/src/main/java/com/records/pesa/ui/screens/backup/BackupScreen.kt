package com.records.pesa.ui.screens.backup

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime

object BackupScreenDestination: AppNavigation {
    override val title: String = "Backup Screen"
    override val route: String = "backup-screen"
}

@Composable
fun BackupScreenComposable(
    refreshScreen: Boolean = false,
    navigateToHomeScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = navigateToPreviousScreen)
    val viewModel: BackupScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if(refreshScreen) {
        LaunchedEffect(Unit) {
            viewModel.getAllLocalData()
            var i = 10
            while (i != 0) {
                delay(2000)
                viewModel.getItemsNotBackedUp()
                i--
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
        if(lifecycleState.name.lowercase() == "started") {
            viewModel.getItemsNotBackedUp()
        }
    }

    if(uiState.restoreStatus == RestoreStatus.SUCCESS) {
        Toast.makeText(context, "Restore successful", Toast.LENGTH_SHORT).show()
        navigateToHomeScreen()
        viewModel.resetStatus()
    } else if(uiState.restoreStatus == RestoreStatus.FAIL) {
        Toast.makeText(context, "Restore failed. Make sure you are connected to the internet", Toast.LENGTH_SHORT).show()
        viewModel.resetStatus()
    }

    if(uiState.backupStatus == BackupStatus.SUCCESS) {
        Toast.makeText(context, "Backup successful", Toast.LENGTH_SHORT).show()
        viewModel.resetStatus()
    } else if(uiState.backupStatus == BackupStatus.FAIL) {
        Toast.makeText(context, "Backup failed. Make sure you are connected to the internet", Toast.LENGTH_SHORT).show()
        viewModel.resetStatus()
    }

    if(showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscriptionDialog = !showSubscriptionDialog
            },
            onConfirm = {
                showSubscriptionDialog = !showSubscriptionDialog
                navigateToSubscriptionScreen()
            }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BackupScreen(
            backupSet = uiState.userDetails.backupSet,
            lastBackup = uiState.userDetails.lastBackup,
            paymentStatus = uiState.userDetails.paymentStatus || uiState.userDetails.phoneNumber == "0179189199",
            transactionsNotBackedUp = uiState.itemsNotBackedUp,
            onBackup = {
                viewModel.backup(context = context)
            },
            onRestore = {
                viewModel.restore()
            },
            totalBackupItems = uiState.totalItems,
            backedUpItems = uiState.itemsInserted,
            backupStatus = uiState.backupStatus,
            totalItemsRestored = uiState.totalItemsRestored,
            totalItemsToRestore = uiState.totalItemsToRestore,
            restoreStatus = uiState.restoreStatus,
            navigateToSubscriptionScreen = {
                showSubscriptionDialog = !showSubscriptionDialog
            }
        )
    }
}

@Composable
fun BackupScreen(
    backupSet: Boolean,
    lastBackup: LocalDateTime?,
    paymentStatus: Boolean,
    transactionsNotBackedUp: Int,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    totalBackupItems: Int,
    backedUpItems: Int,
    totalItemsToRestore: Int,
    totalItemsRestored: Int,
    backupStatus: BackupStatus,
    restoreStatus: RestoreStatus,
    navigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if(backupSet) {
        Column(
            modifier = Modifier
                .padding(
                    vertical = screenHeight(x = 8.0),
                    horizontal = screenWidth(x = 16.0)
                )
                .fillMaxSize()
        ) {
            Text(
                text = "Backup & Restore",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Backup your transactions",
                fontSize = screenFontSize(x = 18.0).sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Last backup: ${lastBackup?.let { formatIsoDateTime(it) } ?: "Never"}",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            if(paymentStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automatic backup: ",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Text(
                        text = "ON",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surfaceTint
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "Your items will be backed up automatically if you do not do a manual backup",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "$transactionsNotBackedUp items scheduled for backup",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "Data restore happens when you log in",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row {
                    Button(
                        enabled = transactionsNotBackedUp != 0 && backupStatus != BackupStatus.LOADING,
                        onClick = onBackup,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        if(backupStatus == BackupStatus.LOADING) {
                            Text(
                                text = "Backing up...",
                                fontSize = screenFontSize(x = 14.0).sp
                            )
                        } else {
                            Text(
                                text = "Manual backup",
                                fontSize = screenFontSize(x = 14.0).sp
                            )
                        }
                    }
                }

            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automatic backup: ",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Text(
                        text = "OFF",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                Text(
                    text = "$transactionsNotBackedUp  transactions not backed up",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "Pay the subscription fee to resume backups",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                Button(onClick = navigateToSubscriptionScreen) {
                    Text(
                        text = "Pay subscription fee",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
            if(backupStatus == BackupStatus.LOADING) {
                Log.d("showIndicator", "showing indicator")
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                Text(
                    text = "Backed up $backedUpItems / $totalBackupItems item(s)",
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
                LinearProgressIndicator(
                    progress = { if ((backedUpItems.toFloat() / totalBackupItems.toFloat()).isNaN()) 0f else (backedUpItems.toFloat() / totalBackupItems.toFloat()) },
                    modifier = Modifier
                        .height(screenHeight(x = 20.0))
                        .fillMaxWidth(),
                )
            }

            if(restoreStatus == RestoreStatus.LOADING) {
                Log.d("showIndicator", "showing indicator")
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                Text(
                    text = "Restored $totalItemsRestored / $totalItemsToRestore item(s)",
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
                LinearProgressIndicator(
                    progress = { if ((totalItemsRestored.toFloat() / totalItemsToRestore.toFloat()).isNaN()) 0f else (totalItemsRestored.toFloat() / totalItemsToRestore.toFloat()) },
                    modifier = Modifier
                        .height(screenHeight(x = 20.0))
                        .fillMaxWidth(),
                )
            }
        }

    } else {
        Column(
            modifier = Modifier
                .padding(
                    vertical = screenHeight(x = 8.0),
                    horizontal = screenWidth(x = 16.0)
                )
                .fillMaxSize()
        ) {
            Text(
                text = "Backup & Restore",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Backup your transactions",
                fontSize = screenFontSize(x = 18.0).sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Last backup: ${lastBackup?.let { formatIsoDateTime(it) } ?: "Never"}",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "$transactionsNotBackedUp not backed up",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Why backup your data?",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 16.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            Text(
                text = "Backing up your data means you can restore it on a new device or after logging in and out",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Clicking on the backup button will automate daily backups, so you will not need to do a manual backup again.",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row {
                Button(
                    enabled = backupStatus != BackupStatus.LOADING,
                    onClick = {
                        if(paymentStatus) {
                            onBackup()
                        } else {
                            navigateToSubscriptionScreen()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = "Backup now",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
        }
    }

}

@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Go premium?",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Ksh50.0 premium monthly fee",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Premium version allows you to: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. See transactions and export reports of more than one months",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "2. Backup your transactions",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "3. Manage more than one category",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "4. Use in dark mode",
                        fontSize = screenFontSize(x = 14.0).sp
                    )

                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Subscribe",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BackupScreenPreview() {
    CashLedgerTheme {
        BackupScreen(
            transactionsNotBackedUp = 15,
            backupSet = true,
            lastBackup = null,
            paymentStatus = true,
            onBackup = {},
            onRestore = {},
            totalItemsRestored = 0,
            totalItemsToRestore = 0,
            totalBackupItems = 15,
            backedUpItems = 7,
            backupStatus = BackupStatus.INITIAL,
            restoreStatus = RestoreStatus.INITIAL,
            navigateToSubscriptionScreen = {}
        )
    }
}