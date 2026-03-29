package com.records.pesa.workers

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
class SubscriptionExpiryWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val container = (applicationContext as CashLedger).container
        val preferences = container.dbRepository.getUserPreferences()?.first() ?: return Result.success()

        // Skip for lifetime/non-paid users
        if (preferences.permanent || !preferences.paid) return Result.success()

        val expiryDate = preferences.expiryDate ?: return Result.success()
        val now = LocalDateTime.now()

        // Already expired by more than a day — clear old alerts and return
        if (expiryDate.isBefore(now.minusDays(1))) {
            prefs.edit().remove(KEY_LAST_EXPIRY).remove(KEY_FIRED).apply()
            return Result.success()
        }

        val daysLeft = ChronoUnit.DAYS.between(now, expiryDate)

        // Reset alert tracking when the expiry date changes (user renewed)
        val savedExpiry = prefs.getString(KEY_LAST_EXPIRY, null)
        val currentExpiry = expiryDate.toString()
        if (savedExpiry != currentExpiry) {
            prefs.edit()
                .putString(KEY_LAST_EXPIRY, currentExpiry)
                .remove(KEY_FIRED)
                .apply()
        }

        val fired = prefs.getStringSet(KEY_FIRED, emptySet())?.toMutableSet() ?: mutableSetOf()

        fun hasFired(key: String) = fired.contains(key)
        fun markFired(key: String) {
            fired.add(key)
            prefs.edit().putStringSet(KEY_FIRED, fired).apply()
        }

        // 7-day warning
        if (daysLeft <= 7 && daysLeft > 3 && !hasFired("7")) {
            NotificationHelper.notifySubscription(
                context = context,
                id = NOTIF_ID_7_DAYS,
                title = "⏳ Subscription expiring soon",
                body = "Your Cash Ledger premium subscription expires in $daysLeft days. Renew now to keep access."
            )
            markFired("7")
        }

        // 3-day warning
        if (daysLeft <= 3 && daysLeft > 1 && !hasFired("3")) {
            NotificationHelper.notifySubscription(
                context = context,
                id = NOTIF_ID_3_DAYS,
                title = "⚠️ Subscription expiring in $daysLeft days",
                body = "Only $daysLeft days left on your Cash Ledger Premium subscription. Don't lose access — renew today."
            )
            markFired("3")
        }

        // 1-day warning
        if (daysLeft <= 1 && daysLeft >= 0 && !hasFired("1")) {
            val msg = if (daysLeft == 0L) "Your subscription expires today!" else "Your subscription expires tomorrow!"
            NotificationHelper.notifySubscription(
                context = context,
                id = NOTIF_ID_1_DAY,
                title = "🚨 Subscription expires ${if (daysLeft == 0L) "today" else "tomorrow"}",
                body = "$msg Renew your Cash Ledger Premium to keep uninterrupted access."
            )
            markFired("1")
        }

        // Expiry day (already expired, within the same day)
        if (daysLeft < 0 && !hasFired("expired")) {
            NotificationHelper.notifySubscription(
                context = context,
                id = NOTIF_ID_EXPIRED,
                title = "❌ Your subscription has expired",
                body = "Your Cash Ledger Premium subscription has ended. Renew now to restore full access."
            )
            markFired("expired")
        }

        return Result.success()
    }

    companion object {
        private const val PREFS_NAME = "subscription_expiry_prefs"
        private const val KEY_LAST_EXPIRY = "last_expiry_date"
        private const val KEY_FIRED = "fired_alerts"

        private const val NOTIF_ID_7_DAYS = 9_001
        private const val NOTIF_ID_3_DAYS = 9_002
        private const val NOTIF_ID_1_DAY  = 9_003
        private const val NOTIF_ID_EXPIRED = 9_004
    }
}
