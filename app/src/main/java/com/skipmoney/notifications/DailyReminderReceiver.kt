package com.skipmoney.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(
            "DailyReminderReceiver",
            "onReceive: action=${intent?.action}, receivedAt=${System.currentTimeMillis()}",
        )
        DailyReminderScheduler.showNotification(context)
        DailyReminderScheduler.scheduleNextReminder(context)
    }
}
