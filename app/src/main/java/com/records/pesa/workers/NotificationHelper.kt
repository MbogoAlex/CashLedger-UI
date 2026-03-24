package com.records.pesa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.records.pesa.MainActivity
import com.records.pesa.R

object NotificationHelper {
    const val BUDGET_CHANNEL_ID = "budget_alerts"
    const val BUDGET_CHANNEL_NAME = "Budget Alerts"
    const val SMS_CHANNEL_ID = "mpesa_transactions"
    const val SMS_CHANNEL_NAME = "M-PESA Transactions"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = NotificationManagerCompat.from(context)
            mgr.createNotificationChannel(
                NotificationChannel(BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for budget threshold alerts"
                }
            )
            mgr.createNotificationChannel(
                NotificationChannel(SMS_CHANNEL_ID, SMS_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifications for new M-PESA transactions detected via SMS"
                }
            )
        }
    }

    fun notifyTransaction(context: Context, transactionId: Int, title: String, body: String) {
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
        val notification = NotificationCompat.Builder(context, SMS_CHANNEL_ID)
            .setSmallIcon(R.drawable.transactions)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(transactionId + 100_000, notification)
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
}
