package com.records.pesa.ui.screens.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccountInformationScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
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
    
    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        AccountInformationScreen(
            onEditFirstName = {
                showEditFirstNameDialog = !showEditFirstNameDialog
            },
            onEditLastName = {
                showEditLastNameDialog = !showEditLastNameDialog
            },
            onEditEmail = {
                showEditEmailDialog = !showEditEmailDialog
            },
            firstName = uiState.userDetails.firstName ?: "Enter first name",
            lastName = uiState.userDetails.lastName ?: "Enter last name",
            email = uiState.userDetails.email ?: "",
            phoneNumber = uiState.userDetails.phoneNumber,
            logoutLoading = logoutLoading,
            onLogout = {
                logoutLoading = true
                val phoneNumber = uiState.userDetails.phoneNumber
                val password = uiState.userDetails.password
                scope.launch {
                    viewModel.logout()
                    delay(2000)
                    logoutLoading = !logoutLoading
                    Toast.makeText(context, "Logging out", Toast.LENGTH_SHORT).show()
                    navigateToLoginScreenWithArgs(phoneNumber, password)
                }
            }
        )
    }
}

@Composable
fun AccountInformationScreen(
    onEditFirstName: () -> Unit,
    onEditLastName: () -> Unit,
    onEditEmail: () -> Unit,
    firstName: String,
    lastName: String,
    email: String,
    phoneNumber: String,
    onLogout: () -> Unit,
    logoutLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 32.dp,
                vertical = 8.dp
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
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "Account details")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "First name")
        Spacer(modifier = Modifier.height(10.dp))
        ElevatedCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = firstName)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            onEditFirstName()
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Surname")
        Spacer(modifier = Modifier.height(10.dp))
        ElevatedCard {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = lastName)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            onEditLastName()
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Email")
        Spacer(modifier = Modifier.height(10.dp))
        ElevatedCard {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = email)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            onEditEmail()
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Phone number")
        Spacer(modifier = Modifier.height(10.dp))
        ElevatedCard {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "0794649026")
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    tint = Color.LightGray,
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            enabled = !logoutLoading,
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(logoutLoading) {
                Text(text = "Logging out...")
            } else {
                Text(text = "Log out")
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
            Text(text = heading)
        },
        text = {
            OutlinedTextField(
                label = {
                    Text(text = label)
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
                Text(text = "Dismiss")
            }
        },
        confirmButton = {
            Button(
                enabled = value.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
                onClick = onConfirm
            ) {
                if(loadingStatus == LoadingStatus.LOADING) {
                    Text(text = "Saving...")
                } else {
                    Text(text = "Save")
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
            onEditFirstName = { /*TODO*/ },
            onEditLastName = { /*TODO*/ },
            onEditEmail = { /*TODO*/ },
            firstName = "",
            lastName = "",
            phoneNumber = "",
            email = "",
            logoutLoading = false,
            onLogout = {}
        )
    }
}