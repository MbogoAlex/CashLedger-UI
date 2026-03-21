package com.records.pesa.ui.screens.profile

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatIsoDateTime2
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun AccountInformationScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    navigateToBackupScreen: () -> Unit,
    navigateToSubscriptionPaymentScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToHomeScreen)
    val context = LocalContext.current
    val viewModel: AccountInformationScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    var showEditFirstNameDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showEditLastNameDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showEditEmailDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var logoutLoading by rememberSaveable {
        mutableStateOf(false)
    }

    var showLogoutDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        showEditFirstNameDialog = false
        showEditLastNameDialog = false
        showEditFirstNameDialog = false
        showEditEmailDialog = false
        Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        showEditFirstNameDialog = false
        showEditLastNameDialog = false
        showEditFirstNameDialog = false
        showEditEmailDialog = false
        Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    if(showEditFirstNameDialog) {
        EditDialog(
            heading = "Edit your first name",
            label = "First name",
            value = uiState.firstName,
            onChangeValue = {
                viewModel.updateFirstName(it)
            },
            onConfirm = {
                viewModel.updateUserDetails()
            },
            onDismiss = {
                if(uiState.loadingStatus != LoadingStatus.LOADING) {
                    showEditFirstNameDialog = !showEditFirstNameDialog
                }
            },
            loadingStatus = uiState.loadingStatus
        )
    }

    if(showEditLastNameDialog) {
        EditDialog(
            heading = "Edit your surname",
            label = "Surname",
            value = uiState.lastName,
            onChangeValue = {
                viewModel.updateLastName(it)
            },
            onConfirm = {
                viewModel.updateUserDetails()
            },
            onDismiss = {
                if(uiState.loadingStatus != LoadingStatus.LOADING) {
                    showEditLastNameDialog = !showEditLastNameDialog
                }
            },
            loadingStatus = uiState.loadingStatus
        )
    }
    if(showEditEmailDialog) {
        EditDialog(
            heading = "Edit your email",
            label = "Email",
            value = uiState.email,
            onChangeValue = {
                viewModel.updateEmail(it)
            },
            onConfirm = {
                viewModel.updateUserDetails()
            },
            onDismiss = {
                if(uiState.loadingStatus != LoadingStatus.LOADING) {
                    showEditEmailDialog = !showEditEmailDialog
                }
            },
            loadingStatus = uiState.loadingStatus
        )
    }

    if(showLogoutDialog) {
        LogoutDialog(
            clearLoginDetails = uiState.clearLoginDetails,
            onClearLoginDetails = viewModel::updateClearLoginDetails,
            navigateToBackupScreen = navigateToBackupScreen,
            onConfirm = {
                logoutLoading = true
                val phoneNumber = uiState.userDetails!!.phoneNumber
                val password = uiState.userDetails!!.password
                showLogoutDialog = !showLogoutDialog
                scope.launch {
                    viewModel.logout()
                    delay(2000)
                    logoutLoading = !logoutLoading
                    Toast.makeText(context, "Logging out", Toast.LENGTH_SHORT).show()
                    if(uiState.clearLoginDetails) {
                        while(uiState.userDetails?.phoneNumber?.isNotEmpty() == true || uiState.userDetails?.password?.isNotEmpty() == true) {
                            delay(1000)
                        }
                        navigateToLoginScreen()
                    } else {
                        try {
                            navigateToLoginScreenWithArgs(phoneNumber, password)
                        } catch (e: Exception) {
                            Log.e("failedToLogout", e.toString())
                        }
                    }
                }
            },
            onDismiss = {
                showLogoutDialog = !showLogoutDialog
            }
        )
    }
    
    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        AccountInformationScreen(
            lastPaymentDate = uiState.preferences.paidAt,
            expiryDate = uiState.preferences.expiryDate,
            paid = uiState.preferences.expiryDate?.isAfter(uiState.preferences.paidAt) ?: false,
            permanent = uiState.preferences.permanent,
            onEditFirstName = {
                showEditFirstNameDialog = !showEditFirstNameDialog
            },
            onEditLastName = {
                showEditLastNameDialog = !showEditLastNameDialog
            },
            onEditEmail = {
                showEditEmailDialog = !showEditEmailDialog
            },
            firstName = uiState.userDetails?.firstName ?: "Enter first name",
            lastName = uiState.userDetails?.lastName ?: "Enter surname",
            email = uiState.userDetails?.email ?: "Enter email",
            phoneNumber = uiState.userDetails?.phoneNumber ?: "Enter phone number",
            logoutLoading = logoutLoading,
            onLogout = {
                showLogoutDialog = !showLogoutDialog
            },
            navigateToSubscriptionPaymentScreen = navigateToSubscriptionPaymentScreen
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = screenWidth(x = 32.0),
                vertical = screenHeight(x = 8.0)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = false,
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
            Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
            Text(
                text = "Account details",
                fontSize = screenFontSize(x = 14.0).sp
            )
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))

        Column(
            modifier = if(phoneNumber == "0888888888") modifier
                 else Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "First name",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            ElevatedCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(screenWidth(x = 20.0))
                        .fillMaxWidth()
                ) {
                    Text(
                        text = firstName,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                onEditFirstName()
                            }
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
            Text(
                text = "Surname",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .padding(screenWidth(x = 20.0))
                        .fillMaxWidth()
                ) {
                    Text(
                        text = lastName,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                onEditLastName()
                            }
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
            Text(
                text = "Email",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .padding(screenHeight(x = 20.0))
                        .fillMaxWidth()
                ) {
                    Text(
                        text = email,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                onEditEmail()
                            }
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
            Text(
                text = "Phone number",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .padding(screenWidth(x = 20.0))
                        .fillMaxWidth()
                ) {
                    Text(
                        text = phoneNumber,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        tint = Color.LightGray,
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            if(phoneNumber != "0888888888") {
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                Column {
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(screenWidth(x = 16.0))
                        ) {
                            Text(
                                text = "My subscription",
                                fontSize = screenFontSize(x = 18.0).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                            if(permanent) {
                                Text(
                                    text = "Lifetime, No expiry",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = screenFontSize(x = 14.0).sp
                                )
                                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                Text(text = "Last payment: ${if(lastPaymentDate != null) formatIsoDateTime2(lastPaymentDate) else "N/A"}")
                            } else {
                                if(expiryDate?.isBefore(LocalDateTime.now()) == true) {
                                    Text(
                                        text = "Expired",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = screenFontSize(x = 14.0).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                    Text(
                                        text = "Last payment: ${if(lastPaymentDate != null) formatIsoDateTime2(lastPaymentDate) else "N/A"}",
                                        fontSize = screenFontSize(x = 14.0).sp,
                                    )
                                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                    Text(
                                        text = "Expired on: ${if(expiryDate != null) formatIsoDateTime2(expiryDate) else "N/A"}",
                                        fontSize = screenFontSize(x = 14.0).sp,
                                    )
                                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                    Button(
                                        onClick = navigateToSubscriptionPaymentScreen,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Renew",
                                            fontSize = screenFontSize(x = 14.0).sp
                                        )
                                    }
                                } else if(expiryDate?.isAfter(LocalDateTime.now()) == true) {
                                    Text(
                                        text = "Active",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = screenFontSize(x = 14.0).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                    Text(
                                        text = "Last payment: ${if(lastPaymentDate != null) formatIsoDateTime2(lastPaymentDate) else "N/A"}",
                                        fontSize = screenFontSize(x = 14.0).sp,
                                    )
                                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                    Text(
                                        text = "Expires on: ${if(expiryDate != null) formatIsoDateTime2(expiryDate) else "N/A"}",
                                        fontSize = screenFontSize(x = 14.0).sp,
                                    )

                                } else {
                                    Text(
                                        text = "Limited access",
                                        fontSize = screenFontSize(x = 14.0).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                                    Button(
                                        onClick = navigateToSubscriptionPaymentScreen,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Upgrade",
                                            fontSize = screenFontSize(x = 14.0).sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))

            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                enabled = !logoutLoading,
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if(logoutLoading) {
                    Text(
                        text = "Logging out...",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Log out",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
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
    AlertDialog(
        title = {
            Text(
                text = heading,
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            OutlinedTextField(
                label = {
                    Text(
                        text = label,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                },
                value = value,
                onValueChange = onChangeValue,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onDismiss
            ) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(
                enabled = value.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
                onClick = onConfirm
            ) {
                if(loadingStatus == LoadingStatus.LOADING) {
                    Text(
                        text = "Saving...",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Save",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
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
    AlertDialog(
        title = {
            Text(
                text = "Logout",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to log out?",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp
                )

                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                if(clearLoginDetails) {
                    Text(
                        text = "Your login details will be cleared",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Your login details will be retained",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier
//                        .padding(screenWidth(x = 10.0))
                ) {
                    if(clearLoginDetails) {
                        IconButton(onClick = onClearLoginDetails) {
                            Icon(
                                painter = painterResource(id = R.drawable.check_box_filled),
                                contentDescription = "Clear login details: true",
                                modifier = Modifier
                                    .size(screenWidth(x = 24.0))
                            )
                        }
                    } else {
                        IconButton(onClick = onClearLoginDetails) {
                            Icon(
                                painter = painterResource(id = R.drawable.check_box_blank),
                                contentDescription = "Clear login details: false",
                                modifier = Modifier
                                    .size(screenWidth(x = 24.0))
                            )
                        }
                    }
//                    Spacer(modifier = Modifier.width(screenWidth(x = 4.0)))
                    Text(
                        text = "Clear your login details",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(text = "Log out")
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