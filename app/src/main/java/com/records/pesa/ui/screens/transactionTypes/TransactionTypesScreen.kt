package com.records.pesa.ui.screens.transactionTypes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.ui.screens.transactions.DateRangePickerDialog
import com.records.pesa.ui.screens.transactions.TransactionCostCard
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.scale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

// ─── Type colour palette ──────────────────────────────────────────────────────

private val typeColors: List<Color> = listOf(
    Color(0xFF006A65), Color(0xFF7B5EA7), Color(0xFF48607B), Color(0xFFB5542D),
    Color(0xFF2D6A8A), Color(0xFF8A3D4A), Color(0xFF5A7A2B), Color(0xFF4A6361),
    Color(0xFF7B6E28), Color(0xFF6B3D7A), Color(0xFF2D6A4A), Color(0xFF8A5D2D),
)

private fun entryColor(index: Int): Color = typeColors[index % typeColors.size]

private fun typeDisplayName(type: String): String = when (type) {
    "Buy Goods and Services (till)" -> "Buy Goods (Till)"
    "KCB Mpesa account"             -> "KCB M-PESA"
    "Hustler fund", "Hustler Fund"  -> "Hustler Fund"
    "Mshwari"                       -> "M-Shwari"
    else                            -> type
}

// ─── Entry point ─────────────────────────────────────────────────────────────

@Composable
fun TransactionTypesScreenComposable(
    navigateToTransactionsScreen: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    initialStartDate: String? = null,
    initialEndDate: String? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToPreviousScreen)

    val viewModel: TransactionTypesScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialStartDate, initialEndDate) {
        if (!initialStartDate.isNullOrEmpty() && !initialEndDate.isNullOrEmpty()) {
            viewModel.applyInitialDates(initialStartDate, initialEndDate)
        }
    }

    TransactionTypesScreen(
        allMoneyIn      = uiState.allMoneyIn,
        allMoneyOut     = uiState.allMoneyOut,
        moneyInEntries  = uiState.moneyInEntries,
        moneyOutEntries = uiState.moneyOutEntries,
        totalTransactionCost = uiState.totalTransactionCost,
        selectedTimePeriod = uiState.selectedTimePeriod,
        startDate       = uiState.startDate,
        endDate         = uiState.endDate,
        onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
        onChangeStartDate = { viewModel.updateStartDate(it) },
        onChangeEndDate   = { viewModel.updateEndDate(it) },
        isPremium = uiState.preferences.paid || uiState.preferences.permanent,
        onShowSubscriptionScreen = navigateToSubscriptionScreen,
        navigateToPreviousScreen = navigateToPreviousScreen,
        onNavigateToTransactions = navigateToTransactionsScreen,
        modifier = modifier
    )
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun TransactionTypesScreen(
    allMoneyIn: Double,
    allMoneyOut: Double,
    moneyInEntries: List<TransactionTypeEntry>,
    moneyOutEntries: List<TransactionTypeEntry>,
    totalTransactionCost: Double,
    selectedTimePeriod: TimePeriod,
    startDate: String,
    endDate: String,
    onPeriodSelected: (TimePeriod) -> Unit,
    onChangeStartDate: (LocalDate) -> Unit = {},
    onChangeEndDate: (LocalDate) -> Unit = {},
    isPremium: Boolean = false,
    onShowSubscriptionScreen: () -> Unit = {},
    navigateToPreviousScreen: () -> Unit = {},
    onNavigateToTransactions: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showIn by rememberSaveable { mutableStateOf(true) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }
    val entries = if (showIn) moneyInEntries else moneyOutEntries
    val total   = if (showIn) allMoneyIn else allMoneyOut
    val listState = rememberLazyListState()
    val showStickyHeader by remember { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Content column ────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateToPreviousScreen) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scaleX = -1f, scaleY = 1f)
                        )
                    }
                    Text(
                        text = "Transaction Types",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                    FullPeriodChip(
                        selectedTimePeriod = selectedTimePeriod,
                        startDate = startDate,
                        endDate = endDate,
                        isPremium = isPremium,
                        onPeriodSelected = onPeriodSelected,
                        onShowSubscriptionScreen = onShowSubscriptionScreen,
                        onOpenCustomPicker = { showDateRangePicker = true }
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Hero summary card ────────────────────────────────────────
                item {
                    TxTypesHeroCard(
                        totalIn  = allMoneyIn,
                        totalOut = allMoneyOut,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // ── Tab toggle ───────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabChip(
                            label = "Money In",
                            selected = showIn,
                            selectedBg = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { showIn = true }
                        )
                        TabChip(
                            label = "Money Out",
                            selected = !showIn,
                            selectedBg = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            onClick = { showIn = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Donut chart (only when data exists) ──────────────────────
                if (entries.isNotEmpty()) {
                    item {
                        DonutChart(
                            entries  = entries,
                            total    = total,
                            showIn   = showIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // ── Type breakdown list ──────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (showIn) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error
                                )
                        )
                        Text(
                            text = if (showIn) "BREAKDOWN — MONEY IN" else "BREAKDOWN — MONEY OUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = if (showIn) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error,
                        )
                    }
                }

                if (entries.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.transactions),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "No ${if (showIn) "money in" else "money out"} transactions",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Try a different time period",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    items(entries.mapIndexed { i, e -> i to e }, key = { it.first }) { (idx, entry) ->
                        TypeBreakdownRow(
                            entry     = entry,
                            total     = total,
                            color     = entryColor(idx),
                            index     = idx,
                            startDate = startDate,
                            endDate   = endDate,
                            showIn    = showIn,
                            onNavigate = onNavigateToTransactions,
                            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // ── Transaction fees section ─────────────────────────────────
                if (totalTransactionCost > 0) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionCostCard(
                            totalCost = totalTransactionCost,
                            costByType = (if (showIn) moneyInEntries else moneyOutEntries)
                                .filter { it.transactionCost > 0 }
                                .map { entry ->
                                    TransactionTypeSummary(
                                        transactionType = entry.type,
                                        totalAmount = entry.transactionCost,
                                        transactionCount = 0,
                                        percentageOfTotal = if (totalTransactionCost > 0) (entry.transactionCost / totalTransactionCost * 100).toFloat() else 0f
                                    )
                                }
                                .sortedByDescending { it.totalAmount },
                            periodLabel = selectedTimePeriod.getDisplayName(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            } // end LazyColumn
        } // end Column

        // ── Sticky header (overlay, in BoxScope) ─────────────────────────────
        AnimatedVisibility(
            visible = showStickyHeader,
            enter   = fadeIn(tween(200)),
            exit    = fadeOut(tween(150))
        ) {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                tonalElevation  = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateToPreviousScreen) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scaleX = -1f, scaleY = 1f)
                        )
                    }
                    Text(
                        text = "Transaction Types",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                    FullPeriodChip(
                        selectedTimePeriod = selectedTimePeriod,
                        startDate = startDate,
                        endDate = endDate,
                        isPremium = isPremium,
                        onPeriodSelected = onPeriodSelected,
                        onShowSubscriptionScreen = onShowSubscriptionScreen,
                        onOpenCustomPicker = { showDateRangePicker = true }
                    )
                }
            }
        }

        // ── Date range picker overlay ─────────────────────────────────────────
        if (showDateRangePicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showDateRangePicker = false },
                contentAlignment = Alignment.Center
            ) {
                DateRangePickerDialog(
                    premium = isPremium,
                    startDate = runCatching { LocalDate.parse(startDate) }.getOrElse { LocalDate.now().withDayOfMonth(1) },
                    endDate = runCatching { LocalDate.parse(endDate) }.getOrElse { LocalDate.now() },
                    defaultStartDate = null,
                    defaultEndDate = null,
                    onChangeStartDate = { onPeriodSelected(TimePeriod.CUSTOM); onChangeStartDate(it); showDateRangePicker = false },
                    onChangeLastDate = { onChangeEndDate(it); showDateRangePicker = false },
                    onDismiss = { showDateRangePicker = false },
                    onConfirm = { showDateRangePicker = false },
                    onShowSubscriptionDialog = onShowSubscriptionScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { }
                )
            }
        }
    } // end outer Box
}

// ─── Hero summary card ─────────────────────────────────────────────────────────

@Composable
private fun TxTypesHeroCard(
    totalIn: Double,
    totalOut: Double,
    modifier: Modifier = Modifier
) {
    val net = totalIn - totalOut
    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "heroGrad")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroOffset"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, spotColor = primary.copy(alpha = 0.25f), shape = RoundedCornerShape(24.dp)),
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.15f),
                            tertiary.copy(alpha = 0.10f),
                            primary.copy(alpha = 0.05f)
                        ),
                        start = Offset(animOffset, animOffset),
                        end   = Offset(animOffset + 500f, animOffset + 500f)
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = "NET FLOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.0f", kotlin.math.abs(net))}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroStat("In",  "Ksh ${String.format("%,.0f", totalIn)}",  MaterialTheme.colorScheme.tertiary)
                    HeroStat("Out", "Ksh ${String.format("%,.0f", totalOut)}", MaterialTheme.colorScheme.error)
                    HeroStat(
                        "Net",
                        "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.0f", kotlin.math.abs(net))}",
                        if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ─── Donut chart ──────────────────────────────────────────────────────────────

@Composable
private fun DonutChart(
    entries: List<TransactionTypeEntry>,
    total: Double,
    showIn: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (showIn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val surface     = MaterialTheme.colorScheme.surface
    val onSurface   = MaterialTheme.colorScheme.onSurface

    // Animate draw progress 0→1 whenever entries/showIn changes
    var drawTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(entries, showIn) { drawTarget = 1f }
    val drawProgress by animateFloatAsState(
        targetValue  = drawTarget,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "donutDraw"
    )

    if (entries.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.height(200.dp)
        ) {
            Text(
                text  = "No data",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        }
        return
    }

    val segments = entries.mapIndexed { idx, e ->
        Triple(e, entryColor(idx), (e.amount / total.coerceAtLeast(1.0)).toFloat())
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Donut ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val canvasSize   = min(size.width, size.height)
                val strokeWidth  = canvasSize * 0.18f
                val arcRadius    = (canvasSize / 2f) - strokeWidth / 2f
                val arcTopLeft   = Offset((size.width - arcRadius * 2) / 2f, (size.height - arcRadius * 2) / 2f)
                val arcSize      = Size(arcRadius * 2, arcRadius * 2)
                var startAngle   = -90f
                val gap          = if (segments.size > 1) 2f else 0f

                segments.forEach { (_, color, fraction) ->
                    val sweep = (fraction * 360f - gap).coerceAtLeast(0f) * drawProgress
                    drawArc(
                        color      = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter  = false,
                        style      = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        topLeft    = arcTopLeft,
                        size       = arcSize
                    )
                    startAngle += fraction * 360f
                }

                // Centre fill to make it a donut
                drawCircle(
                    color  = surface,
                    radius = arcRadius - strokeWidth / 2f + 2f
                )
            }
            // Centre label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = if (showIn) "IN" else "OUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text  = "Ksh ${String.format("%,.0f", total)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
            }
        }

        // ── Legend ──
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.take(6).forEach { (entry, color, fraction) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(
                        text  = typeDisplayName(entry.type),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text  = "${"%.2f".format(fraction * 100)}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
            if (segments.size > 6) {
                Text(
                    text  = "+${segments.size - 6} more",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Type breakdown row ───────────────────────────────────────────────────────

@Composable
private fun TypeBreakdownRow(
    entry: TransactionTypeEntry,
    total: Double,
    color: Color,
    index: Int,
    startDate: String,
    endDate: String,
    showIn: Boolean,
    onNavigate: (type: String?, direction: String, start: String, end: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = (entry.amount / total.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val barProgress by animateFloatAsState(
        targetValue  = fraction,
        animationSpec = tween(700, delayMillis = index * 60, easing = FastOutSlowInEasing),
        label = "barAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication    = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                val txType = when (entry.type) {
                    "Send Money" -> "Send Money"
                    "Buy Goods and Services (till)" -> "Buy Goods and Services (till)"
                    "Pay Bill"   -> "Pay Bill"
                    "Withdraw Cash" -> "Withdraw Cash"
                    "Airtime & Bundles" -> "Airtime & Bundles"
                    "Fuliza"     -> "Fuliza"
                    "Mshwari"    -> "Mshwari"
                    "Deposit"    -> "Deposit"
                    "Reversal"   -> "Reversal"
                    "Hustler fund", "Hustler Fund" -> "Hustler fund"
                    "KCB Mpesa account" -> "KCB Mpesa account"
                    "Pochi la Biashara" -> "Pochi la Biashara"
                    else         -> entry.type
                }
                onNavigate(txType, if (showIn) "in" else "out", startDate, endDate)
            },
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Colored dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = typeDisplayName(entry.type),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text  = "${if (showIn) "+" else "-"}Ksh ${String.format("%,.0f", entry.amount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text  = "${"%.1f".format(fraction * 100)}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Full period chip ─────────────────────────────────────────────────────────

@Composable
private fun FullPeriodChip(
    selectedTimePeriod: TimePeriod,
    startDate: String,
    endDate: String,
    isPremium: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit,
    onShowSubscriptionScreen: () -> Unit,
    onOpenCustomPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM, yyyy") }
    val periodOptions = remember {
        listOf(
            TimePeriod.TODAY, TimePeriod.YESTERDAY,
            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
            TimePeriod.THIS_YEAR, TimePeriod.ENTIRE
        )
    }
    Box(modifier = modifier) {
        var showMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(primary.copy(alpha = 0.10f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showMenu = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = if (selectedTimePeriod == TimePeriod.CUSTOM) {
                    val sd = runCatching { LocalDate.parse(startDate) }.getOrNull()
                    val ed = runCatching { LocalDate.parse(endDate) }.getOrNull()
                    if (sd != null && ed != null) "${dateFormatter.format(sd)} – ${dateFormatter.format(ed)}"
                    else "Custom"
                } else selectedTimePeriod.getDisplayName(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primary
            )
            Icon(
                painter = painterResource(R.drawable.arrow_downward),
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(11.dp)
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            periodOptions.forEach { period ->
                val requiresPremium = !isPremium && (
                    period == TimePeriod.LAST_MONTH ||
                    period == TimePeriod.THIS_YEAR ||
                    period == TimePeriod.ENTIRE
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = period.getDisplayName(),
                                fontSize = 14.sp,
                                fontWeight = if (period == selectedTimePeriod) FontWeight.Bold else FontWeight.Normal,
                                color = if (period == selectedTimePeriod) primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            if (requiresPremium) {
                                Icon(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = "Premium",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        if (requiresPremium) onShowSubscriptionScreen()
                        else onPeriodSelected(period)
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.calendar),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = primary
                        )
                        Text(
                            text = "Custom",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = primary
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    if (!isPremium) onShowSubscriptionScreen()
                    else onOpenCustomPicker()
                }
            )
        }
    }
}

// ─── Tab chip ─────────────────────────────────────────────────────────────────

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) selectedBg else Color.Transparent)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text  = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Keep old DateRangePickerDialog for any remaining usages ─────────────────
