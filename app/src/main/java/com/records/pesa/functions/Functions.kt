package com.records.pesa.functions

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
fun formatIsoDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("d'th' MMMM, yyyy hh:mm a", Locale.ENGLISH)
    return dateTime.format(formatter)
}

fun formatLocalDate(dateTime: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d'th' MMMM, yyyy", Locale.ENGLISH)
    return dateTime.format(formatter)
}

fun formatDate(inputDate: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val outputFormat = SimpleDateFormat("d'suffix' MMMM yyyy, h:mm a", Locale.getDefault())

    val date = inputFormat.parse(inputDate)

    val day = SimpleDateFormat("d", Locale.getDefault()).format(date).toInt()
    val suffix = getDayOfMonthSuffix(day)

    val outputFormatStr = outputFormat.format(date).replace("suffix", suffix)

    return outputFormatStr
}

fun getDayOfMonthSuffix(n: Int): String {
    return if (n in 11..13) {
        "th"
    } else {
        when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}

fun formatMoneyValue(amount: Double): String {
    return  NumberFormat.getCurrencyInstance(Locale("en", "KE")).format(amount)
}