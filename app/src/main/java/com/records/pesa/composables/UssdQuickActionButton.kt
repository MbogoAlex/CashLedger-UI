package com.records.pesa.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth

/**
 * Prominent button to initiate M-PESA transactions via *334# USSD
 */
@Composable
fun UssdQuickActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = screenHeight(x = 4.0))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(screenWidth(x = 20.0))
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Make a Transaction",
                    fontSize = screenFontSize(x = 18.0).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = getCaptivatingCTA(),
                    fontSize = screenFontSize(x = 13.0).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = screenHeight(x = 4.0))
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_phone_ussd),
                contentDescription = "USSD Dial",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(screenWidth(x = 32.0))
            )
        }
    }
}

/**
 * Returns a captivating call-to-action text
 */
private fun getCaptivatingCTA(): String {
    val ctas = listOf(
        "Once you go USSD, you don't go back! 🚀",
        "Tap to transact like a pro! ⚡",
        "Your wallet's best friend is just a tap away 💰",
        "Quick! Send money faster than lightning ⚡💸",
        "USSD: Because life's too short for slow transfers 🚀",
        "Make it rain with *334#! 💸",
        "The fastest way to move your money! 🏃‍♂️💨"
    )
    return ctas.random()
}
