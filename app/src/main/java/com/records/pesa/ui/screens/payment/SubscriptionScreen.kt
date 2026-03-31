package com.records.pesa.ui.screens.payment

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.subscription.SubscriptionPackageDt
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

object SubscriptionScreenDestination : AppNavigation {
    override val title: String = "Subscription screen"
    override val route: String = "subscription-screen"
}

@Composable
fun SubscriptionScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToSmsFetchScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SubscriptionScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkConnectivity(context)
    }

    var lipaStatusCheck by rememberSaveable { mutableStateOf(false) }
    var showSuccessDialogue by rememberSaveable { mutableStateOf(false) }

    if (showSuccessDialogue) {
        PaymentSuccessDialogue(
            paymentPlan = when (uiState.amount) {
                "100" -> "Monthly"
                "400" -> "6 Months"
                "700" -> "12 Months"
                "2000" -> "Lifetime"
                else -> "Monthly"
            },
            onConfirm = {
                showSuccessDialogue = !showSuccessDialogue
                navigateToSmsFetchScreen()
            },
            onDismiss = {
                showSuccessDialogue = !showSuccessDialogue
                navigateToSmsFetchScreen()
            }
        )
    }

    LaunchedEffect(uiState.loadingStatus) {
        if (uiState.loadingStatus == LoadingStatus.SUCCESS) {
            Toast.makeText(context, uiState.paymentMessage, Toast.LENGTH_SHORT).show()
            lipaStatusCheck = false
            showSuccessDialogue = true
            viewModel.resetPaymentStatus()
        } else if (uiState.loadingStatus == LoadingStatus.FAIL) {
            Toast.makeText(context, uiState.failedReason, Toast.LENGTH_SHORT).show()
            lipaStatusCheck = false
            viewModel.resetPaymentStatus()
        }
    }

    var showPaymentScreen by rememberSaveable { mutableStateOf(false) }

    val isConnected by viewModel.isConnected.observeAsState(false)

    BackHandler(onBack = {
        if (uiState.loadingStatus != LoadingStatus.LOADING) {
            if (showPaymentScreen) {
                showPaymentScreen = !showPaymentScreen
            } else {
                navigateToHomeScreen()
            }
            viewModel.cancel()
        } else {
            Toast.makeText(context, "Payment in progress", Toast.LENGTH_SHORT).show()
        }
    })

    Box(modifier = Modifier.statusBarsPadding()) {
        if (showPaymentScreen) {
            PaymentScreen(
                firstTransactionDate = uiState.firstTransactionDate,
                amount = uiState.amount,
                isConnected = isConnected,
                status = uiState.state,
                phoneNumber = uiState.phoneNumber,
                onAmountChange = { viewModel.updateAmount(it) },
                onPhoneNumberChange = { viewModel.updatePhoneNumber(it) },
                buttonEnabled = uiState.phoneNumber.isNotEmpty(),
                onPay = { viewModel.lipa() },
                navigateToPreviousScreen = {
                    if (uiState.loadingStatus != LoadingStatus.LOADING) {
                        if (showPaymentScreen) {
                            showPaymentScreen = !showPaymentScreen
                            viewModel.cancel()
                        } else {
                            navigateToPreviousScreen()
                        }
                    } else {
                        Toast.makeText(context, "Payment in progress", Toast.LENGTH_SHORT).show()
                    }
                },
                lipaStatus = { viewModel.lipaStatus() },
                onCheckSubscriptionStatus = { viewModel.checkSubscriptionStatus() },
                onChangeLipaStatusCheck = { lipaStatusCheck = !lipaStatusCheck },
                lipaStatusCheck = lipaStatusCheck,
                failedReason = uiState.failedReason ?: "",
                lipaSave = viewModel::lipa,
                packageId = uiState.selectedPackageId,
                loadingStatus = uiState.loadingStatus,
            )
        } else {
            SubscriptionScreen(
                firstTransactionDate = uiState.firstTransactionDate,
                subscriptionPackages = uiState.subscriptionPackages,
                fetchingStatus = uiState.fetchingStatus,
                navigateToPreviousScreen = navigateToPreviousScreen,
                navigateToPaymentScreen = {
                    viewModel.updatePackageId(it.toInt())
                    showPaymentScreen = true
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Subscription Screen (plan picker)
// ---------------------------------------------------------------------------

@Composable
fun SubscriptionScreen(
    firstTransactionDate: String,
    subscriptionPackages: List<SubscriptionPackageDt>,
    fetchingStatus: LoadingStatus,
    navigateToPaymentScreen: (packageId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = 180f },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Upgrade to Pro",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // ── Compact hero ─────────────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.15f),
                                tertiary.copy(alpha = 0.06f),
                                primary.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color = primary.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.star),
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Mpesa Ledger Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                            if (firstTransactionDate.isNotEmpty()) {
                                Text(
                                    text = "Tracking since $firstTransactionDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Features in a 2-col mini grid
                    val features = listOf(
                        "Full history — no 30-day cap",
                        "AI insights & projections",
                        "Unusual activity alerts",
                        "Backup & restore data",
                        "Unlimited categories",
                        "Dark mode support",
                        "PDF & CSV reports"
                    )
                    features.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { feature ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.check_box_filled),
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = feature,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Plans header ─────────────────────────────────────────────────────
        Text(
            text = "Choose Your Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── Plans grid ───────────────────────────────────────────────────────
        when {
            fetchingStatus == LoadingStatus.LOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = primary, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        Text("Loading plans…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            subscriptionPackages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Could not load plans. Check your connection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                // Render as a 2-column grid using chunked rows
                subscriptionPackages.chunked(2).forEach { rowPkgs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPkgs.forEach { pkg ->
                            PlanCard(
                                subscriptionPackage = pkg,
                                onSelect = { navigateToPaymentScreen(pkg.id.toString()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill the remaining cell if odd number of packages
                        if (rowPkgs.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PlanCard(
    subscriptionPackage: SubscriptionPackageDt,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val planType = when (subscriptionPackage.amount) {
        100.0 -> "Monthly"
        400.0 -> "6 Months"
        700.0 -> "Annual"
        2000.0 -> "Lifetime"
        else -> "Monthly"
    }
    val planName = when (planType) {
        "Monthly" -> "Monthly"
        "6 Months" -> "6 Months"
        "Annual" -> "Annual"
        "Lifetime" -> "Lifetime"
        else -> "Monthly"
    }
    val priceText = when (planType) {
        "Monthly" -> "Ksh 100"
        "6 Months" -> "Ksh 400"
        "Annual" -> "Ksh 700"
        "Lifetime" -> "Ksh 2,000"
        else -> "Ksh 100"
    }
    val periodText = when (planType) {
        "Monthly" -> "/ month"
        "6 Months" -> "/ 6 months"
        "Annual" -> "/ year"
        "Lifetime" -> "one-time"
        else -> "/ month"
    }
    val savingsText = when (planType) {
        "6 Months" -> "Save Ksh 200"
        "Annual" -> "Save Ksh 500"
        "Lifetime" -> "Best value"
        else -> ""
    }
    val badge: String? = when (planType) {
        "6 Months" -> "BEST VALUE"
        "Annual" -> "POPULAR"
        "Lifetime" -> "LIFETIME"
        else -> null
    }

    ElevatedCard(
        modifier = modifier.clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.12f),
                            tertiary.copy(alpha = 0.06f),
                            primary.copy(alpha = 0.04f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primary
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (savingsText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = savingsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text(
                        text = if (planType == "Lifetime") "Buy" else "Subscribe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Payment Screen
// ---------------------------------------------------------------------------

@Composable
fun PaymentScreen(
    failedReason: String,
    firstTransactionDate: String,
    amount: String,
    packageId: Int?,
    isConnected: Boolean,
    status: String,
    phoneNumber: String,
    onAmountChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    buttonEnabled: Boolean,
    lipaStatusCheck: Boolean,
    onChangeLipaStatusCheck: () -> Unit,
    lipaStatus: () -> Unit,
    onCheckSubscriptionStatus: () -> Unit,
    lipaSave: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    onPay: () -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    var understood by rememberSaveable { mutableStateOf(false) }
    var payButtonClicked by rememberSaveable { mutableStateOf(false) }
    var showCheckingStatus by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = navigateToPreviousScreen)

    val planTitle = when (amount) {
        "100" -> "Monthly Plan"
        "400" -> "6-Month Plan"
        "700" -> "Annual Plan"
        "2000" -> "Lifetime Access"
        else -> "Subscription"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = 180f },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = planTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Form card ─────────────────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 0.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = 0.12f),
                                    tertiary.copy(alpha = 0.06f),
                                    primary.copy(alpha = 0.04f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Amount display
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = primary.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "Ksh $amount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                            )
                        }

                        // Payment method
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primary.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "M-Pesa",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // Phone number
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = onPhoneNumberChange,
                            label = { Text("M-Pesa Phone Number") },
                            placeholder = { Text("e.g. 2547XXXXXXXX") },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Tracking since info
                        if (firstTransactionDate.isNotEmpty()) {
                            HorizontalDivider(color = primary.copy(alpha = 0.12f))
                            Text(
                                text = "Transactions tracked since: $firstTransactionDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // "I understand" checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                enabled = loadingStatus != LoadingStatus.LOADING,
                                onClick = { understood = !understood },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (understood) R.drawable.check_box_filled
                                        else R.drawable.check_box_blank
                                    ),
                                    contentDescription = "I understand",
                                    tint = if (understood) primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (firstTransactionDate.isNotEmpty())
                                    "I understand that subscribing will unlock my full transaction history since $firstTransactionDate"
                                else
                                    "I understand that subscribing will unlock my full transaction history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // No internet warning
            if (!isConnected) {
                Text(
                    text = "Connect to the internet to make a payment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Payment pending hint
            if (payButtonClicked && loadingStatus != LoadingStatus.LOADING) {
                Text(
                    text = "Payment request will appear in a few seconds…",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Check Payment Status button
            if (status.isNotEmpty() &&
                status.lowercase() !in listOf("completed", "failed", "cancelled") &&
                loadingStatus != LoadingStatus.FAIL
            ) {
                OutlinedButton(
                    onClick = {
                        showCheckingStatus = true
                        onCheckSubscriptionStatus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = loadingStatus != LoadingStatus.LOADING && isConnected,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Check Payment Status",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Pay button
            Button(
                enabled = buttonEnabled && loadingStatus != LoadingStatus.LOADING && isConnected && understood,
                onClick = {
                    payButtonClicked = true
                    if (failedReason == "Failed to save payment") {
                        lipaSave()
                    } else {
                        onPay()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Text(
                    text = when {
                        loadingStatus == LoadingStatus.LOADING -> "$status…"
                        loadingStatus == LoadingStatus.FAIL && failedReason == "Failed to save payment" ->
                            "Click to save payment"
                        amount == "2000" -> "Get Lifetime Access"
                        else -> "Pay Now"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Support card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Need help?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If you experience any payment issues, our support team is here to assist you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("hubkiwitech@gmail.com"))
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Payment Support Request - Mpesa Ledger"
                                )
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hello Support Team,\n\n" +
                                        "I am experiencing issues with my payment for the ${
                                            when (amount) {
                                                "100" -> "Monthly"
                                                "400" -> "6 Months"
                                                "700" -> "Annual"
                                                "2000" -> "Lifetime"
                                                else -> "subscription"
                                            }
                                        } plan (Ksh $amount).\n\n" +
                                        "Issue Details:\n" +
                                        "Phone Number: $phoneNumber\n" +
                                        "Transaction Status: $status\n" +
                                        "${if (failedReason.isNotEmpty()) "Error: $failedReason\n" else ""}" +
                                        "\nPlease assist me to resolve this issue.\n\n" +
                                        "Thank you."
                                )
                            }
                            try {
                                context.startActivity(emailIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.contact),
                            contentDescription = "Contact Support",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contact Support",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Loading overlay
        if (loadingStatus == LoadingStatus.LOADING) {
            PaymentLoadingOverlay(
                isCheckingStatus = showCheckingStatus,
                onComplete = { showCheckingStatus = false }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Payment loading overlay  (unchanged — keep as-is)
// ---------------------------------------------------------------------------

@Composable
private fun PaymentLoadingOverlay(
    isCheckingStatus: Boolean,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "payment_loading")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(isCheckingStatus) {
        if (!isCheckingStatus) {
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            elevation = CardDefaults.cardElevation(defaultElevation = screenHeight(12.0)),
            shape = RoundedCornerShape(screenWidth(20.0)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(screenWidth(24.0)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(screenHeight(24.0))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier
                            .size(screenWidth(80.0))
                            .rotate(rotationAngle),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        strokeWidth = screenWidth(3.0)
                    )
                    CircularProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier
                            .size(screenWidth(60.0))
                            .rotate(-rotationAngle * 0.8f),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        strokeWidth = screenWidth(4.0)
                    )
                    Box(
                        modifier = Modifier
                            .size(screenWidth(32.0))
                            .scale(pulseScale)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(screenHeight(8.0))
                ) {
                    Text(
                        text = if (isCheckingStatus) "Checking Payment Status" else "Processing Payment",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCheckingStatus)
                            "Verifying payment completion..."
                        else
                            "Sending payment request to M-Pesa...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Payment success dialogue  (unchanged — keep as-is)
// ---------------------------------------------------------------------------

@Composable
fun PaymentSuccessDialogue(
    paymentPlan: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Payment Successful",
                fontSize = screenFontSize(x = 16.0).sp
            )
        },
        text = {
            Text(
                text = "You have successfully subscribed to $paymentPlan",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Exit",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SubscriptionScreenPreview() {
    CashLedgerTheme {
        SubscriptionScreen(
            firstTransactionDate = formatLocalDate(LocalDate.now().minusMonths(6)),
            fetchingStatus = LoadingStatus.INITIAL,
            navigateToPaymentScreen = {},
            navigateToPreviousScreen = {},
            subscriptionPackages = listOf(
                SubscriptionPackageDt(
                    id = 1, title = "Monthly", description = null, amount = 100.0,
                    createdAt = "", updatedAt = "", subscriptionContainerId = 1,
                    subscriptionContainerTitle = "", payments = 0
                ),
                SubscriptionPackageDt(
                    id = 2, title = "6 Months", description = null, amount = 400.0,
                    createdAt = "", updatedAt = "", subscriptionContainerId = 1,
                    subscriptionContainerTitle = "", payments = 0
                ),
                SubscriptionPackageDt(
                    id = 3, title = "Annual", description = null, amount = 700.0,
                    createdAt = "", updatedAt = "", subscriptionContainerId = 1,
                    subscriptionContainerTitle = "", payments = 0
                ),
                SubscriptionPackageDt(
                    id = 4, title = "Lifetime", description = null, amount = 2000.0,
                    createdAt = "", updatedAt = "", subscriptionContainerId = 1,
                    subscriptionContainerTitle = "", payments = 0
                )
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaymentScreenPreview() {
    CashLedgerTheme {
        PaymentScreen(
            firstTransactionDate = formatLocalDate(LocalDate.now().minusMonths(6)),
            isConnected = true,
            status = "",
            phoneNumber = "254794649026",
            onAmountChange = {},
            onPhoneNumberChange = {},
            buttonEnabled = true,
            lipaStatus = {},
            onCheckSubscriptionStatus = {},
            onChangeLipaStatusCheck = {},
            lipaStatusCheck = false,
            navigateToPreviousScreen = {},
            amount = "700",
            failedReason = "",
            onPay = {},
            lipaSave = {},
            loadingStatus = LoadingStatus.INITIAL,
            packageId = 3
        )
    }
}
