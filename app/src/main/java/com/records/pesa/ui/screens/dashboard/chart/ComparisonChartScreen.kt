package com.records.pesa.ui.screens.dashboard.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.Point
import com.records.pesa.R
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.groupedTransactions
import com.records.pesa.reusables.transactionTypes
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

@Composable
fun ComparisonChartScreenComposable(
    categoryId: String?,
    budgetId: String?,
    startDate: String?,
    endDate: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        ComparisonChartScreen(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(5),
            totalMoneyOut = formatMoneyValue(1500.0),
            totalMoneyIn = formatMoneyValue(2000.0)
        )
    }
}

@Composable
fun ComparisonChartScreen(
    startDate: LocalDate,
    endDate: LocalDate,
    totalMoneyIn: String,
    totalMoneyOut: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
    ) {
        Text(
            text = "Chart 1",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        ChartOne(
            startDate = startDate,
            endDate = endDate,
            totalMoneyIn = totalMoneyIn,
            totalMoneyOut = totalMoneyOut,
            modifier = Modifier
                .height(600.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Divider()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Chart 2",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        ChartOne(
            startDate = startDate,
            endDate = endDate,
            totalMoneyIn = totalMoneyIn,
            totalMoneyOut = totalMoneyOut,
            modifier = Modifier
                .height(600.dp)
        )

    }
}

@Composable
fun ChartOne(
    startDate: LocalDate,
    endDate: LocalDate,
    totalMoneyIn: String,
    totalMoneyOut: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        Column {
            TextField(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                value = "",
                placeholder = {
                    androidx.compose.material3.Text(text = "Search for transactions")
                },
                trailingIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.inverseOnSurface)
                            .padding(5.dp)
                            .clickable {
//                            onClearSearch()
                            }

                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier
                                .size(16.dp)
                        )
                    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                Text(
                    text = "${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}",
                    fontWeight = FontWeight.Bold,
//                        textAlign = TextAlign.Center,
                    modifier = Modifier

//                            .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = "Select date range",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                        }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                TextButton(onClick = {}) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(text = "All types")
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = false, onDismissRequest = {}) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 250.dp)
                            .padding(
                                horizontal = 5.dp
                            )
                            .verticalScroll(rememberScrollState())
                    ) {
                        transactionTypes.forEach {
                            DropdownMenuItem(onClick = {  }) {
                                androidx.compose.material3.Text(text = it)
                            }
                            androidx.compose.material3.Divider()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                Icon(painter = painterResource(id = R.drawable.arrow_downward), contentDescription = null)
                Text(
                    text = totalMoneyIn,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(painter = painterResource(id = R.drawable.arrow_upward), contentDescription = null)
                Text(
                    text = totalMoneyOut,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            BarWithLineChart(
                transactions = groupedTransactions,
                moneyInPointsData = groupedTransactions.mapIndexed { index, transaction ->
                    Point(index.toFloat(), transaction.moneyIn)
                },
                moneyOutPointsData = groupedTransactions.mapIndexed { index, transaction ->
                    Point(index.toFloat(), transaction.moneyOut)
                },
                maxAmount = groupedTransactions.maxOf { maxOf(it.moneyIn, it.moneyOut) },
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ComparisonChartScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        Box(
            modifier = Modifier
                .safeDrawingPadding()
        ) {
            ComparisonChartScreen(
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(5),
                totalMoneyOut = formatMoneyValue(1500.0),
                totalMoneyIn = formatMoneyValue(2000.0)
            )
        }
    }
}