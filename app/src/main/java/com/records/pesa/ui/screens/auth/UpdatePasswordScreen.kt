package com.records.pesa.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
    var passwordVisibility by rememberSaveable { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scaleX = -1f, scaleY = 1f)
                    )
                }
                Text(
                    text = "Reset Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Hero gradient banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.15f),
                                tertiary.copy(alpha = 0.08f),
                                primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(top = 28.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.lock),
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Reset Password",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter your phone number and choose a new password",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    label = {
                        Text(text = "Safaricom phone number", fontSize = 14.sp)
                    },
                    value = phoneNumber,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.phone),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Phone
                    ),
                    onValueChange = { if (it.length <= 10) onChangePhoneNumber(it) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordInputField(
                    heading = "New Password",
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

                PasswordInputField(
                    heading = "Confirm New Password",
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

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    enabled = resetButtonEnabled && loadingStatus != LoadingStatus.LOADING,
                    onClick = onReset,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (loadingStatus == LoadingStatus.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Resetting…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(text = "Reset Password", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Remember your password? ", fontSize = 14.sp)
                    TextButton(onClick = navigateToLoginScreen) {
                        Text(
                            text = "Sign in",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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