package com.records.pesa.ui.screens.transactions

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.decoration.rememberHorizontalBox
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.records.pesa.composables.SortedTransactionItemCell
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.moneyInSortedTransactionItems
import com.records.pesa.ui.screens.dashboard.chart.vico.rememberMarker
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GroupedTransactionsScreenComposable(
    pullRefreshState: PullRefreshState,
    loadingStatus: LoadingStatus,
    groupedTransactionItems: List<SortedTransactionItem>,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        GroupedTransactionsScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = loadingStatus,
            sortedTransactionItems = groupedTransactionItems,
            navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GroupedTransactionsScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    sortedTransactionItems: List<SortedTransactionItem>,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if(loadingStatus != LoadingStatus.LOADING) {
            LazyColumn {
                if(sortedTransactionItems.isNotEmpty()) {
                    item {
                        Chart6(sortedTransactionItems, Modifier.height(screenHeight(x = 350.0)))
                    }
                }

                items(sortedTransactionItems) {
                    SortedTransactionItemCell(
                        transaction = it,
                        modifier = Modifier
                            .clickable {
                                val encodedEntity = Uri.encode(it.entity)
                                navigateToEntityTransactionsScreen(it.transactionType, encodedEntity, it.times.toString())
                            }
                    )
                    Divider()
                }
            }
        }

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
        ) {
            PullRefreshIndicator(
                refreshing = loadingStatus == LoadingStatus.LOADING,
                state = pullRefreshState!!
            )
        }
    }
}

@Composable
internal fun Chart6(transactions: List<SortedTransactionItem>, modifier: Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(transactions) {
        withContext(Dispatchers.Default) {
            while (isActive) {
                modelProducer.runTransaction {
                    /* Learn more:
                    https://patrykandpatrick.com/vico/wiki/cartesian-charts/layers/column-layer#data. */
                    columnSeries {
                        series(
                            List(transactions.size) {
                                transactions[it].totalIn
                            }
                        )
                        series(
                            List(transactions.size) {
                                transactions[it].totalOut
                            }
                        )
                    }
                }
                delay(2000L)
            }
        }
    }

    ComposeChart6(transactions, modelProducer, modifier)
}

@Composable
private fun ComposeChart6(transactions: List<SortedTransactionItem>, modelProducer: CartesianChartModelProducer, modifier: Modifier) {
    val columnColors = listOf(MaterialTheme.colorScheme.surfaceTint, MaterialTheme.colorScheme.error)
    val shape = remember { Shape.cut(topLeftPercent = 50) }
    val bottomAxisValueFormatter = CartesianValueFormatter { x, _, _ ->
        transactions.map { it.entity }[x.toInt() % transactions.size]
    }
    CartesianChartHost(
        chart =
        rememberCartesianChart(
            rememberColumnCartesianLayer(
                ColumnCartesianLayer.ColumnProvider.series(
                    columnColors.map { rememberLineComponent(color = it, thickness = 50.dp, shape = shape) }
                )

            ),
            startAxis = rememberStartAxis(
                guideline = rememberAxisGuidelineComponent(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                label = rememberAxisLabelComponent(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ),

            bottomAxis = rememberBottomAxis(
                guideline = rememberAxisGuidelineComponent(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                valueFormatter = bottomAxisValueFormatter,
                label = rememberAxisLabelComponent(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ),
            marker = rememberMarker(),
            decorations = listOf(rememberComposeHorizontalBox()),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        zoomState = rememberVicoZoomState(zoomEnabled = false),
    )
}

@Composable
private fun rememberComposeHorizontalBox(): HorizontalBox {
    val HORIZONTAL_BOX_COLOR = -1448529
    val horizontalBoxY = 7.0..14.0
    val HORIZONTAL_BOX_ALPHA = 0.36f
    val color = Color(HORIZONTAL_BOX_COLOR)
    val HORIZONTAL_BOX_LABEL_MARGIN_DP = 4f
    val HORIZONTAL_BOX_LABEL_HORIZONTAL_PADDING_DP = 8f
    val HORIZONTAL_BOX_LABEL_VERTICAL_PADDING_DP = 2f
    return rememberHorizontalBox(
        y = { horizontalBoxY },
        box = rememberShapeComponent(color = color.copy(HORIZONTAL_BOX_ALPHA)),
        labelComponent =
        rememberTextComponent(
            margins = Dimensions.of(HORIZONTAL_BOX_LABEL_MARGIN_DP.dp),
            padding =
            Dimensions.of(
                HORIZONTAL_BOX_LABEL_HORIZONTAL_PADDING_DP.dp,
                HORIZONTAL_BOX_LABEL_VERTICAL_PADDING_DP.dp,
            ),
            background = rememberShapeComponent(color, Shape.Rectangle),
        ),
    )
}



@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GroupedTransactionsScreenPreview() {
    CashLedgerTheme {
        GroupedTransactionsScreen(
            navigateToEntityTransactionsScreen = {transactionType, entity, times ->  },
            sortedTransactionItems = moneyInSortedTransactionItems,
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null
        )
    }
}