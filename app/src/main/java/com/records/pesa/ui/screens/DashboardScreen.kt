package com.records.pesa.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.functions.formatDateTime
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreenComposable(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        DashboardScreen()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions History",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = "See all")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn {
            items(2) {
                TransactionItemCell(
                    transaction = transactions[it]
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = "See all")
            }
        }

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HeaderSection(
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Text(text = "Hello,")
//                Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Alex Mbogo",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .padding(
                        top = 10.dp,
                        start = 10.dp,
                        end = 10.dp
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(text = formatDateTime(LocalDateTime.now()))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Current balance")
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                painter = painterResource(id = R.drawable.visibility_off),
                                contentDescription = "Hide balance"
                            )
                        }
                    }

//                Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "KES 5,530",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row() {
                        Column {
                            Row {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_downward),
                                    contentDescription = null
                                )
                                Text(text = "Income")
                            }
                            Text(text = "KES 1,200")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            Row {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_upward),
                                    contentDescription = null
                                )
                                Text(text = "Expenses")
                            }
                            Text(text = "KES 4,330")
                        }
                    }
                }
            }

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        DashboardScreen()
    }
}