package com.records.pesa.ui.screens.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenWidth

@Composable
fun PasswordInputField(
    heading: String,
    value: String,
    trailingIcon: Int?,
    readOnly: Boolean = false,
    onValueChange: (newValue: String) -> Unit,
    keyboardOptions: KeyboardOptions,
    visibility: Boolean?,
    onChangeVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        label = {
            Text(
                text = heading,
//                    fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.scrim,
                fontSize = screenFontSize(x = 14.0).sp
//                    fontSize = 18.sp
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.password),
                contentDescription = null
            )
        },
        trailingIcon = {
            if(trailingIcon != null) {
                IconButton(onClick = onChangeVisibility) {
                    if(visibility != null && visibility) {
                        Icon(
                            painter = painterResource(id = R.drawable.visibility_off),
                            contentDescription = null,
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    } else if(visibility != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.visibility_on),
                            contentDescription = null,
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }

            }
        },
        readOnly = readOnly,
        visualTransformation = if(visibility != null && visibility) VisualTransformation.None else PasswordVisualTransformation(),
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
    )
}