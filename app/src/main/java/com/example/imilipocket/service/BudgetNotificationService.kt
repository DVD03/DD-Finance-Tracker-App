package com.example.imilipocket.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.imilipocket.util.ImiliPocketApp
import com.example.imilipocket.R
import com.example.imilipocket.data.PreferenceManager

class BudgetNotificationService : Service() {
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkBudgetAndNotify()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkBudgetAndNotify() {
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val monthlyExpenses = preferenceManager.getMonthlyExpenses()
        val remainingBudget = monthlyBudget - monthlyExpenses
        val remainingPercentage = (remainingBudget / monthlyBudget) * 100

        if (remainingPercentage <= 20) {
            showBudgetWarningNotification(remainingPercentage.toInt())
        }
    }

    private fun showBudgetWarningNotification(remainingPercentage: Int) {
        val notification = NotificationCompat.Builder(this, ImiliPocketApp.BUDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.budget_warning))
            .setContentText("Only $remainingPercentage% of your monthly budget remaining")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(BUDGET_WARNING_ID, notification)
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
        }
    }

    companion object {
        const val BUDGET_WARNING_ID = 1002
    }
} 