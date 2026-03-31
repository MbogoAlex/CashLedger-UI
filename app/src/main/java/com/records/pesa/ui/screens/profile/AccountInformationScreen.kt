package com.records.pesa.ui.screens.profile

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatIsoDateTime2
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme
import com.records.pesa.ui.screens.components.SubscriptionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun AccountInformationScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    navigateToBackupScreen: () -> Unit,
    navigateToSubscriptionPaymentScreen: () -> Unit,
    onSwitchTheme: () -> Unit = {},
    darkMode: Boolean = false,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToHomeScreen)
    val context = LocalContext.current
    val viewModel: AccountInformationScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    var showEditFirstNameDialog by rememberSaveable { mutableStateOf(false) }
    var showEditLastNameDialog  by rememberSaveable { mutableStateOf(false) }
    var showEditEmailDialog     by rememberSaveable { mutableStateOf(false) }
    var logoutLoading           by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog        by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionDialog  by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val isPremiumFinal = isPremium ||
        uiState.preferences.permanent ||
        (uiState.preferences.expiryDate?.isAfter(LocalDateTime.now()) == true)

    if (uiState.loadingStatus == LoadingStatus.SUCCESS) {
        showEditFirstNameDialog = false
        showEditLastNameDialog  = false
        showEditEmailDialog     = false
        Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    } else if (uiState.loadingStatus == LoadingStatus.FAIL) {
        showEditFirstNameDialog = false
        showEditLastNameDialog  = false
        showEditEmailDialog     = false
        Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    if (showEditFirstNameDialog) {
        EditDialog(
            heading = "First name",
            label = "First name",
            value = uiState.firstName,
            onChangeValue = { viewModel.updateFirstName(it) },
            onConfirm = { viewModel.updateUserDetails() },
            onDismiss = { if (uiState.loadingStatus != LoadingStatus.LOADING) showEditFirstNameDialog = false },
            loadingStatus = uiState.loadingStatus
        )
    }

    if (showEditLastNameDialog) {
        EditDialog(
            heading = "Last name",
            label = "Last name",
            value = uiState.lastName,
            onChangeValue = { viewModel.updateLastName(it) },
            onConfirm = { viewModel.updateUserDetails() },
            onDismiss = { if (uiState.loadingStatus != LoadingStatus.LOADING) showEditLastNameDialog = false },
            loadingStatus = uiState.loadingStatus
        )
    }

    if (showEditEmailDialog) {
        EditDialog(
            heading = "Email address",
            label = "Email",
            value = uiState.email,
            onChangeValue = { viewModel.updateEmail(it) },
            onConfirm = { viewModel.updateUserDetails() },
            onDismiss = { if (uiState.loadingStatus != LoadingStatus.LOADING) showEditEmailDialog = false },
            loadingStatus = uiState.loadingStatus
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(
            clearLoginDetails = uiState.clearLoginDetails,
            onClearLoginDetails = viewModel::updateClearLoginDetails,
            navigateToBackupScreen = navigateToBackupScreen,
            onConfirm = {
                logoutLoading = true
                val phoneNumber = uiState.userDetails!!.phoneNumber
                val password    = uiState.userDetails!!.password
                showLogoutDialog = false
                scope.launch {
                    viewModel.logout()
                    delay(2000)
                    logoutLoading = false
                    Toast.makeText(context, "Logging out", Toast.LENGTH_SHORT).show()
                    if (uiState.clearLoginDetails) {
                        while (uiState.userDetails?.phoneNumber?.isNotEmpty() == true ||
                               uiState.userDetails?.password?.isNotEmpty() == true) { delay(1000) }
                        navigateToLoginScreen()
                    } else {
                        try { navigateToLoginScreenWithArgs(phoneNumber, password) }
                        catch (e: Exception) { Log.e("failedToLogout", e.toString()) }
                    }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = {
                showSubscriptionDialog = false
                navigateToSubscriptionPaymentScreen()
            }
        )
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        AccountInformationScreen(
            lastPaymentDate = uiState.preferences.paidAt,
            expiryDate = uiState.preferences.expiryDate,
            paid = uiState.preferences.expiryDate?.isAfter(uiState.preferences.paidAt) ?: false,
            permanent = uiState.preferences.permanent,
            onEditFirstName = { showEditFirstNameDialog = true },
            onEditLastName  = { showEditLastNameDialog = true },
            onEditEmail     = { showEditEmailDialog = true },
            firstName   = uiState.userDetails?.firstName ?: "",
            lastName    = uiState.userDetails?.lastName ?: "",
            email       = uiState.userDetails?.email ?: "",
            phoneNumber = uiState.userDetails?.phoneNumber ?: "",
            logoutLoading = logoutLoading,
            onLogout = { showLogoutDialog = true },
            navigateToSubscriptionPaymentScreen = navigateToSubscriptionPaymentScreen,
            onSwitchTheme = {
                if (isPremiumFinal) {
                    onSwitchTheme()
                } else {
                    showSubscriptionDialog = true
                }
            },
            darkMode = darkMode,
            isPremium = isPremiumFinal,
        )
    }
}


@Composable
fun AccountInformationScreen(
    lastPaymentDate: LocalDateTime?,
    expiryDate: LocalDateTime?,
    permanent: Boolean,
    paid: Boolean,
    onEditFirstName: () -> Unit,
    onEditLastName: () -> Unit,
    onEditEmail: () -> Unit,
    firstName: String,
    lastName: String,
    email: String,
    phoneNumber: String,
    onLogout: () -> Unit,
    logoutLoading: Boolean,
    navigateToSubscriptionPaymentScreen: () -> Unit,
    onSwitchTheme: () -> Unit = {},
    darkMode: Boolean = false,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isAdmin = phoneNumber == "0888888888"
    val initials = buildString {
        firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
    }.ifEmpty { "?" }

    val subStatus = when {
        permanent -> "Lifetime"
        expiryDate?.isAfter(LocalDateTime.now()) == true -> "Active"
        expiryDate?.isBefore(LocalDateTime.now()) == true -> "Expired"
        else -> "Free"
    }
    val subColor = when (subStatus) {
        "Lifetime" -> MaterialTheme.colorScheme.primary
        "Active"   -> Color(0xFF2E7D32)
        "Expired"  -> MaterialTheme.colorScheme.error
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {

        // ── Hero header ─────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "$firstName $lastName".trim().ifEmpty { "—" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Subscription badge
                    if (!isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = subColor.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = subStatus,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = subColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Dark mode quick toggle in header
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable(onClick = onSwitchTheme)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (darkMode) R.drawable.nightlight else R.drawable.lightmode),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (darkMode) "Dark mode" else "Light mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isPremium) {
                                Icon(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Personal information ─────────────────────────────────────────────
        item {
            SectionHeader(title = "Personal information")
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                InfoRow(label = "First name", value = firstName, onEdit = onEditFirstName)
                RowDivider()
                InfoRow(label = "Last name", value = lastName, onEdit = onEditLastName)
                RowDivider()
                InfoRow(label = "Email", value = email.ifEmpty { "Not set" }, onEdit = onEditEmail)
                RowDivider()
                InfoRow(label = "Phone number", value = phoneNumber, onEdit = null)
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Subscription ─────────────────────────────────────────────────────
        if (!isAdmin) {
            item { SectionHeader(title = "Subscription") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.star),
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when (subStatus) {
                                    "Lifetime" -> "Lifetime Premium"
                                    "Active"   -> "Premium — Active"
                                    "Expired"  -> "Subscription Expired"
                                    else       -> "Free Plan"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = subColor
                            )
                        }
                        if (lastPaymentDate != null) {
                            Text(
                                text = "Last payment: ${formatIsoDateTime2(lastPaymentDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        when (subStatus) {
                            "Active" -> {
                                val daysLeft = if (expiryDate != null)
                                    ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate) else null
                                Text(
                                    text = "Expires: ${if (expiryDate != null) formatIsoDateTime2(expiryDate) else "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (daysLeft != null) {
                                    val daysColor = when {
                                        daysLeft <= 3  -> MaterialTheme.colorScheme.error
                                        daysLeft <= 7  -> Color(0xFFF57F17) // amber
                                        else           -> Color(0xFF2E7D32) // green
                                    }
                                    Text(
                                        text = when {
                                            daysLeft <= 0L -> "Expires today!"
                                            daysLeft == 1L -> "1 day remaining"
                                            else           -> "$daysLeft days remaining"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = daysColor
                                    )
                                }
                            }
                            "Expired" -> {
                                Text(
                                    text = "Expired: ${if (expiryDate != null) formatIsoDateTime2(expiryDate) else "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = navigateToSubscriptionPaymentScreen,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Renew Subscription", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            "Free" -> {
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = navigateToSubscriptionPaymentScreen,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Upgrade to Premium", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Appearance ───────────────────────────────────────────────────────
        item { SectionHeader(title = "Appearance") }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchTheme() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.nightlight),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isPremium) {
                            Text(
                                text = "Premium feature",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isPremium) {
                        Switch(checked = darkMode, onCheckedChange = { onSwitchTheme() })
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.lock),
                            contentDescription = "Premium feature",
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        // ── Logout ───────────────────────────────────────────────────────────
        item {
            OutlinedButton(
                onClick = onLogout,
                enabled = !logoutLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (logoutLoading) 0.4f else 1f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                if (logoutLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Logging out...")
                } else {
                    Text("Log out", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun InfoRow(label: String, value: String, onEdit: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onEdit != null) {
            Icon(
                painter = painterResource(R.drawable.edit),
                contentDescription = "Edit $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
fun EditDialog(
    heading: String,
    label: String,
    value: String,
    onChangeValue: (value: String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon header band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            )
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Edit $heading",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                OutlinedTextField(
                    label = { Text(text = label) },
                    value = value,
                    onValueChange = onChangeValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    enabled = value.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loadingStatus == LoadingStatus.LOADING) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save changes", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(
                    enabled = loadingStatus != LoadingStatus.LOADING,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
fun LogoutDialog(
    clearLoginDetails: Boolean,
    onClearLoginDetails: () -> Unit,
    navigateToBackupScreen: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorColor = MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon header band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(errorColor, errorColor.copy(alpha = 0.7f))
                            )
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.logout),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "Are you sure you want to log out?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // Remember me toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearLoginDetails() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (!clearLoginDetails) R.drawable.check_box_filled else R.drawable.check_box_blank
                            ),
                            contentDescription = null,
                            tint = if (!clearLoginDetails) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Remember me?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (!clearLoginDetails) "Your credentials will be saved"
                                       else "You will need to sign in again",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) {
                    Text("Yes, log out", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AccountInformationScreenPreview() {
    CashLedgerTheme {
        AccountInformationScreen(
            lastPaymentDate = null,
            expiryDate = null,
            permanent = false,
            paid = false,
            onEditFirstName = { /*TODO*/ },
            onEditLastName = { /*TODO*/ },
            onEditEmail = { /*TODO*/ },
            firstName = "",
            lastName = "",
            phoneNumber = "0888888888",
            email = "",
            logoutLoading = false,
            onLogout = {},
            navigateToSubscriptionPaymentScreen = {}
        )
    }
}