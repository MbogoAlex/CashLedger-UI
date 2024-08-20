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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme

object RegistrationScreenDestination: AppNavigation {
    override val title: String = "Registration screen"
    override val route: String = "registration-screen"
}

@Composable
fun RegistrationScreenComposable(
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    BackHandler(onBack = {activity?.finish()})

    val viewModel: RegistrationScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.registrationStatus == RegistrationStatus.SUCCESS) {
        Toast.makeText(context, uiState.registrationMessage, Toast.LENGTH_SHORT).show()
        navigateToLoginScreenWithArgs(uiState.phoneNumber, uiState.password)
        viewModel.resetRegistrationStatus()
    } else if(uiState.registrationStatus == RegistrationStatus.FAIL) {
        Toast.makeText(context, uiState.registrationMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetRegistrationStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        RegistrationScreen(
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
            registerButtonEnabled = uiState.registerButtonEnabled,
            onRegister = {
                if(!uiState.phoneNumber.startsWith("0")) {
                    Toast.makeText(context, "Phone number must start with '0'", Toast.LENGTH_SHORT).show()
                } else if(uiState.password.length < 5) {
                    Toast.makeText(context, "Password must be at least 5 characters", Toast.LENGTH_SHORT).show()
                } else if(uiState.password != uiState.passwordConfirmation) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.registerUser()
                }
            },
            registrationStatus = uiState.registrationStatus,
            navigateToLoginScreen = navigateToLoginScreen
        )
    }

}

@Composable
fun RegistrationScreen(
    phoneNumber: String,
    onChangePhoneNumber: (value: String) -> Unit,
    password: String,
    onChangePassword: (value: String) -> Unit,
    passwordConfirmation: String,
    onChangePasswordConfirmation: (value: String) -> Unit,
    registerButtonEnabled: Boolean,
    onRegister: () -> Unit,
    registrationStatus: RegistrationStatus,
    navigateToLoginScreen: () -> Unit,
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
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 16.0)
            )
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = R.drawable.cashledger_logo),
            contentDescription = null,
            modifier = Modifier.aspectRatio(16f / 9f)
        )
        Text(
            text = "Register now to be able to analyze your M-PESA transactions",
            fontWeight = FontWeight.Bold,
            fontSize = screenFontSize(x = 14.0).sp,
            modifier = Modifier
                .align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        TextField(
            label = {
                Text(
                    text = "Safaricom phone number",
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.scrim,
                )
            },
            value = phoneNumber,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phone),
                    contentDescription = null,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
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
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        PasswordInputField(
            heading = "Password",
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
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        PasswordInputField(
            heading = "Confirm password",
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
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Row {
            Text(text = "Already registered? ")
            Text(
                text = "Sign in",
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier
                    .clickable {
                        navigateToLoginScreen()
                    }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            enabled = registerButtonEnabled && registrationStatus != RegistrationStatus.LOADING,
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(registrationStatus == RegistrationStatus.LOADING) {
                Text(
                    text = "Loading...",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            } else {
                Text(
                    text = "Register",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistrationScreenPreview() {
    CashLedgerTheme {
        RegistrationScreen(
            phoneNumber = "",
            onChangePhoneNumber = {},
            password = "",
            onChangePassword = {},
            passwordConfirmation = "",
            onChangePasswordConfirmation = {},
            registerButtonEnabled = false,
            onRegister = { /*TODO*/ },
            registrationStatus = RegistrationStatus.INITIAL,
            navigateToLoginScreen = { /*TODO*/ })
    }
}