package com.records.pesa.ui.screens.backup

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
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
    val context = LocalContext.current
    BackHandler(onBack = {
        Toast.makeText(context, "You can't go back! Don't interrupt at this point", Toast.LENGTH_SHORT).show()
    })
    val viewModel: BackupRestoreScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.restoreStatus == RestoreStatus.SUCCESS) {
        navigateToSmsFetchScreen()
        viewModel.resetStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BackupRestoreScreen(
            totalItemsToRestore = uiState.totalItemsToRestore.toFloat(),
            totalItemsRestored = uiState.totalItemsRestored.toFloat(),
            onRestore = { viewModel.restore() },
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
    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val error     = MaterialTheme.colorScheme.error
    val scope     = rememberCoroutineScope()

    // Spinning animation for the icon while loading
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    var statusText by rememberSaveable { mutableStateOf("Restore your data") }
    var subText by rememberSaveable {
        mutableStateOf("We found a backup of your data on the cloud. Tap below to restore your categories, budgets, and transactions.")
    }

    LaunchedEffect(restoreStatus) {
        when(restoreStatus) {
            RestoreStatus.LOADING -> {
                statusText = "Restoring your data…"
                subText    = "Please keep the app open. This may take a moment."
            }
            RestoreStatus.SUCCESS -> {
                statusText = "All done!"
                subText    = "Your data has been restored successfully."
            }
            RestoreStatus.FAIL -> {
                statusText = "Restore failed"
                subText    = "Check your internet connection and try again."
            }
            else -> { /* INITIAL — defaults already set */ }
        }
    }

    Column(
        verticalArrangement   = Arrangement.Top,
        horizontalAlignment   = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(40.dp))

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
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (restoreStatus == RestoreStatus.FAIL)
                                    error.copy(alpha = 0.12f)
                                else
                                    primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (restoreStatus == RestoreStatus.FAIL) R.drawable.ic_error_outline
                                else R.drawable.backup_restore
                            ),
                            contentDescription = null,
                            tint = if (restoreStatus == RestoreStatus.FAIL) error else primary,
                            modifier = Modifier
                                .size(38.dp)
                                .then(
                                    if (restoreStatus == RestoreStatus.LOADING)
                                        Modifier.rotate(rotation)
                                    else Modifier
                                )
                        )
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Progress section (LOADING only) ──────────────────────────────
        if (restoreStatus == RestoreStatus.LOADING) {
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
                            text = "Progress",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${totalItemsRestored.toInt()} / ${totalItemsToRestore.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            if ((totalItemsRestored / totalItemsToRestore).isNaN()) 0f
                            else (totalItemsRestored / totalItemsToRestore).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = primary,
                        trackColor = primary.copy(alpha = 0.12f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = primary
                        )
                        Text(
                            text = "Restoring items…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))

        // ── Action buttons ────────────────────────────────────────────────
        when(restoreStatus) {
            RestoreStatus.INITIAL -> {
                Button(
                    onClick  = onRestore,
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.backup_restore),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Restore My Data",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
            RestoreStatus.FAIL -> {
                Button(
                    onClick = {
                        scope.launch {
                            statusText = "Restoring your data…"
                            subText    = "Please keep the app open. This may take a moment."
                            onRestore()
                        }
                    },
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Try Again",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick  = { /* skip — will navigate to SmsFetch once user taps */ },
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Skip for now", fontWeight = FontWeight.Medium)
                }
            }
            else -> { /* LOADING / SUCCESS — no button */ }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BackupRestoreScreenPreview() {
    CashLedgerTheme {
        BackupRestoreScreen(
            totalItemsRestored = 40f,
            totalItemsToRestore = 100f,
            restoreStatus = RestoreStatus.LOADING,
            onRestore = {}
        )
    }
}