package com.records.pesa.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R

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
    OutlinedTextField(
        value = value,
        label = {
            Text(
                text = heading,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.password),
                contentDescription = null
            )
        },
        trailingIcon = {
            if (trailingIcon != null) {
                IconButton(onClick = onChangeVisibility) {
                    if (visibility != null && visibility) {
                        Icon(
                            painter = painterResource(id = R.drawable.visibility_off),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (visibility != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.visibility_on),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        readOnly = readOnly,
        visualTransformation = if (visibility != null && visibility) VisualTransformation.None else PasswordVisualTransformation(),
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}