package com.records.pesa.functions

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

/**
 * Supported recurrence patterns for recurring budgets.
 */
enum class RecurrenceType(val displayName: String, val shortLabel: String) {
    DAILY("Daily", "Day"),
    WEEKLY("Weekly", "Week"),
    BIWEEKLY("Bi-weekly", "2 Weeks"),
    MONTHLY("Monthly", "Month"),
    QUARTERLY("Quarterly", "Quarter"),
    ANNUALLY("Annually", "Year"),
    CUSTOM("Custom", "Custom");

    companion object {
        fun fromString(value: String?): RecurrenceType? =
            entries.firstOrNull { it.name == value }
    }
}

/**
 * Utility for computing next cycle dates for recurring budgets.
 */
object RecurrenceHelper {

    /**
     * Given the end date of the current cycle, compute the start date of the next cycle.
     * Next cycle starts the day after the current one ends.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun nextCycleStartDate(currentEndDate: LocalDate): LocalDate =
        currentEndDate.plusDays(1)

    /**
     * Given the start date of the next cycle, compute its end date based on the recurrence type.
     * For CUSTOM, [intervalDays] determines the length (defaults to 30 if null).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun nextCycleEndDate(
        startDate: LocalDate,
        recurrenceType: String,
        intervalDays: Int? = null,
    ): LocalDate = when (RecurrenceType.fromString(recurrenceType)) {
        RecurrenceType.DAILY      -> startDate                        // single day
        RecurrenceType.WEEKLY     -> startDate.plusDays(6)            // 7-day cycle
        RecurrenceType.BIWEEKLY   -> startDate.plusDays(13)           // 14-day cycle
        RecurrenceType.MONTHLY    -> startDate.plusMonths(1).minusDays(1)
        RecurrenceType.QUARTERLY  -> startDate.plusMonths(3).minusDays(1)
        RecurrenceType.ANNUALLY   -> startDate.plusYears(1).minusDays(1)
        RecurrenceType.CUSTOM, null -> startDate.plusDays(((intervalDays ?: 30) - 1).toLong())
    }

    /**
     * Human-readable summary for a given recurrence type.
     */
    fun summaryLabel(recurrenceType: String, intervalDays: Int? = null): String =
        when (RecurrenceType.fromString(recurrenceType)) {
            RecurrenceType.DAILY      -> "Resets every day"
            RecurrenceType.WEEKLY     -> "Resets every week"
            RecurrenceType.BIWEEKLY   -> "Resets every 2 weeks"
            RecurrenceType.MONTHLY    -> "Resets every month"
            RecurrenceType.QUARTERLY  -> "Resets every 3 months"
            RecurrenceType.ANNUALLY   -> "Resets every year"
            RecurrenceType.CUSTOM, null -> "Resets every ${intervalDays ?: 30} days"
        }
}
