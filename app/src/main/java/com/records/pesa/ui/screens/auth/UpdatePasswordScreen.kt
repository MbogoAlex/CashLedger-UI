package com.records.pesa.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme

object UpdatePasswordScreenDestination: AppNavigation {
    override val title: String = "Update password screen"
    override val route: String = "update-password-screen"

}

@Composable
fun UpdatePasswordScreenComposable(
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToPreviousScreen)
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current

    val viewModel: UpdatePasswordScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, uiState.resetMessage, Toast.LENGTH_SHORT).show()
        navigateToLoginScreenWithArgs(uiState.phoneNumber, uiState.password)
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, uiState.resetMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        UpdatePasswordScreen(
            phoneNumber = uiState.phoneNumber,
            onChangePhoneNumber = {
                viewModel.updatePhoneNumber(it)
                viewModel.buttonEnabled()
            },
            password = uiState.password,
            onChangePassword = {
                viewModel.updatePassword(it)
                viewModel.buttonEnabled()
            },
            passwordConfirmation = uiState.passwordConfirmation,
            onChangePasswordConfirmation = {
                viewModel.updatePasswordConfirmation(it)
                viewModel.buttonEnabled()
            },
            resetButtonEnabled = uiState.resetButtonEnabled,
            onReset = {
                if(!uiState.phoneNumber.startsWith("0")) {
                    Toast.makeText(context, "Phone number must start with '0'", Toast.LENGTH_SHORT).show()
                } else if(uiState.password.length < 5) {
                    Toast.makeText(context, "Password must be at least 5 characters", Toast.LENGTH_SHORT).show()
                } else if(uiState.password != uiState.passwordConfirmation) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.resetPassword()
                }
            },
            loadingStatus = uiState.loadingStatus,
            navigateToLoginScreen = navigateToLoginScreen,
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }

}

@Composable
fun UpdatePasswordScreen(
    phoneNumber: String,
    onChangePhoneNumber: (value: String) -> Unit,
    password: String,
    onChangePassword: (value: String) -> Unit,
    passwordConfirmation: String,
    onChangePasswordConfirmation: (value: String) -> Unit,
    resetButtonEnabled: Boolean,
    onReset: () -> Unit,
    loadingStatus: LoadingStatus,
    navigateToLoginScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisibility by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
//        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            )
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(
            onClick = navigateToPreviousScreen,
            modifier = Modifier
                .align(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous screen"
            )
        }
        Image(
            painter = painterResource(id = R.drawable.cashledger_logo),
            contentDescription = null
        )
        Text(
            text = "Enter your correct phone number and new password to reset old password",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = {
                Text(
                    text = "Safaricom phone number",
                    color = MaterialTheme.colorScheme.scrim,
                )
            },
            value = phoneNumber,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phone),
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Phone
            ),
            onValueChange = onChangePhoneNumber,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        PasswordInputField(
            heading = "New password",
            value = password,
            trailingIcon = R.drawable.visibility_on,
            onValueChange = onChangePassword,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Password
            ),
            visibility = passwordVisibility,
            onChangeVisibility = { passwordVisibility = !passwordVisibility }
        )
        Spacer(modifier = Modifier.height(20.dp))
        PasswordInputField(
            heading = "Confirm new password",
            value = passwordConfirmation,
            trailingIcon = R.drawable.visibility_on,
            onValueChange = onChangePasswordConfirmation,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Password
            ),
            visibility = passwordVisibility,
            onChangeVisibility = { passwordVisibility = !passwordVisibility }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Text(text = "Remember password? ")
            Text(
                text = "Sign in",
                color = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier
                    .clickable {
                        navigateToLoginScreen()
                    }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            enabled = resetButtonEnabled && loadingStatus != LoadingStatus.LOADING,
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(loadingStatus == LoadingStatus.LOADING) {
                Text(text = "Loading...")
            } else {
                Text(text = "Reset password")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun UpdatePasswordScreenPreview() {
    CashLedgerTheme {
        UpdatePasswordScreen(
            phoneNumber = "",
            onChangePhoneNumber = {},
            password = "",
            onChangePassword = {},
            passwordConfirmation = "",
            onChangePasswordConfirmation = {},
            resetButtonEnabled = false,
            onReset = { /*TODO*/ },
            loadingStatus = LoadingStatus.INITIAL,
            navigateToLoginScreen = { /*TODO*/ },
            navigateToPreviousScreen = {}
        )
    }
}