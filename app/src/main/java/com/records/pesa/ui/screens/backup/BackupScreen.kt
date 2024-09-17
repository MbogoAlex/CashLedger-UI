package com.records.pesa.ui.screens.backup

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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime

@Composable
fun BackupScreenComposable(
    navigateToPreviousScreen: () -> Unit,
) {
    BackHandler(onBack = navigateToPreviousScreen)
    val viewModel: BackupScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BackupScreen(
            backupSet = uiState.userDetails.backupSet,
            paymentStatus = true,
            transactionsNotBackedUp = 14,
            onBackup = {
                viewModel.backup()
            },
            onRestore = {
                viewModel.restore()
            },
            totalBackupItems = uiState.totalItems,
            backedUpItems = uiState.itemsInserted,
            backupStatus = uiState.backupStatus
        )
    }
}

@Composable
fun BackupScreen(
    backupSet: Boolean,
    paymentStatus: Boolean,
    transactionsNotBackedUp: Int,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    totalBackupItems: Int,
    backedUpItems: Int,
    backupStatus: BackupStatus,
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
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Backup your transactions",
                fontSize = screenFontSize(x = 18.0).sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Last backup: ${formatIsoDateTime(LocalDateTime.parse("2024-09-17T06:47:30.082328"))}",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            if(paymentStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automatic backup and restore: ",
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
                Text(text = "$transactionsNotBackedUp transactions scheduled for backup")
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row {
                    Button(
                        enabled = transactionsNotBackedUp != 0 && backupStatus != BackupStatus.LOADING,
                        onClick = onBackup,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(text = "Manual backup")
                    }
                    Spacer(modifier = Modifier.width(screenWidth(x = 8.0)))
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(text = "Manual restore")
                    }
                }

            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automatic backup and restore: ",
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
                Button(onClick = { /*TODO*/ }) {
                    Text(
                        text = "Pay subscription fee",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
            if(backupStatus == BackupStatus.LOADING) {
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                androidx.compose.material.Text(
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
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Backup your transactions",
                fontSize = screenFontSize(x = 18.0).sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Last backup: ${formatIsoDateTime(LocalDateTime.parse("2024-09-17T06:47:30.082328"))}",
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
                    onClick = onBackup,
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = "Backup now",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
                Spacer(modifier = Modifier.width(screenWidth(x = 8.0)))
                OutlinedButton(
                    enabled = backupStatus != BackupStatus.LOADING,
                    onClick = {
                        Toast.makeText(context, "Back up your data first", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = "Restore",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
        }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BackupScreenPreview() {
    CashLedgerTheme {
        BackupScreen(
            transactionsNotBackedUp = 15,
            backupSet = true,
            paymentStatus = true,
            onBackup = {},
            onRestore = {},
            totalBackupItems = 15,
            backedUpItems = 7,
            backupStatus = BackupStatus.INITIAL
        )
    }
}