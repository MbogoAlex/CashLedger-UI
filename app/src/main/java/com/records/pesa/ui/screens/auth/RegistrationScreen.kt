package com.records.pesa.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.records.pesa.R
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun RegistrationScreenComposable(
    modifier: Modifier = Modifier
) {

}

@Composable
fun RegistrationScreen(
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
    ) {
        Image(
            painter = painterResource(id = R.drawable.cashledger_logo),
            contentDescription = null
        )
        Text(
            text = "Register now to be able to analyze your M-PESA transactions",
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
            value = "",
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
            onValueChange = {},
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        PasswordInputField(
            heading = "Password",
            value = "",
            trailingIcon = R.drawable.visibility_on,
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Password
            ),
            visibility = passwordVisibility,
            onChangeVisibility = { passwordVisibility = !passwordVisibility }
        )
        Spacer(modifier = Modifier.height(20.dp))
        PasswordInputField(
            heading = "Confirm password",
            value = "",
            trailingIcon = R.drawable.visibility_on,
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Password
            ),
            visibility = passwordVisibility,
            onChangeVisibility = { passwordVisibility = !passwordVisibility }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Text(text = "Already registered? ")
            Text(
                text = "Sign in",
                color = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier
                    .clickable {  }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Register")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistrationScreenPreview() {
    CashLedgerTheme {
        RegistrationScreen()
    }
}