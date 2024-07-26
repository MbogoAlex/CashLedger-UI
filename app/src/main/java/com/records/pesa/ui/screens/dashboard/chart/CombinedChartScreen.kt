package com.records.pesa.ui.screens.dashboard.chart

import android.app.DatePickerDialog
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import co.yml.charts.axis.AxisData
import co.yml.charts.common.components.Legends
import co.yml.charts.common.model.LegendsConfig
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.models.BarPlotData
import co.yml.charts.ui.barchart.models.BarStyle
import co.yml.charts.ui.combinedchart.CombinedChart
import co.yml.charts.ui.combinedchart.model.CombinedChartData
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.records.pesa.R
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.groupedTransactions
import com.records.pesa.reusables.transactionTypes
import com.records.pesa.ui.screens.dashboard.chart.setup.getColorPaletteList
import com.records.pesa.ui.screens.dashboard.chart.setup.getGroupBarChartData
import com.records.pesa.ui.screens.dashboard.chart.setup.getLegendsLabelData
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun BarWithLineChart(
    modifier: Modifier = Modifier
) {
    // Calculate the maximum value for moneyIn and moneyOut dynamically
    val maxAmount = groupedTransactions.maxOf { maxOf(it.moneyIn, it.moneyOut) }
    Log.i("MAX_VALUE", maxAmount.toString())
    val steps = 5

    // Prepare data points for moneyIn and moneyOut
    val moneyInPointsData: List<Point> = groupedTransactions.mapIndexed { index, transaction ->
        Point(index.toFloat(), transaction.moneyIn)
    }

    val moneyOutPointsData: List<Point> = groupedTransactions.mapIndexed { index, transaction ->
        Point(index.toFloat(), transaction.moneyOut)
    }

    // Group bar data preparation
    val groupBarData = getGroupBarChartData(
        transactions = groupedTransactions,
        barSize = 2
    )

    // Calculate yStepSize dynamically based on maxAmount and steps
    val yStepSize = maxAmount / steps

    // Define x-axis data
    val xAxisData = AxisData.Builder()
        .steps(groupedTransactions.size - 1)
        .backgroundColor(Color.Transparent)
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .axisStepSize(100.dp)
        .bottomPadding(5.dp)
        .labelData { i -> groupedTransactions.getOrNull(i)?.date ?: "" }
        .build()

    // Define y-axis data with dynamic scaling
    val yAxisData = AxisData.Builder()
        .steps(steps)
        .backgroundColor(Color.Transparent)
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .labelAndAxisLinePadding(20.dp)
        .axisOffset(20.dp)
        .labelData { i ->
            val yScale = maxAmount / steps
            (i * yScale).toString()
        }
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = moneyInPointsData,
                    LineStyle(
                        color = MaterialTheme.colorScheme.inversePrimary,
                        width = 3f
                    ),
                    IntersectionPoint(
                        color = MaterialTheme.colorScheme.inversePrimary,
                    ),
                    SelectionHighlightPoint(
                        color = Color.Red,
                    ),
                    ShadowUnderLine(
                        alpha = 0.5f,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.inversePrimary,
                                Color.Transparent
                            )
                        )
                    ),
                    SelectionHighlightPopUp(
                        popUpLabel = {x, y ->
                            Log.i("POINTS", "X: $x, Y: $y")
                            "X: $x, Y: $y"
                        }
                    )
                ),
                Line(
                    dataPoints = moneyOutPointsData,
                    LineStyle(
                        color = MaterialTheme.colorScheme.error,
                        width = 3f
                    ),
                    IntersectionPoint(
                        color = MaterialTheme.colorScheme.error
                    ),
                    SelectionHighlightPoint(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    ShadowUnderLine(
                        alpha = 0.5f,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error,
                                Color.Transparent
                            )
                        )
                    ),
                    SelectionHighlightPopUp()
                )
            )
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(
            enableVerticalLines = false
        ),
        backgroundColor = MaterialTheme.colorScheme.background
    )


    // Layout the combined chart and legends
    LineChart(
        modifier = Modifier
            .fillMaxHeight(),
        lineChartData = lineChartData
    )

}
@Composable
fun CombinedChartScreenComposable(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        CombinedChartScreen()
    }
}

@Composable
fun CombinedChartScreen(
    modifier: Modifier = Modifier
) {
    val durations = listOf("Daily", "Weekly", "Monthly", "Yearly")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
//            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
        ) {
            Icon(painter = painterResource(id = R.drawable.arrow_downward), contentDescription = null)
            Text(
                text = "Ksh3,200",
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(painter = painterResource(id = R.drawable.arrow_upward), contentDescription = null)
           Text(
                text = "Ksh3,200",
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Transactions",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            value = "",
            placeholder = {
                Text(text = "Search for transactions")
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
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Column {
                TextButton(onClick = {}) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "All types")
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
                                Text(text = it)
                            }
                            Divider()
                        }
                    }
                }
            }
            Card {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
                                vertical = 8.dp,
                                horizontal = 16.dp
                            )
                    ) {
                        Text(
                            text = "Daily",
                            modifier = Modifier
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Select duration")
                    }
                    DropdownMenu(expanded = false, onDismissRequest = { /*TODO*/ }) {
                        durations.forEach {
                            DropdownMenuItem(onClick = { /*TODO*/ }) {
                                Text(text = it)
                            }
                        }
                    }

                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Date selection section
        DateRangePicker(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(5),
            defaultStartDate = null,
            defaultEndDate = null,
            onChangeStartDate = {},
            onChangeLastDate = {},
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        // Draw chart
        BarWithLineChart()
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangePickerDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Red
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier

    ) {
        Popup(
            alignment = Alignment.TopEnd,
            properties = PopupProperties(
                excludeFromSystemGesture = true
            ),
            onDismissRequest = onDismiss,
        ) {
            Card(
                shape = RoundedCornerShape(0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Select date range",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(
                                start = 16.dp
                            )
                    )
                    DateRangePicker(
                        startDate = startDate,
                        endDate = endDate,
                        defaultStartDate = defaultStartDate,
                        defaultEndDate = defaultEndDate,
                        onChangeStartDate = onChangeStartDate,
                        onChangeLastDate = onChangeLastDate,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp
                            )
                            .align(Alignment.End)
                    ) {
                        Text(text = "Dismiss")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangePicker(
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse the default start and end dates
    val defaultStartLocalDate = defaultStartDate?.let { LocalDate.parse(it) }
    val defaultEndLocalDate = defaultEndDate?.let { LocalDate.parse(it) }

    // Convert LocalDate to milliseconds since epoch
    val defaultStartMillis = defaultStartLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val defaultEndMillis = defaultEndLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker(isStart: Boolean) {
        val initialDate = if (isStart) startDate else endDate
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    if (selectedDate.isBefore(endDate) || selectedDate.isEqual(endDate)) {
                        onChangeStartDate(selectedDate)
                    } else {
                        // Handle case where start date is after end date
                        Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (selectedDate.isAfter(startDate) || selectedDate.isEqual(startDate)) {
                        onChangeLastDate(selectedDate)
                    } else {
                        // Handle case where end date is before start date
                        Toast.makeText(context, "End date must be after start date", Toast.LENGTH_LONG).show()
                    }
                }
            },

            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )

        // Set minimum and maximum dates
        defaultStartMillis?.let { datePicker.datePicker.minDate = it }
        defaultEndMillis?.let { datePicker.datePicker.maxDate = it }

        datePicker.show()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.elevatedCardElevation(10.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(onClick = { showDatePicker(true) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null
                )
            }
            Text(text = dateFormatter.format(startDate))
            Text(text = "to")

            Text(text = dateFormatter.format(endDate))
            IconButton(onClick = { showDatePicker(false) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CombinedChartScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        Box(
            modifier = Modifier
                .safeDrawingPadding()
        ) {
            CombinedChartScreen()
        }
    }
}