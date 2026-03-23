package com.records.pesa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.records.pesa.R

object NotificationHelper {
    const val BUDGET_CHANNEL_ID = "budget_alerts"
    const val BUDGET_CHANNEL_NAME = "Budget Alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BUDGET_CHANNEL_ID,
                BUDGET_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget threshold alerts"
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    fun notify(context: Context, id: Int, title: String, body: String) {
        val iconRes = try {
            R.drawable.budget_2
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
