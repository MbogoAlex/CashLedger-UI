package com.records.pesa.ui.screens.contact

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun ContactFormScreenComposable(
    navigateToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToHomeScreen)
    // Variables for email subject and body
    var emailSubject by remember {
        mutableStateOf("")
    }
    var emailBody by remember {
        mutableStateOf("")
    }

    // Context for starting the email intent
    val ctx = LocalContext.current

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        ContactFormScreen(
            emailSubject = emailSubject,
            onEmailSubjectChange = {
                emailSubject = it
            },
            emailBody = emailBody,
            onEmailBodyChange = {
                emailBody = it
            },
            ctx = ctx
        )
    }


}

@Composable
fun ContactFormScreen(
    emailSubject: String,
    onEmailSubjectChange: (String) -> Unit,
    emailBody: String,
    onEmailBodyChange: (String) -> Unit,
    ctx: Context
) {
    // Column for UI components
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = screenHeight(x = 8.0),
                horizontal = screenWidth(x = 16.0)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Contact Us",
            fontSize = screenFontSize(x = 16.0).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
        )
        Text(
            text = "Have any problem / query? We reply quickly!",
            fontSize = screenFontSize(x = 14.0).sp,
            modifier = Modifier
                .align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        // Text field for entering the email subject
        OutlinedTextField(
            value = emailSubject,
            label = {
                Text(
                    text = "Email subject",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            },
            onValueChange = onEmailSubjectChange,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier
//                .padding(screenWidth(x = 16.0))
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))

        // Text field for entering the email body
        OutlinedTextField(
            value = emailBody,
            label = {
                Text(
                    text = "Email body",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            },
            onValueChange = onEmailBodyChange,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            placeholder = { Text(text = "Enter email body") },
            modifier = Modifier
//                .padding(16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        // Button to send the email
        Button(
            enabled = emailSubject.isNotEmpty() && emailBody.isNotEmpty(),
            onClick = {
            // Create an intent to send an email
            val i = Intent(Intent.ACTION_SEND)

            // Hard-coded recipient email address
            val emailAddress = arrayOf("hubkiwitech@gmail.com")
            i.putExtra(Intent.EXTRA_EMAIL, emailAddress)

            // Pass the subject and body entered by the user
            i.putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            i.putExtra(Intent.EXTRA_TEXT, emailBody)

            // Set the intent type
            i.type = "message/rfc822"

            // Start the email client chooser
            ctx.startActivity(Intent.createChooser(i, "Choose an Email client : "))
        }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Send Email",
                fontSize = screenFontSize(x = 14.0).sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ContactFormScreenPreview() {
    CashLedgerTheme {
        ContactFormScreen(
            emailSubject = "",
            onEmailSubjectChange = {},
            emailBody = "",
            onEmailBodyChange = {},
            ctx = LocalContext.current
        )
    }
}