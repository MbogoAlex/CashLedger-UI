package com.records.pesa.ui.screens.dashboard.chart.setup

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import co.yml.charts.common.model.LegendLabel
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.GroupBar
import com.records.pesa.models.GroupedTransactionData
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