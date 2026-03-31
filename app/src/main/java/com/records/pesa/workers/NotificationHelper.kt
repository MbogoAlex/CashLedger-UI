package com.records.pesa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.records.pesa.MainActivity
import com.records.pesa.R

object NotificationHelper {
    const val BUDGET_CHANNEL_ID = "budget_alerts"
    const val BUDGET_CHANNEL_NAME = "Budget Alerts"
    const val SMS_CHANNEL_ID = "mpesa_transactions_v2"
    const val SMS_CHANNEL_NAME = "M-PESA Transactions"
    const val SUBSCRIPTION_CHANNEL_ID = "subscription_alerts"
    const val SUBSCRIPTION_CHANNEL_NAME = "Subscription Alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = NotificationManagerCompat.from(context)
            mgr.createNotificationChannel(
                NotificationChannel(BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for budget threshold alerts"
                }
            )
            mgr.createNotificationChannel(
                NotificationChannel(SMS_CHANNEL_ID, SMS_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for new M-PESA transactions detected via SMS"
                    enableVibration(true)
                }
            )
            mgr.createNotificationChannel(
                NotificationChannel(SUBSCRIPTION_CHANNEL_ID, SUBSCRIPTION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders about your Cash Ledger subscription expiry"
                }
            )
            // Backup worker foreground service channel — must exist before BackupWorker starts
            mgr.createNotificationChannel(
                NotificationChannel(BackupWorker.CHANNEL_ID, "Backup", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Background cloud backup progress"
                }
            )
        }
    }

    fun notifyTransaction(context: Context, transactionId: Int, title: String, body: String) {
        val mgr = NotificationManagerCompat.from(context)

        if (!mgr.areNotificationsEnabled()) {
            Log.e("CashLedger_SMS", "notifyTransaction: POST_NOTIFICATIONS not granted — go to Settings > Apps > Cash Ledger > Notifications and enable them")
            return
        }

        // Ensure channel exists every time (idempotent — safe to call repeatedly)
        createChannel(context)

        val deepLinkUri = Uri.parse("cashledger://transaction-details-screen/$transactionId")
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLinkUri
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId + 100_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appLogo = BitmapFactory.decodeResource(context.resources, R.drawable.cashledger_logo)
        val notification = NotificationCompat.Builder(context, SMS_CHANNEL_ID)
            .setSmallIcon(R.drawable.transactions)
            .setLargeIcon(appLogo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        mgr.notify(transactionId + 100_000, notification)
        Log.d("CashLedger_SMS", "notifyTransaction: posted ✓ id=${transactionId + 100_000}")
    }

    fun notify(context: Context, id: Int, budgetId: Int, title: String, body: String) {
        val deepLinkUri = Uri.parse("cashledger://budget-info-screen/$budgetId")
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLinkUri
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.budget_2)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun notifySubscription(context: Context, id: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, SUBSCRIPTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.star)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
