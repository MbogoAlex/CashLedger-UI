package com.records.pesa.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.Point
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Modern line chart showing money in vs money out over time with Vico library
 */
@Composable
fun MoneyFlowLineChart(
    moneyInPoints: List<Point>,
    moneyOutPoints: List<Point>,
    modifier: Modifier = Modifier
) {
    // Debug logging
    android.util.Log.d("MoneyFlowChart", "Money In Points: ${moneyInPoints.size}, Money Out Points: ${moneyOutPoints.size}")
    
    if (moneyInPoints.isEmpty() && moneyOutPoints.isEmpty()) {
        EmptyChartState(
            message = "No transaction data available",
            modifier = modifier
        )
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    
    LaunchedEffect(moneyInPoints, moneyOutPoints) {
        withContext(Dispatchers.Default) {
            if (isActive && (moneyInPoints.isNotEmpty() || moneyOutPoints.isNotEmpty())) {
                modelProducer.runTransaction {
                    lineSeries {
                        // Money In series (green)
                        series(moneyInPoints.map { it.y.toDouble() })
                        // Money Out series (red)
                        series(moneyOutPoints.map { it.y.toDouble() })
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = screenHeight(x = 2.0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenWidth(x = 16.0))
        ) {
            // Chart title
            Text(
                text = "Money Flow",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            
            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(screenWidth(x = 16.0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Money In"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.error,
                    label = "Money Out"
                )
            }
            
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            
            // Chart
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            // Money In line (green/primary with gradient fill)
                            rememberLine(
                                shader = DynamicShader.color(MaterialTheme.colorScheme.primary),
                                backgroundShader = DynamicShader.verticalGradient(
                                    arrayOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                ),
                                thickness = 3.dp
                            ),
                            // Money Out line (red with gradient fill)
                            rememberLine(
                                shader = DynamicShader.color(MaterialTheme.colorScheme.error),
                                backgroundShader = DynamicShader.verticalGradient(
                                    arrayOf(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    )
                                ),
                                thickness = 3.dp
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        guideline = rememberAxisGuidelineComponent(
                            color = MaterialTheme.colorScheme.outlineVariant
                        ),
                        label = rememberAxisLabelComponent(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ),
                    bottomAxis = rememberBottomAxis(
                        guideline = rememberAxisGuidelineComponent(
                            color = MaterialTheme.colorScheme.outlineVariant
                        ),
                        label = rememberAxisLabelComponent(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight(x = 250.0))
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.size(screenWidth(x = 12.0)),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(screenWidth(x = 6.0)))
        Text(
            text = label,
            fontSize = screenFontSize(x = 12.0).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyChartState(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = screenHeight(x = 2.0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenWidth(x = 32.0))
                .height(screenHeight(x = 250.0)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📊",
                fontSize = screenFontSize(x = 48.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = message,
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Start making transactions to see insights",
                fontSize = screenFontSize(x = 12.0).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = screenHeight(x = 8.0))
            )
        }
    }
}
