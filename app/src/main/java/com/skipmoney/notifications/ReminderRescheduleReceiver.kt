package com.skipmoney.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(
            "ReminderRescheduleReceiver",
            "onReceive: action=${intent?.action}, receivedAt=${System.currentTimeMillis()}",
        )
        DailyReminderScheduler.scheduleNextReminder(context)
    }
}
