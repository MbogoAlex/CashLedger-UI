package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

@Composable
fun BudgetCreationScreenComposable(
    modifier: Modifier = Modifier
) {

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetCreationScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startDate = LocalDate.now()
    var selectedEndDate by rememberSaveable {
        mutableStateOf<LocalDate?>(null)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker() {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (selectedDate.isAfter(startDate)) {
                    selectedEndDate = selectedDate
                } else {
                    // Handle case where end date is before start date
                    Toast.makeText(context, "Start date must be after the current date", Toast.LENGTH_LONG)
                        .show()
                }
            },

            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth + 1
        )

        datePicker.show()
    }

    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Set budget",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        OutlinedTextField(
            label = {
                Text(text = "name")
            },
            value = "",
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Budget starts on ${formatLocalDate(LocalDate.now())} (Today)")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = {
                    Text(text = "Budget limit")
                },
                value = "",
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal
                ),
                onValueChange = {},
                modifier = Modifier
                    .weight(2f)
            )
            Spacer(modifier = Modifier.width(5.dp))
            TextButton(
                onClick = { showDatePicker() },
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if(selectedEndDate == null) "Limit date" else selectedEndDate.toString())
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(painter = painterResource(id = R.drawable.calendar), contentDescription = "Select limit date")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Create budget")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetCreationScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        BudgetCreationScreen()
    }

}