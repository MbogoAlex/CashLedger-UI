package com.records.pesa.models

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Represents different time periods for transaction filtering and analysis
 */
sealed class TimePeriod {
    object TODAY : TimePeriod()
    object YESTERDAY : TimePeriod()
    object THIS_WEEK : TimePeriod()
    object LAST_WEEK : TimePeriod()
    object THIS_MONTH : TimePeriod()
    object LAST_MONTH : TimePeriod()
    object THIS_YEAR : TimePeriod()
    data class SPECIFIC_YEAR(val year: Int) : TimePeriod()
    
    /**
     * Get the date range for this period
     * @return Pair of (startDate, endDate) inclusive
     */
    fun getDateRange(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        
        return when (this) {
            is TODAY -> now to now
            
            is YESTERDAY -> {
                val yesterday = now.minusDays(1)
                yesterday to yesterday
            }
            
            is THIS_WEEK -> {
                // Week starts on Sunday
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                startOfWeek to endOfWeek
            }
            
            is LAST_WEEK -> {
                // Previous week (Sunday to Saturday)
                val startOfLastWeek = now.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val endOfLastWeek = startOfLastWeek.plusDays(6)
                startOfLastWeek to endOfLastWeek
            }
            
            is THIS_MONTH -> {
                val startOfMonth = now.withDayOfMonth(1)
                val endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                startOfMonth to endOfMonth
            }
            
            is LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                val startOfLastMonth = lastMonth.withDayOfMonth(1)
                val endOfLastMonth = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
                startOfLastMonth to endOfLastMonth
            }
            
            is THIS_YEAR -> {
                val startOfYear = now.withDayOfYear(1)
                val endOfYear = now.with(TemporalAdjusters.lastDayOfYear())
                startOfYear to endOfYear
            }
            
            is SPECIFIC_YEAR -> {
                val startOfYear = LocalDate.of(year, 1, 1)
                val endOfYear = LocalDate.of(year, 12, 31)
                startOfYear to endOfYear
            }
        }
    }
    
    /**
     * Get display name for this period
     */
    fun getDisplayName(): String {
        val now = LocalDate.now()
        
        return when (this) {
            is TODAY -> "Today"
            is YESTERDAY -> "Yesterday"
            is THIS_WEEK -> "This Week"
            is LAST_WEEK -> "Last Week"
            is THIS_MONTH -> "This Month"
            is LAST_MONTH -> "Last Month"
            is THIS_YEAR -> "This Year"
            is SPECIFIC_YEAR -> year.toString()
        }
    }
    
    /**
     * Get detailed label with date range
     */
    fun getDetailedLabel(): String {
        val (start, end) = getDateRange()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")
        
        return when (this) {
            is TODAY -> "Today, ${start.format(formatter)}"
            is YESTERDAY -> "Yesterday, ${start.format(formatter)}"
            is THIS_WEEK -> "This Week (${start.format(formatter)} - ${end.format(formatter)})"
            is LAST_WEEK -> "Last Week (${start.format(formatter)} - ${end.format(formatter)})"
            is THIS_MONTH -> "This Month (${start.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))})"
            is LAST_MONTH -> "Last Month (${start.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))})"
            is THIS_YEAR -> "This Year (${start.year})"
            is SPECIFIC_YEAR -> "Year $year"
        }
    }
}

/**
 * Summary of transactions grouped by type for a specific period
 */
data class TransactionTypeSummary(
    val transactionType: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentageOfTotal: Float,
    val icon: Int? = null
)
