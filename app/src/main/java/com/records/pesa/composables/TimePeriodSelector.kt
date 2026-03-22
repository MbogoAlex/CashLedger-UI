package com.records.pesa.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.models.TimePeriod
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.screens.utils.screenFontSize
import java.time.LocalDate

@Composable
fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    availableYears: List<Int>,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = LocalDate.now().year
    
    // Build list of all periods
    val allPeriods = buildList {
        add(TimePeriod.TODAY to "Today")
        add(TimePeriod.YESTERDAY to "Yesterday")
        add(TimePeriod.THIS_WEEK to "This Week")
        add(TimePeriod.LAST_WEEK to "Last Week")
        add(TimePeriod.THIS_MONTH to "This Month")
        add(TimePeriod.LAST_MONTH to "Last Month")
        add(TimePeriod.THIS_YEAR to "This Year")
        availableYears.filter { it < currentYear }.forEach { year ->
            add(TimePeriod.SPECIFIC_YEAR(year) to year.toString())
        }
    }
    
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPeriod.getDisplayName(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            allPeriods.forEach { (period, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = modifier
    )
}