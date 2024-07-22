package com.records.pesa.ui.screens.dashboard.budget

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.BudgetDt
import com.records.pesa.reusables.budget
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Composable
fun BudgetInfoScreenComposable(
    modifier: Modifier = Modifier
) {

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetInfoScreen(
    budgetDt: BudgetDt,
    navigateToTransactionsScreen: (categoryId: Int, budgetId: Int, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val difference = budgetDt.expenditure - budgetDt.budgetLimit
    var expanded by remember {
        mutableStateOf(false)
    }
    val progress = budgetDt.expenditure / budgetDt.budgetLimit
    val percentLeft = (1 - progress) * 100

    val days = ChronoUnit.DAYS.between(LocalDate.parse(budgetDt.limitDate), LocalDateTime.parse(budgetDt.createdAt))

    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.error,
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete this budget",
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = budgetDt.name!!,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit budget"
                )
            }
        }
        if(budgetDt.active) {
            Text(
                text = "ACTIVE",
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "INACTIVE",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Spent: ${formatMoneyValue(budgetDt.expenditure)} / ${formatMoneyValue(budgetDt.budgetLimit)}",
            color = MaterialTheme.colorScheme.surfaceTint,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = progress.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "$percentLeft % left",
            color = MaterialTheme.colorScheme.surfaceTint,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Difference:",
            )
            Spacer(modifier = Modifier.width(3.dp))
            if(difference <= 0) {
                Text(
                    text = formatMoneyValue(difference.absoluteValue),
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
            } else {
                Text(
                    text = "- ${formatMoneyValue(difference)}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Created on ${formatIsoDateTime(LocalDateTime.parse(budgetDt.createdAt))}",
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budget period ends on ${budgetDt.limitDate}",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit budget"
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Period: ${days.absoluteValue} days",
            fontWeight = FontWeight.Bold
        )
        if(budgetDt.limitReached) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(text = "Limit Reached")
            Spacer(modifier = Modifier.height(5.dp))
            Text(text = "Reached limit on ${budgetDt.limitReachedAt}")
            Spacer(modifier = Modifier.height(5.dp))
            Text(text = "Overspent by ${formatMoneyValue(difference)}")
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = "Category: ${budgetDt.category.name}")
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                navigateToTransactionsScreen(budgetDt.category.id, budgetDt.id, LocalDate.parse(budgetDt.createdAt).toString(), budgetDt.limitDate)
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Transactions")
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "See transactions")
            }
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetInfoScreenPreview() {
    CashLedgerTheme {
        BudgetInfoScreen(
            budgetDt = budget,
            navigateToTransactionsScreen = {categoryId, budgetId, startDate, endDate ->}
        )
    }
}