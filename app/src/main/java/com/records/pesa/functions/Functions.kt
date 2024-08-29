package com.records.pesa.functions

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
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
    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()), // Original format
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())     // Format without seconds
    )
    val outputFormat = SimpleDateFormat("d'suffix' MMMM yyyy, h:mm a", Locale.getDefault())

    var date: Date? = null

    for (inputFormat in inputFormats) {
        try {
            date = inputFormat.parse(inputDate)
            if (date != null) break
        } catch (e: ParseException) {
            // Ignore and try the next format
        }
    }

    // Handle case where none of the formats matched
    if (date == null) throw ParseException("Unparseable date: \"$inputDate\"", 0)

    val day = SimpleDateFormat("d", Locale.getDefault()).format(date).toInt()
    val suffix = getDayOfMonthSuffix(day)

    val outputFormatStr = outputFormat.format(date).replace("suffix", suffix)

    return outputFormatStr
}

fun getDayOfMonthSuffix(day: Int): String {
    return when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}


fun formatMoneyValue(amount: Double): String {
    return  NumberFormat.getCurrencyInstance(Locale("en", "KE")).format(amount)
}