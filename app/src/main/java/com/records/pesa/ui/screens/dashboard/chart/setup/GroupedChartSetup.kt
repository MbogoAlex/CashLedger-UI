package com.records.pesa.ui.screens.dashboard.chart.setup

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import co.yml.charts.axis.XAxis
import co.yml.charts.axis.YAxis
import co.yml.charts.axis.getXAxisScale
import co.yml.charts.chartcontainer.container.ScrollableCanvasContainer
import co.yml.charts.common.components.ItemDivider
import co.yml.charts.common.components.accessibility.AccessibilityBottomSheetDialog
import co.yml.charts.common.components.accessibility.LinePointInfo
import co.yml.charts.common.extensions.drawGridLines
import co.yml.charts.common.extensions.isNotNull
import co.yml.charts.common.model.LegendLabel
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.GroupBar
import co.yml.charts.ui.linechart.drawHighLightOnSelectedPoint
import co.yml.charts.ui.linechart.drawHighlightText
import co.yml.charts.ui.linechart.drawShadowUnderLineAndIntersectionPoint
import co.yml.charts.ui.linechart.drawStraightOrCubicLine
import co.yml.charts.ui.linechart.getCubicPoints
import co.yml.charts.ui.linechart.getMappingPointsToGraph
import co.yml.charts.ui.linechart.getMaxElementInYAxis
import co.yml.charts.ui.linechart.getMaxScrollDistance
import co.yml.charts.ui.linechart.getYAxisScale
import co.yml.charts.ui.linechart.isTapped
import co.yml.charts.ui.linechart.model.LineChartData
import com.records.pesa.models.transaction.GroupedTransactionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun getGroupBarChartData(transactions: List<GroupedTransactionData>, barSize: Int): List<GroupBar> {
    val list = mutableListOf<GroupBar>()
    for ((index, transaction) in transactions.withIndex()) {
        val barList = mutableListOf<BarData>()
        for (i in 0 until barSize) {
            val barValue = if (i == 0) transaction.moneyIn else transaction.moneyOut
            barList.add(
                BarData(
                    Point(
                        index.toFloat(),
                        barValue
                    ),
                    label = "B$i",
                    description = "Bar at $index with label B$i has value ${
                        String.format("%.2f", barValue)
                    }"
                )
            )
        }
        list.add(GroupBar(index.toString(), barList))
    }
    return list
}
@Composable
fun getColorPaletteList(listSize: Int): List<Color> {
    val colorList = mutableListOf<Color>()
    colorList.add(
        MaterialTheme.colorScheme.surfaceTint
    )
    colorList.add(
        MaterialTheme.colorScheme.error
    )
    return colorList
}
@Composable
fun getLegendsLabelData(colorPaletteList: List<Color>): List<LegendLabel> {
    val legendLabelList = mutableListOf<LegendLabel>()
    legendLabelList.add(
        LegendLabel(
            colorPaletteList[0],
            "Money in"
        )
    )
    legendLabelList.add(
        LegendLabel(
            colorPaletteList[1],
            "Money Out"
        )
    )
    return legendLabelList
}

private fun DrawScope.drawUnderScrollMask(columnWidth: Float, paddingRight: Dp, bgColor: Color) {
    drawRect(
        bgColor, Offset(0f, 0f), Size(columnWidth, size.height)
    )
    drawRect(
        bgColor,
        Offset(size.width - paddingRight.toPx(), 0f),
        Size(paddingRight.toPx(), size.height)
    )
}

private const val TALKBACK_PACKAGE_NAME = "com.google.android.marvin.talkback"
private const val TALKBACK_PACKAGE_NAME_SAMSUNG = "com.samsung.android.accessibility.talkback"
@Composable
internal fun Context.collectIsTalkbackEnabledAsState(): State<Boolean> {
    val accessibilityManager =
        this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    fun isTalkbackEnabled(): Boolean {
        val accessibilityServiceInfoList =
            accessibilityManager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
        return accessibilityServiceInfoList?.any {
            it.resolveInfo.serviceInfo.processName.equals(TALKBACK_PACKAGE_NAME) || it.resolveInfo.serviceInfo.processName.equals(
                TALKBACK_PACKAGE_NAME_SAMSUNG
            )
        } ?: false
    }

    val talkbackEnabled = remember { mutableStateOf(isTalkbackEnabled()) }
    val accessibilityManagerEnabled = accessibilityManager?.isEnabled ?: false
    var accessibilityEnabled by remember { mutableStateOf(accessibilityManagerEnabled) }

    accessibilityManager?.addAccessibilityStateChangeListener { accessibilityEnabled = it }

    LaunchedEffect(accessibilityEnabled) {
        talkbackEnabled.value = if (accessibilityEnabled) isTalkbackEnabled() else false
    }
    return talkbackEnabled
}

internal class RowClip(
    private val leftPadding: Float,
    private val rightPadding: Dp,
    private val topPadding: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            androidx.compose.ui.geometry.Rect(
                leftPadding,
                topPadding,
                size.width - rightPadding.value * density.density,
                size.height
            )
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LineChart(modifier: Modifier, lineChartData: LineChartData, triggerRebuild: (xStart: Int, xEnd: Int) -> Float){
    val accessibilitySheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val isTalkBackEnabled by LocalContext.current.collectIsTalkbackEnabledAsState()

    var xStart = 0
    var xEnd = 0

    if (accessibilitySheetState.isVisible && isTalkBackEnabled
        && lineChartData.accessibilityConfig.shouldHandleBackWhenTalkBackPopUpShown
    ) {
        BackHandler {
            scope.launch {
                accessibilitySheetState.hide()
            }
        }
    }

    Surface(modifier = modifier) {
        with(lineChartData) {
            var columnWidth by remember { mutableStateOf(0f) }
            var rowHeight by remember { mutableStateOf(0f) }
            var xOffset by remember { mutableStateOf(0f) }
            val bgColor = MaterialTheme.colorScheme.surface
            var isTapped by remember { mutableStateOf(false) }
            var tapOffset by remember { mutableStateOf(Offset(0f, 0f)) }
            var selectionTextVisibility by remember { mutableStateOf(false) }
            var identifiedPoint by remember { mutableStateOf(Point(0f, 0f)) }
            var scrollOffset by remember { mutableStateOf(0f) } // Track scroll offset

            val linePoints: List<Point> = linePlotData.lines.flatMap { line -> line.dataPoints.map { it } }

            val (xMin, xMax, xAxisScale) = getXAxisScale(linePoints, xAxisData.steps)
            val (yMin, _, yAxisScale) = getYAxisScale(linePoints, yAxisData.steps)
            var maxElementInYAxis = getMaxElementInYAxis(yAxisScale, yAxisData.steps)
            val xAxisData = xAxisData.copy(axisBottomPadding = bottomPadding)
            val yAxisData = yAxisData.copy(
                axisBottomPadding = LocalDensity.current.run { rowHeight.toDp() },
                axisTopPadding = paddingTop
            )



            ScrollableCanvasContainer(modifier = modifier
                .semantics {
                    contentDescription = lineChartData.accessibilityConfig.chartDescription
                }
                .clickable {
                    if (isTalkBackEnabled) {
                        scope.launch {
                            accessibilitySheetState.show()
                        }
                    }
                },
                calculateMaxDistance = { xZoom ->
                    xOffset = xAxisData.axisStepSize.toPx() * xZoom
                    getMaxScrollDistance(
                        columnWidth,
                        xMax,
                        xMin,
                        xOffset,
                        paddingRight.toPx(),
                        size.width,
                        containerPaddingEnd.toPx()
                    )
                },
                containerBackgroundColor = backgroundColor,
                isPinchZoomEnabled = isZoomAllowed,
                drawXAndYAxis = { scrollOffset, xZoom ->
                    YAxis(
                        modifier = Modifier
                            .fillMaxHeight()
                            .onGloballyPositioned {
                                columnWidth = 0f
                            }, yAxisData = yAxisData
                    )
                    XAxis(xAxisData = xAxisData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .align(Alignment.BottomStart)
                            .onGloballyPositioned {
                                rowHeight = it.size.height.toFloat()
                            }
                            .clip(
                                RowClip(
                                    columnWidth, paddingRight
                                )
                            ),
                        xStart = columnWidth,
                        scrollOffset = scrollOffset,
                        zoomScale = xZoom,
                        chartData = linePoints,axisStart = columnWidth)
                },
                onDraw = { newScrollOffset, xZoom ->
                    scrollOffset = newScrollOffset

                    val xFirst = xMin + (scrollOffset / xOffset)
                    val xLast = xMin + ((scrollOffset + size.width) / xOffset)
                    maxElementInYAxis = triggerRebuild(xFirst.toInt(), xLast.toInt())
                    xStart = xFirst.toInt()
                    xEnd = xLast.toInt()

                    Log.d("LineChart", "xStart: $xStart, xEnd: $xEnd")

                    linePlotData.lines.forEach { line ->
                        val yBottom = size.height - rowHeight
                        val yOffset = ((yBottom - paddingTop.toPx()) / maxElementInYAxis)
                        xOffset = xAxisData.axisStepSize.toPx() * xZoom
                        val xLeft = columnWidth
                        val pointsData = getMappingPointsToGraph(
                            line.dataPoints, xMin, xOffset, xLeft, scrollOffset, yBottom, yMin, yOffset
                        )
                        val (cubicPoints1, cubicPoints2) = getCubicPoints(pointsData)
                        val tapPointLocks = mutableMapOf<Int, Pair<Point, Offset>>()

                        gridLines?.let {
                            drawGridLines(
                                yBottom,
                                yAxisData.axisTopPadding.toPx(),
                                xLeft,
                                paddingRight,
                                scrollOffset,
                                pointsData.size,
                                xZoom,
                                xAxisScale,
                                yAxisData.steps,
                                xAxisData.axisStepSize,
                                it
                            )
                        }

                        val cubicPath = drawStraightOrCubicLine(
                            pointsData, cubicPoints1, cubicPoints2, line.lineStyle
                        )

                        drawShadowUnderLineAndIntersectionPoint(
                            cubicPath, pointsData, yBottom, line
                        )

                        drawUnderScrollMask(columnWidth, paddingRight, bgColor)

                        pointsData.forEachIndexed { index, point ->
                            if (isTapped && point.isTapped(tapOffset.x, xOffset)) {
                                tapPointLocks[0] = line.dataPoints[index] to point
                            }
                        }

                        val selectedOffset = tapPointLocks.values.firstOrNull()?.second
                        if (selectionTextVisibility && selectedOffset.isNotNull()) {
                            drawHighlightText(
                                identifiedPoint,
                                selectedOffset ?: Offset(0f, 0f),
                                line.selectionHighlightPopUp
                            )
                        }
                        if (isTapped) {
                            val x = tapPointLocks.values.firstOrNull()?.second?.x
                            if (x != null) identifiedPoint =
                                tapPointLocks.values.map { it.first }.first()
                            drawHighLightOnSelectedPoint(
                                tapPointLocks,
                                columnWidth,
                                paddingRight,
                                yBottom,
                                line.selectionHighlightPoint
                            )
                        }
                    }
                },

                onPointClicked = { offset: Offset, _: Float ->
                    isTapped = true
                    selectionTextVisibility = true
                    tapOffset = offset
                },
                onScroll = {
                    isTapped = false
                    selectionTextVisibility = false
                },
                onZoomInAndOut = {
                    isTapped = false
                    selectionTextVisibility = false
                })
            if (isTalkBackEnabled) {
                AccessibilityBottomSheetDialog(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color.White,
                    content = {
                        LazyColumn {
                            items(count = linePlotData.lines.size) { lineIndex ->
                                linePlotData.lines[lineIndex].dataPoints.forEachIndexed { pointIndex, point ->
                                    Column {
                                        LinePointInfo(
                                            xAxisData.axisLabelDescription(
                                                xAxisData.labelData(
                                                    pointIndex
                                                )
                                            ),
                                            point.description,
                                            linePlotData.lines[lineIndex].lineStyle.color,
                                            accessibilityConfig.titleTextSize,
                                            accessibilityConfig.descriptionTextSize
                                        )

                                        ItemDivider(
                                            thickness = accessibilityConfig.dividerThickness,
                                            dividerColor = accessibilityConfig.dividerColor
                                        )
                                    }

                                }
                            }
                        }
                    },
                    popUpTopRightButtonTitle = accessibilityConfig.popUpTopRightButtonTitle,
                    popUpTopRightButtonDescription = accessibilityConfig.popUpTopRightButtonDescription,
                    sheetState = accessibilitySheetState
                )

            }

        }
    }
}

fun <T> debounce(
    delayMillis: Long,
    scope: CoroutineScope,
    destinationFunction: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMillis)
            destinationFunction(param)
        }
    }
}



