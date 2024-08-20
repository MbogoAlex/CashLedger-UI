package com.records.pesa.ui.screens.utils

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun screenHeight(x: Double): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val a = 803.6363636363636 / x
    return screenHeight / a.toFloat()
}

@Composable
fun screenWidth(x: Double): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val a = 392.72727272727275 / x
    return screenWidth / a.toFloat()
}

@Composable
fun screenFontSize(x: Double): Double {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp.value
    val a = 803.6363636363636 / x
    val fontSize = screenHeight / a
    return fontSize
}
