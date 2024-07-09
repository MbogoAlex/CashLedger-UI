package com.records.pesa.ui.screens.dashboard.budget

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.BudgetDt
import com.records.pesa.reusables.budgets
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime
import kotlin.math.absoluteValue

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetScreenComposable(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        BudgetScreen()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        TextField(
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            value = "",
            placeholder = {
                          Text(text = "Budget / Category")
            },
            trailingIcon = {
                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn {
            items(budgets) {
                BudgetItem(budgetDt = it)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetItem(
    budgetDt: BudgetDt,
    modifier: Modifier = Modifier
) {
    val difference = budgetDt.expenditure - budgetDt.budgetLimit
    var expanded by remember {
        mutableStateOf(false)
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 10.dp
            )
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            ) {
                Text(
                    text = budgetDt.name ?: "N/A",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
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
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Spent: ${formatMoneyValue(budgetDt.expenditure)} / ${formatMoneyValue(budgetDt.budgetLimit)}:",
                    color = MaterialTheme.colorScheme.surfaceTint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
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
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Created on ${formatIsoDateTime(LocalDateTime.parse(budgetDt.createdAt))}",
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

                Spacer(modifier = Modifier.height(5.dp))
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = "Explore")
                }

                
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        BudgetScreen()
    }
}