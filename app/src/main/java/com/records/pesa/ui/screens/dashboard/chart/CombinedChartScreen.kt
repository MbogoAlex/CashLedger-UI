package com.records.pesa.ui.screens.dashboard.chart

//import com.records.pesa.ui.screens.dashboard.chart.setup.LineChart
import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberFadingEdges
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.groupedTransactions
import com.records.pesa.reusables.transactionTypes
import com.records.pesa.ui.screens.dashboard.chart.setup.getGroupBarChartData
import com.records.pesa.ui.screens.dashboard.chart.vico.rememberMarker
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun CombinedChartScreenComposable(
    categoryId: String?,
    budgetId: String?,
    startDate: String?,
    endDate: String?,
    modifier: Modifier = Modifier
) {

    val viewModel: CombinedChartScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(
            categoryId = categoryId,
            budgetId = budgetId,
            defaultStartDate = startDate,
            defaultEndDate = endDate
        )
    }

    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        CombinedChartScreen(
            transactions = uiState.monthlyTransactions,
            totalMoneyIn = formatMoneyValue(uiState.totalMoneyIn),
            totalMoneyOut = formatMoneyValue(uiState.totalMoneyOut),
            maxAmount = uiState.maxAmount,
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse(uiState.endDate),
            defaultStartDate = uiState.defaultStartDate,
            defaultEndDate = uiState.defaultEndDate,
            moneyInPointsData = uiState.moneyInPointsData,
            moneyOutPointsData = uiState.moneyOutPointsData,
            transactionType = uiState.transactionType,
            onChangeTransactionType = {
                viewModel.updateTransactionType(it)
            },
            searchText = uiState.searchText,
            onChangeSearchText = {
                viewModel.updateEntity(it)
            },
            onClearSearch = {
                viewModel.updateEntity("")
            },
            onChangeStartDate = {
                viewModel.updateStartDate(it)
            },
            onChangeEndDate = {
                viewModel.updateEndDate(it)
            }
        )
    }
}

@Composable
fun CombinedChartScreen(
    transactions: List<MonthlyTransaction>,
    totalMoneyIn: String,
    totalMoneyOut: String,
    maxAmount: Float,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    moneyInPointsData: List<Point>,
    moneyOutPointsData: List<Point>,
    searchText: String,
    onChangeSearchText: (name: String) -> Unit,
    transactionType: String,
    onChangeTransactionType: (type: String) -> Unit,
    onChangeStartDate: (startDate: LocalDate) -> Unit,
    onChangeEndDate: (endDate: LocalDate) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durations = listOf("Daily", "Weekly", "Monthly", "Yearly")

    var typeChangeOn by rememberSaveable {
        mutableStateOf(false)
    }

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
            value = searchText,
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
                            onClearSearch()
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
            onValueChange = onChangeSearchText,
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Column {
                TextButton(onClick = {
                    typeChangeOn = !typeChangeOn
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = transactionType)
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = typeChangeOn, onDismissRequest = {typeChangeOn = !typeChangeOn}) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 250.dp)
                            .padding(
                                horizontal = 5.dp
                            )
                            .verticalScroll(rememberScrollState())
                    ) {
                        transactionTypes.forEach {
                            DropdownMenuItem(onClick = { onChangeTransactionType(it) }) {
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
            startDate = startDate,
            endDate = endDate,
            defaultStartDate = defaultStartDate,
            defaultEndDate = defaultEndDate,
            onChangeStartDate = onChangeStartDate,
            onChangeLastDate = onChangeEndDate,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        // Draw chart
        BarWithLineChart(
            transactions = transactions,
            maxAmount = maxAmount,
            moneyInPointsData = moneyInPointsData,
            moneyOutPointsData = moneyOutPointsData
        )
    }
}


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

@Composable
fun BarWithLineChart(
    transactions: List<MonthlyTransaction>,
    maxAmount: Float,
    moneyInPointsData: List<Point>,
    moneyOutPointsData: List<Point>,
    modifier: Modifier = Modifier
) {

    var maxAmount by rememberSaveable {
        mutableFloatStateOf(0.0f)
    }

    val steps = 20

    // Group bar data preparation
    val groupBarData = getGroupBarChartData(
        transactions = groupedTransactions,
        barSize = 2
    )

    // Calculate yStepSize dynamically based on maxAmount and steps
    val yStepSize = maxAmount / steps

    // Define x-axis data
    val xAxisData = AxisData.Builder()
        .steps(transactions.size)
        .backgroundColor(Color.Transparent)
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .axisStepSize(100.dp)
        .bottomPadding(5.dp)
        .labelData { i -> transactions.getOrNull(i)?.date ?: "" }
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
            )
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(
            enableVerticalLines = false
        ),
        backgroundColor = MaterialTheme.colorScheme.background
    )
    var scrollJob: Job? = null
    val scope = rememberCoroutineScope()


    var targetMaxAmount by remember { mutableStateOf(0f) }

    val transition = updateTransition(targetMaxAmount, label = "Max Amount Transition")
    val animatedMaxAmount by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "Animated Max Amount"
    ) { it }


    Chart3(
        transactions = transactions,
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    )

//    val data = listOf(LineData())
    


}



@Composable
internal fun Chart3(transactions: List<MonthlyTransaction>, modifier: Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(transactions) {
        withContext(Dispatchers.Default) {
            while (isActive) {
                modelProducer.runTransaction {
                    /* Learn more:
                    https://patrykandpatrick.com/vico/wiki/cartesian-charts/layers/line-layer#data. */
                    lineSeries {
                        series(y = transactions.map { it.moneyIn }, x = transactions.mapIndexed { index, transaction -> index.toFloat() })
                        series(y = transactions.map { it.moneyOut }, x = transactions.mapIndexed { index, transaction -> index.toFloat() })
                    }

                }
                delay(2000L)
            }
        }
    }
    ComposeChart3(transactions, modelProducer, modifier)
}
val pointerXDeltas = MutableSharedFlow<Float>(extraBufferCapacity = 1)
@Composable
private fun ComposeChart3(transactions: List<MonthlyTransaction>, modelProducer: CartesianChartModelProducer, modifier: Modifier) {
    val axisValueOverrider = AxisValueOverrider.adaptiveYValues(yFraction = 1.2f, round = true)
    val lineColor = Color(0xffffbb00)
    val bottomAxisLabelBackgroundColor = Color(0xff9db591)
    val scrollState = rememberVicoScrollState()
    CartesianChartHost(
        chart =
        rememberCartesianChart(
            rememberLineCartesianLayer(
                pointSpacing = 100.dp,
                lineProvider =
                LineCartesianLayer.LineProvider.series(
                    rememberLine(DynamicShader.color(Color.Green)),
                    rememberLine(DynamicShader.color(Color.Red))
                ),
                axisValueOverrider = axisValueOverrider,
            ),
            startAxis =
            rememberStartAxis(
                guideline = null,
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
                titleComponent =
                rememberTextComponent(
                    color = Color.Black,
                    margins = Dimensions.of(end = 4.dp),
                    padding = Dimensions.of(8.dp, 2.dp),
                    background = rememberShapeComponent(lineColor, Shape.Pill),
                ),
                title = stringResource(R.string.y_axis),
            ),
            bottomAxis =
            rememberBottomAxis(
                titleComponent =
                rememberTextComponent(
                    color = Color.White,
                    margins = Dimensions.of(top = 4.dp),
                    padding = Dimensions.of(start = 8.dp, 2.dp),
                    background = rememberShapeComponent(bottomAxisLabelBackgroundColor, Shape.Pill),
                ),
                valueFormatter = { value, _, _ ->
                    val index = value.toInt()
                    if(index in transactions.indices) {
//                        Log.d("INDICES", transactions.indices.toString())
                        transactions[index].date
//                        Log.d("DATE", transactions[index].date.toString())
                    } else {
                        ""
                    }
                },
                sizeConstraint = BaseAxis.SizeConstraint.Auto(
                    minSizeDp = 50f
                ),
                title = stringResource(R.string.x_axis),
            ),
            marker = rememberMarker(DefaultCartesianMarker.LabelPosition.AroundPoint),
            horizontalLayout = HorizontalLayout.fullWidth(),
            fadingEdges = rememberFadingEdges(),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = scrollState,
        zoomState = rememberVicoZoomState(zoomEnabled = false),
    )
}

@Composable
fun CanvasChart(
    transactions: List<GroupedTransactionData>,
    modifier: Modifier = Modifier,
    graphColor: Color = Color.Green
) {
    val spacing = 200f
    val transparentGraphColor = remember {
        graphColor.copy(alpha = 0.5f)
    }
    val upperValue = remember(transactions) {
        (transactions.maxOfOrNull { it.moneyOut }?.plus(1))?.roundToInt() ?: 0
    }
    val lowerValue = remember(transactions) {
        transactions.minOfOrNull { it.moneyOut }?.toInt() ?: 0
    }
    var showAlertDialog by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf<Pair<String, Float>?>(null) }
    val density = LocalDensity.current
    val textPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = density.run { 12.sp.toPx() }
        }
    }

    // Calculate the total width based on data points and spacing
    val totalWidth = spacing * 2 + transactions.size * spacing

    Row(modifier = modifier) {
        // Y-axis labels
        Canvas(modifier = Modifier
            .width(50.dp)
            .fillMaxHeight()) {
            val priceStep = (upperValue - lowerValue) / 5f
            (0..4).forEach { i ->
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        round(lowerValue + priceStep * i).toString(),
                        30f,
                        size.height - spacing - i * size.height / 5f,
                        textPaint
                    )
                }
            }
        }

        // Scrollable chart
        LazyRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Box(modifier = Modifier.fillMaxHeight()) {
                    Canvas(
                        modifier = Modifier
                            .width(with(LocalDensity.current) { totalWidth.toDp() })
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val spacePerTransaction =
                                        (size.width - 2 * spacing) / (transactions.size - 1).coerceAtLeast(
                                            1
                                        )
                                    val closestXIndex = ((offset.x - spacing) / spacePerTransaction)
                                        .toInt()
                                        .coerceIn(0, transactions.size - 1)
                                    val closestX = spacing + closestXIndex * spacePerTransaction
                                    val tolerance = spacePerTransaction / 2

                                    if (abs(offset.x - closestX) < tolerance) {
                                        val clickedTransaction = transactions[closestXIndex]
                                        val yValue = clickedTransaction.moneyOut.toFloat()
                                        selectedPoint = clickedTransaction.date to yValue
                                        showAlertDialog = true
                                    }
                                }
                            }
                    ) {
                        val spacePerTransaction = (size.width - 2 * spacing) / (transactions.size - 1).coerceAtLeast(1)

                        // Draw X-axis labels
                        transactions.forEachIndexed { index, info ->
                            val date = info.date
                            val x = spacing + index * spacePerTransaction
                            drawContext.canvas.nativeCanvas.apply {
                                drawText(
                                    date.substring(5, 10),
                                    x,
                                    size.height - 5,
                                    textPaint
                                )
                            }
                        }

                        // Draw graph path
                        var lastX = 0f
                        val strokePath = Path().apply {
                            val height = size.height
                            transactions.forEachIndexed { i, info ->
                                val nextInfo = transactions.getOrNull(i + 1) ?: transactions.last()
                                val leftRatio = (info.moneyOut - lowerValue) / (upperValue - lowerValue)
                                val rightRatio = (nextInfo.moneyOut - lowerValue) / (upperValue - lowerValue)
                                val x1 = spacing + i * spacePerTransaction
                                val y1 = height - spacing - (leftRatio * height).toFloat()
                                val x2 = spacing + (i + 1) * spacePerTransaction
                                val y2 = height - spacing - (rightRatio * height).toFloat()
                                if (i == 0) {
                                    moveTo(x1, y1)
                                }
                                lastX = (x1 + x2) / 2f
                                quadraticBezierTo(x1, y1, lastX, (y1 + y2) / 2f)
                            }
                        }

                        // Draw the fill path
                        val fillPath = android.graphics.Path(strokePath.asAndroidPath())
                            .asComposePath()
                            .apply {
                                lineTo(lastX, size.height - spacing)
                                lineTo(spacing, size.height - spacing)
                                close()
                            }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    transparentGraphColor,
                                    Color.Transparent
                                ),
                                endY = size.height - spacing
                            )
                        )
                        drawPath(
                            path = strokePath,
                            color = graphColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                            ),
                        )
                    }

                    // Display selected point details
                    selectedPoint?.let { (date, value) ->
                        if (showAlertDialog) {
                            AlertDialog(
                                onDismissRequest = { showAlertDialog = false },
                                confirmButton = {
                                    TextButton(onClick = { showAlertDialog = false }) {
                                        Text("OK")
                                    }
                                },
                                text = {
                                    Box(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .background(Color.DarkGray, RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Date: $date\nValue: ${"%.2f".format(value)}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
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
            CombinedChartScreen(
                transactions = emptyList(),
                totalMoneyIn = "Ksh3,200",
                totalMoneyOut = "Ksh2,500",
                maxAmount = groupedTransactions.maxOf { maxOf(it.moneyIn, it.moneyOut) },
                startDate = LocalDate.parse("2023-03-06"),
                endDate = LocalDate.parse("2024-06-25"),
                defaultStartDate = null,
                defaultEndDate = null,
                moneyInPointsData = groupedTransactions.mapIndexed { index, transaction ->
                    Point(index.toFloat(), transaction.moneyIn)},
                moneyOutPointsData = groupedTransactions.mapIndexed { index, transaction ->
                    Point(index.toFloat(), transaction.moneyOut)
                },
                transactionType = "All types",
                onChangeTransactionType = {},
                searchText = "",
                onChangeSearchText = {},
                onClearSearch = {},
                onChangeStartDate = {},
                onChangeEndDate = {}
            )
        }
    }
}